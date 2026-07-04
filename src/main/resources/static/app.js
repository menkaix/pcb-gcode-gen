const state = {
	project: null,
	shapes: [],
	view: null, // { minX, minY, width, height } in mm; persists across re-renders until reset
	formMode: null, // null | 'layer' | 'element'
	editingLayerIndex: null,
	candidateLayerName: '',
	candidatePasses: 0,
	selectedLayer: null,
	selectedElement: null,
	editingSubType: null,
	candidateName: '',
	candidateProperties: null,
	candidatePreview: null,
};

const DEFAULT_PROPERTIES = {
	Rectangle: { corner: { x: 0, y: 0, z: 0 }, width: 10, height: 10 },
	Circle: { center: { x: 0, y: 0, z: 0 }, radius: 5 },
	ArcPath: { from: { x: 0, y: 0, z: 0 }, to: { x: 10, y: 0, z: 0 }, radius: 10, direction: 'CLOCKWISE' },
	PolyLineElement: { points: [{ x: 0, y: 0, z: 0 }, { x: 10, y: 0, z: 0 }] },
};

const FORM_SCHEMAS = {
	Rectangle: {
		fields: [
			{ key: 'corner', label: 'Coin (mm)', type: 'point' },
			{ key: 'width', label: 'Largeur (mm)', type: 'number' },
			{ key: 'height', label: 'Hauteur (mm)', type: 'number' },
		],
	},
	Circle: {
		fields: [
			{ key: 'center', label: 'Centre (mm)', type: 'point' },
			{ key: 'radius', label: 'Rayon (mm)', type: 'number' },
		],
	},
	ArcPath: {
		fields: [
			{ key: 'from', label: 'Départ (mm)', type: 'point' },
			{ key: 'to', label: 'Arrivée (mm)', type: 'point' },
			{ key: 'radius', label: 'Rayon (mm)', type: 'number' },
			{ key: 'direction', label: 'Sens', type: 'select', options: ['CLOCKWISE', 'COUNTER_CLOCKWISE'] },
		],
	},
	PolyLineElement: {
		fields: [{ key: 'points', label: 'Points (mm)', type: 'pointList' }],
	},
};

// ---------------- API helpers ----------------

async function apiFetch(url, options = {}) {
	const res = await fetch(url, {
		headers: { 'Content-Type': 'application/json' },
		...options,
	});
	if (!res.ok) {
		let body;
		try {
			body = await res.json();
		} catch (e) {
			body = { message: res.statusText };
		}
		throw new Error(body.message || `HTTP ${res.status}`);
	}
	if (res.status === 204) {
		return null;
	}
	return res.json();
}

function toast(message, isError = false) {
	const el = document.getElementById('toast');
	el.textContent = message;
	el.className = isError ? 'error' : 'success';
	clearTimeout(toast._timer);
	toast._timer = setTimeout(() => {
		el.className = 'hidden';
	}, 4000);
}

function button(label, onClick, cls = '') {
	const b = document.createElement('button');
	b.textContent = label;
	b.className = cls;
	b.onclick = onClick;
	return b;
}

// ---------------- Load / refresh ----------------

async function refresh() {
	const [project, shapes] = await Promise.all([apiFetch('/api/project'), apiFetch('/api/preview')]);
	state.project = project;
	state.shapes = shapes;
	renderMeta();
	renderTree();
	renderPreview();
}

function renderMeta() {
	const p = state.project;
	document.getElementById('meta-projectName').value = p.projectName || '';
	document.getElementById('meta-bitHead').value = p.bitHead || 'LASER';
	document.getElementById('meta-safeLevel').value = p.safeLevel ?? 0;
	document.getElementById('meta-passIncrement').value = p.passIncrement ?? 0;
	document.getElementById('meta-feedRate').value = p.feedRate ?? 0;
	document.getElementById('meta-power').value = p.power ?? 0;
}

// ---------------- Layer / element tree ----------------

function renderTree() {
	const container = document.getElementById('layer-tree');
	container.innerHTML = '';

	(state.project.layers || []).forEach((layer, li) => {
		const layerDiv = document.createElement('div');
		layerDiv.className = 'layer-node';

		const header = document.createElement('div');
		header.className = 'layer-header';

		const title = document.createElement('strong');
		title.textContent = layer.layerName;
		const passesSpan = document.createElement('span');
		passesSpan.className = 'muted';
		passesSpan.textContent = `(${layer.passes} passes)`;

		if (state.formMode === 'layer' && state.editingLayerIndex === li) {
			layerDiv.classList.add('editing');
		}

		header.append(
			title,
			passesSpan,
			button('Modifier', () => startEditLayer(li, layer)),
			button('Supprimer', () => deleteLayer(li), 'danger small'),
			button('+ Forme', () => startNewElement(li))
		);
		layerDiv.appendChild(header);

		const list = document.createElement('ul');
		(layer.elements || []).forEach((el, ei) => {
			const item = document.createElement('li');
			item.className = 'element-item';
			if (state.selectedLayer === li && state.selectedElement === ei) {
				item.classList.add('selected');
			}
			const label = document.createElement('span');
			label.textContent = `${el.name} (${el.subType})`;
			item.appendChild(label);
			item.appendChild(
				button('x', (e) => {
					e.stopPropagation();
					deleteElement(li, ei);
				}, 'danger small')
			);
			item.onclick = () => selectElement(li, ei);
			list.appendChild(item);
		});
		layerDiv.appendChild(list);

		container.appendChild(layerDiv);
	});
}

async function addLayer() {
	const name = prompt('Nom de la nouvelle couche', 'nouvelle-couche');
	if (name === null) return;
	try {
		await apiFetch('/api/layers', { method: 'POST', body: JSON.stringify({ layerName: name, passes: 1 }) });
		toast('Couche ajoutée.');
		await refresh();
	} catch (e) {
		toast(e.message, true);
	}
}

function startEditLayer(layerIndex, layer) {
	state.formMode = 'layer';
	state.editingLayerIndex = layerIndex;
	state.candidateLayerName = layer.layerName;
	state.candidatePasses = layer.passes;
	state.selectedLayer = null;
	state.selectedElement = null;
	state.candidatePreview = null;
	renderTree();
	renderLayerForm();
	renderPreview();
}

function renderLayerForm() {
	const container = document.getElementById('element-form');
	container.innerHTML = '';
	document.getElementById('form-title').textContent = 'Modifier la couche';

	const nameWrap = document.createElement('div');
	nameWrap.className = 'field';
	const nameLbl = document.createElement('label');
	nameLbl.textContent = 'Nom de la couche';
	const nameInput = document.createElement('input');
	nameInput.type = 'text';
	nameInput.value = state.candidateLayerName;
	nameInput.oninput = () => {
		state.candidateLayerName = nameInput.value;
	};
	nameWrap.append(nameLbl, nameInput);
	container.appendChild(nameWrap);

	const passesWrap = document.createElement('div');
	passesWrap.className = 'field';
	const passesLbl = document.createElement('label');
	passesLbl.textContent = 'Nombre de passes';
	const passesInput = document.createElement('input');
	passesInput.type = 'number';
	passesInput.step = '1';
	passesInput.min = '0';
	passesInput.value = state.candidatePasses;
	passesInput.oninput = () => {
		state.candidatePasses = parseInt(passesInput.value, 10) || 0;
	};
	passesWrap.append(passesLbl, passesInput);
	container.appendChild(passesWrap);

	const feedback = document.createElement('div');
	feedback.id = 'form-feedback';
	container.appendChild(feedback);

	const btnRow = document.createElement('div');
	btnRow.className = 'form-actions';
	btnRow.append(button('Enregistrer la couche', submitLayerEdit, 'primary'), button('Annuler', cancelForm));
	container.appendChild(btnRow);
}

async function submitLayerEdit() {
	try {
		await apiFetch(`/api/layers/${state.editingLayerIndex}`, {
			method: 'PUT',
			body: JSON.stringify({ layerName: state.candidateLayerName, passes: state.candidatePasses }),
		});
		toast('Couche mise à jour.');
		cancelForm();
		await refresh();
	} catch (e) {
		const feedback = document.getElementById('form-feedback');
		if (feedback) {
			feedback.textContent = e.message;
			feedback.className = 'error';
		}
		toast(e.message, true);
	}
}

async function deleteLayer(layerIndex) {
	if (!confirm('Supprimer cette couche et toutes ses formes ?')) return;
	try {
		await apiFetch(`/api/layers/${layerIndex}`, { method: 'DELETE' });
		toast('Couche supprimée.');
		cancelForm();
		await refresh();
	} catch (e) {
		toast(e.message, true);
	}
}

async function deleteElement(layerIndex, elementIndex) {
	if (!confirm('Supprimer cette forme ?')) return;
	try {
		await apiFetch(`/api/layers/${layerIndex}/elements/${elementIndex}`, { method: 'DELETE' });
		toast('Forme supprimée.');
		cancelForm();
		await refresh();
	} catch (e) {
		toast(e.message, true);
	}
}

// ---------------- Element form ----------------

function startNewElement(layerIndex) {
	state.formMode = 'element';
	state.editingLayerIndex = null;
	state.selectedLayer = layerIndex;
	state.selectedElement = null;
	state.editingSubType = 'Rectangle';
	state.candidateName = 'forme';
	state.candidateProperties = JSON.parse(JSON.stringify(DEFAULT_PROPERTIES.Rectangle));
	// Avoid briefly showing the previously-edited element's (now stale) live
	// preview mislabeled as this new one before its own preview resolves.
	state.candidatePreview = null;
	renderTree();
	renderForm();
}

function selectElement(layerIndex, elementIndex) {
	const element = state.project.layers[layerIndex].elements[elementIndex];
	state.formMode = 'element';
	state.editingLayerIndex = null;
	state.selectedLayer = layerIndex;
	state.selectedElement = elementIndex;
	state.editingSubType = element.subType;
	state.candidateName = element.name;
	state.candidateProperties = JSON.parse(JSON.stringify(element.properties));
	// Same as above: clear any stale preview from a previous selection so
	// currentShapes() falls back to this element's own saved shape until a
	// fresh validated preview comes back for it.
	state.candidatePreview = null;
	renderTree();
	renderForm();
}

function cancelForm() {
	state.formMode = null;
	state.editingLayerIndex = null;
	state.selectedLayer = null;
	state.selectedElement = null;
	state.candidatePreview = null;
	document.getElementById('element-form').innerHTML = '';
	document.getElementById('form-title').textContent = 'Sélectionnez ou ajoutez une forme';
	renderTree();
	renderPreview();
}

function renderForm() {
	const container = document.getElementById('element-form');
	container.innerHTML = '';
	const titleEl = document.getElementById('form-title');

	if (state.selectedLayer === null) {
		titleEl.textContent = 'Sélectionnez ou ajoutez une forme';
		return;
	}

	const isNew = state.selectedElement === null;
	titleEl.textContent = isNew ? 'Nouvelle forme' : 'Modifier la forme';

	const nameWrap = document.createElement('div');
	nameWrap.className = 'field';
	const nameLbl = document.createElement('label');
	nameLbl.textContent = 'Nom';
	const nameInput = document.createElement('input');
	nameInput.type = 'text';
	nameInput.value = state.candidateName;
	nameInput.oninput = () => {
		state.candidateName = nameInput.value;
		livePreview();
	};
	nameWrap.append(nameLbl, nameInput);
	container.appendChild(nameWrap);

	const typeWrap = document.createElement('div');
	typeWrap.className = 'field';
	const typeLbl = document.createElement('label');
	typeLbl.textContent = 'Type';
	const typeSelect = document.createElement('select');
	Object.keys(FORM_SCHEMAS).forEach((t) => {
		const o = document.createElement('option');
		o.value = t;
		o.textContent = t;
		if (t === state.editingSubType) o.selected = true;
		typeSelect.appendChild(o);
	});
	typeSelect.disabled = !isNew;
	typeSelect.onchange = () => {
		state.editingSubType = typeSelect.value;
		state.candidateProperties = JSON.parse(JSON.stringify(DEFAULT_PROPERTIES[state.editingSubType]));
		renderForm();
	};
	typeWrap.append(typeLbl, typeSelect);
	container.appendChild(typeWrap);

	const schema = FORM_SCHEMAS[state.editingSubType];
	const props = state.candidateProperties;
	schema.fields.forEach((f) => {
		if (f.type === 'point') renderPointField(container, f.label, props[f.key], livePreview);
		else if (f.type === 'number') renderNumberField(container, f.label, props, f.key, livePreview);
		else if (f.type === 'select') renderSelectField(container, f.label, props, f.key, f.options, livePreview);
		else if (f.type === 'pointList') renderPointListField(container, f.label, props[f.key], livePreview);
	});

	const feedback = document.createElement('div');
	feedback.id = 'form-feedback';
	container.appendChild(feedback);

	const btnRow = document.createElement('div');
	btnRow.className = 'form-actions';
	btnRow.append(
		button(isNew ? 'Ajouter' : 'Enregistrer la forme', submitElement, 'primary'),
		button('Annuler', cancelForm)
	);
	container.appendChild(btnRow);

	livePreview();
}

function renderPointField(container, label, value, onChange) {
	const wrap = document.createElement('div');
	wrap.className = 'field point-field';
	const lbl = document.createElement('label');
	lbl.textContent = label;
	wrap.appendChild(lbl);
	['x', 'y', 'z'].forEach((axis) => {
		const input = document.createElement('input');
		input.type = 'number';
		input.step = 'any';
		input.value = value[axis] ?? 0;
		input.placeholder = axis;
		input.oninput = () => {
			value[axis] = parseFloat(input.value) || 0;
			onChange();
		};
		wrap.appendChild(input);
	});
	container.appendChild(wrap);
}

function renderNumberField(container, label, obj, key, onChange) {
	const wrap = document.createElement('div');
	wrap.className = 'field';
	const lbl = document.createElement('label');
	lbl.textContent = label;
	const input = document.createElement('input');
	input.type = 'number';
	input.step = 'any';
	input.value = obj[key] ?? 0;
	input.oninput = () => {
		obj[key] = parseFloat(input.value) || 0;
		onChange();
	};
	wrap.append(lbl, input);
	container.appendChild(wrap);
}

function renderSelectField(container, label, obj, key, options, onChange) {
	const wrap = document.createElement('div');
	wrap.className = 'field';
	const lbl = document.createElement('label');
	lbl.textContent = label;
	const select = document.createElement('select');
	options.forEach((opt) => {
		const o = document.createElement('option');
		o.value = opt;
		o.textContent = opt;
		if (obj[key] === opt) o.selected = true;
		select.appendChild(o);
	});
	select.onchange = () => {
		obj[key] = select.value;
		onChange();
	};
	wrap.append(lbl, select);
	container.appendChild(wrap);
}

function renderPointListField(container, label, points, onChange) {
	const wrap = document.createElement('div');
	wrap.className = 'field point-list-field';
	const lbl = document.createElement('label');
	lbl.textContent = label;
	wrap.appendChild(lbl);

	points.forEach((pt, idx) => {
		const row = document.createElement('div');
		row.className = 'point-row';
		['x', 'y', 'z'].forEach((axis) => {
			const input = document.createElement('input');
			input.type = 'number';
			input.step = 'any';
			input.value = pt[axis] ?? 0;
			input.placeholder = axis;
			input.oninput = () => {
				pt[axis] = parseFloat(input.value) || 0;
				onChange();
			};
			row.appendChild(input);
		});
		row.appendChild(
			button('×', () => {
				points.splice(idx, 1);
				renderForm();
			}, 'danger small')
		);
		wrap.appendChild(row);
	});

	wrap.appendChild(
		button('+ point', () => {
			points.push({ x: 0, y: 0, z: 0 });
			renderForm();
		})
	);
	container.appendChild(wrap);
}

function buildCandidateElement() {
	return {
		name: state.candidateName,
		subType: state.editingSubType,
		properties: state.candidateProperties,
	};
}

let livePreviewTimer = null;
let livePreviewSeq = 0;
function livePreview() {
	clearTimeout(livePreviewTimer);
	// clearTimeout only cancels a timer that hasn't fired yet — if an earlier
	// call's 200ms already elapsed and its fetch is in flight when a newer
	// edit (e.g. a second quick drag) triggers another call, both requests
	// are now in flight and can resolve out of order. Without this sequence
	// guard, an older response arriving after a newer one would overwrite
	// state.candidatePreview with stale data, visibly reverting the just-made
	// edit on the canvas.
	const seq = ++livePreviewSeq;
	livePreviewTimer = setTimeout(async () => {
		const candidate = buildCandidateElement();
		try {
			const preview = await apiFetch('/api/preview/element', { method: 'POST', body: JSON.stringify(candidate) });
			if (seq !== livePreviewSeq) return;
			state.candidatePreview = preview;
			const feedback = document.getElementById('form-feedback');
			if (feedback) {
				feedback.textContent = preview.valid ? '' : `Invalide : ${preview.error}`;
				feedback.className = preview.valid ? 'ok' : 'error';
			}
		} catch (e) {
			if (seq !== livePreviewSeq) return;
			state.candidatePreview = null;
		}
		if (seq === livePreviewSeq) renderPreview();
	}, 200);
}

async function submitElement() {
	const candidate = buildCandidateElement();
	const layerIndex = state.selectedLayer;
	try {
		if (state.selectedElement === null) {
			await apiFetch(`/api/layers/${layerIndex}/elements`, { method: 'POST', body: JSON.stringify(candidate) });
			toast('Forme ajoutée.');
		} else {
			await apiFetch(`/api/layers/${layerIndex}/elements/${state.selectedElement}`, {
				method: 'PUT',
				body: JSON.stringify(candidate),
			});
			toast('Forme mise à jour.');
		}
		cancelForm();
		await refresh();
	} catch (e) {
		toast(e.message, true);
	}
}

// ---------------- SVG preview ----------------

const PADDING_MM = 5;
const SVG_NS = 'http://www.w3.org/2000/svg';

function computeBBox(shapes) {
	let minX = Infinity;
	let minY = Infinity;
	let maxX = -Infinity;
	let maxY = -Infinity;

	function extend(x, y, r = 0) {
		minX = Math.min(minX, x - r);
		maxX = Math.max(maxX, x + r);
		minY = Math.min(minY, y - r);
		maxY = Math.max(maxY, y + r);
	}

	shapes.forEach((s) => {
		const shp = s.shape;
		if (!shp) return;
		if (shp.type === 'circle') {
			extend(shp.center.x, shp.center.y, shp.radius);
		} else if (shp.type === 'arc') {
			extend(shp.from.x, shp.from.y, shp.radius);
			extend(shp.to.x, shp.to.y, shp.radius);
		} else if (shp.points) {
			shp.points.forEach((p) => extend(p.x, p.y));
		}
	});

	if (!isFinite(minX)) {
		minX = 0;
		minY = 0;
		maxX = 50;
		maxY = 50;
	}

	return {
		minX: minX - PADDING_MM,
		minY: minY - PADDING_MM,
		maxX: maxX + PADDING_MM,
		maxY: maxY + PADDING_MM,
	};
}

function shapeToSvgElement(shape) {
	if (shape.type === 'polygon' || shape.type === 'polyline') {
		const pts = shape.points.map((p) => `${p.x},${p.y}`).join(' ');
		const el = document.createElementNS(SVG_NS, shape.type === 'polygon' ? 'polygon' : 'polyline');
		el.setAttribute('points', pts);
		el.setAttribute('class', 'shape');
		return el;
	}
	if (shape.type === 'circle') {
		const el = document.createElementNS(SVG_NS, 'circle');
		el.setAttribute('cx', shape.center.x);
		el.setAttribute('cy', shape.center.y);
		el.setAttribute('r', shape.radius);
		el.setAttribute('class', 'shape');
		return el;
	}
	if (shape.type === 'arc') {
		const el = document.createElementNS(SVG_NS, 'path');
		// The surrounding <g> flips Y (matrix(1,0,0,-1,...)) to render our
		// Y-up mm coordinates on screen like the physical/G-code convention,
		// which reverses the naive sweep-flag mapping. Calibrated visually
		// against input-sample-router.json's ArcPath (CLOCKWISE, from (30,10)
		// to (35,15), radius 12).
		const sweep = shape.direction === 'CLOCKWISE' ? 0 : 1;
		const d = `M ${shape.from.x} ${shape.from.y} A ${shape.radius} ${shape.radius} 0 0 ${sweep} ${shape.to.x} ${shape.to.y}`;
		el.setAttribute('d', d);
		el.setAttribute('class', 'shape shape-arc');
		return el;
	}
	return null;
}

function currentShapes() {
	const editing = state.formMode === 'element';
	const isExistingEdit = editing && state.selectedElement !== null;

	// While editing an existing element, drop its saved (server) shape and show
	// only the live candidate in its place — otherwise the two would overlap
	// during a canvas drag instead of the one shape visibly moving.
	const shapes = state.shapes.filter(
		(s) => !(isExistingEdit && s.layerIndex === state.selectedLayer && s.elementIndex === state.selectedElement)
	);

	if (!editing) {
		return shapes;
	}

	const hasValidCandidate = state.candidatePreview && state.candidatePreview.valid;
	if (hasValidCandidate) {
		shapes.push({
			...state.candidatePreview,
			layerIndex: isExistingEdit ? state.selectedLayer : -1,
			elementIndex: isExistingEdit ? state.selectedElement : -1,
			isCandidate: !isExistingEdit,
			isEditing: true,
		});
	} else if (isExistingEdit) {
		// No validated candidate yet — e.g. right after selecting the element,
		// before the debounced /api/preview/element round trip resolves, or if
		// that call ever fails. Fall back to the last-saved shape instead of
		// leaving the canvas blank for the element being edited.
		const original = state.shapes.find(
			(s) => s.layerIndex === state.selectedLayer && s.elementIndex === state.selectedElement
		);
		if (original) {
			shapes.push({ ...original, isEditing: true });
		}
	}
	return shapes;
}

// Grows the view (never crops) so its width/height ratio matches the SVG
// panel's own pixel ratio. Without this, the browser's default
// preserveAspectRatio letterboxing would center-pad the content inside the
// panel on one axis, while the rulers (which map linearly across the panel's
// full clientWidth/clientHeight) know nothing about that padding — the two
// would drift out of sync. Keeping the ratios equal makes that padding zero.
function matchPanelAspect(view) {
	const svg = document.getElementById('preview-svg');
	const panelW = svg.clientWidth || 1;
	const panelH = svg.clientHeight || 1;
	const panelAspect = panelW / panelH;
	const viewAspect = view.width / view.height;

	if (viewAspect > panelAspect) {
		const newHeight = view.width / panelAspect;
		view.minY -= (newHeight - view.height) / 2;
		view.height = newHeight;
	} else {
		const newWidth = view.height * panelAspect;
		view.minX -= (newWidth - view.width) / 2;
		view.width = newWidth;
	}
}

function ensureView(bbox) {
	if (!state.view) {
		state.view = {
			minX: bbox.minX,
			minY: bbox.minY,
			width: bbox.maxX - bbox.minX,
			height: bbox.maxY - bbox.minY,
		};
		matchPanelAspect(state.view);
	}
}

function resetView() {
	const bbox = computeBBox(currentShapes());
	state.view = {
		minX: bbox.minX,
		minY: bbox.minY,
		width: bbox.maxX - bbox.minX,
		height: bbox.maxY - bbox.minY,
	};
	matchPanelAspect(state.view);
	applyView();
}

const MIN_SPAN_MM = 2;
const MAX_SPAN_MM = 5000;

function zoomBy(factor) {
	const view = state.view;
	if (!view) return;
	const cx = view.minX + view.width / 2;
	const cy = view.minY + view.height / 2;
	const newWidth = Math.min(MAX_SPAN_MM, Math.max(MIN_SPAN_MM, view.width * factor));
	const actualFactor = newWidth / view.width;
	view.width = newWidth;
	view.height = view.height * actualFactor;
	view.minX = cx - view.width / 2;
	view.minY = cy - view.height / 2;
	applyView();
}

// Picks a "nice" (1/2/5 * 10^n) step so grid lines and ruler ticks land
// roughly every targetPx pixels, however far zoomed in or out — keeps the
// grid readable instead of a dense mesh or an empty canvas.
function niceStep(spanMm, pixelSpan, targetPx = 60) {
	const unitsPerPixel = spanMm / Math.max(1, pixelSpan);
	const rawStep = unitsPerPixel * targetPx;
	const magnitude = Math.pow(10, Math.floor(Math.log10(rawStep)));
	const residual = rawStep / magnitude;
	let niceResidual;
	if (residual < 1.5) niceResidual = 1;
	else if (residual < 3.5) niceResidual = 2;
	else if (residual < 7.5) niceResidual = 5;
	else niceResidual = 10;
	return niceResidual * magnitude;
}

function formatTick(value, step) {
	const decimals = step < 1 ? Math.min(3, Math.ceil(-Math.log10(step))) : 0;
	return Number(value.toFixed(decimals)).toString();
}

function buildGrid(view, step) {
	const g = document.createElementNS(SVG_NS, 'g');
	g.setAttribute('id', 'grid-group');

	const firstX = Math.ceil(view.minX / step) * step;
	for (let x = firstX; x <= view.minX + view.width + 1e-9; x += step) {
		const line = document.createElementNS(SVG_NS, 'line');
		line.setAttribute('x1', x);
		line.setAttribute('x2', x);
		line.setAttribute('y1', view.minY);
		line.setAttribute('y2', view.minY + view.height);
		line.setAttribute('class', Math.abs(x) < step / 1000 ? 'grid-line grid-axis' : 'grid-line');
		g.appendChild(line);
	}

	const firstY = Math.ceil(view.minY / step) * step;
	for (let y = firstY; y <= view.minY + view.height + 1e-9; y += step) {
		const line = document.createElementNS(SVG_NS, 'line');
		line.setAttribute('y1', y);
		line.setAttribute('y2', y);
		line.setAttribute('x1', view.minX);
		line.setAttribute('x2', view.minX + view.width);
		line.setAttribute('class', Math.abs(y) < step / 1000 ? 'grid-line grid-axis' : 'grid-line');
		g.appendChild(line);
	}

	return g;
}

function buildRulers(view, step) {
	const topSvg = document.getElementById('ruler-top');
	const leftSvg = document.getElementById('ruler-left');
	topSvg.innerHTML = '';
	leftSvg.innerHTML = '';

	const pxW = topSvg.clientWidth || 1;
	const pxHTop = topSvg.clientHeight || 1;
	const pxWLeft = leftSvg.clientWidth || 1;
	const pxH = leftSvg.clientHeight || 1;

	topSvg.setAttribute('viewBox', `0 0 ${pxW} ${pxHTop}`);
	leftSvg.setAttribute('viewBox', `0 0 ${pxWLeft} ${pxH}`);

	const maxY = view.minY + view.height;

	const firstX = Math.ceil(view.minX / step) * step;
	for (let x = firstX; x <= view.minX + view.width + 1e-9; x += step) {
		const px = ((x - view.minX) / view.width) * pxW;
		const tick = document.createElementNS(SVG_NS, 'line');
		tick.setAttribute('x1', px);
		tick.setAttribute('x2', px);
		tick.setAttribute('y1', pxHTop - 6);
		tick.setAttribute('y2', pxHTop);
		tick.setAttribute('class', 'ruler-tick');
		topSvg.appendChild(tick);

		const label = document.createElementNS(SVG_NS, 'text');
		label.setAttribute('x', px + 2);
		label.setAttribute('y', pxHTop - 8);
		label.setAttribute('class', 'ruler-label');
		label.textContent = formatTick(x, step);
		topSvg.appendChild(label);
	}

	const firstY = Math.ceil(view.minY / step) * step;
	for (let y = firstY; y <= maxY + 1e-9; y += step) {
		const py = ((maxY - y) / view.height) * pxH;
		const tick = document.createElementNS(SVG_NS, 'line');
		tick.setAttribute('y1', py);
		tick.setAttribute('y2', py);
		tick.setAttribute('x1', pxWLeft - 6);
		tick.setAttribute('x2', pxWLeft);
		tick.setAttribute('class', 'ruler-tick');
		leftSvg.appendChild(tick);

		const label = document.createElementNS(SVG_NS, 'text');
		label.setAttribute('x', 2);
		label.setAttribute('y', Math.max(9, py - 2));
		label.setAttribute('class', 'ruler-label');
		label.textContent = formatTick(y, step);
		leftSvg.appendChild(label);
	}
}

// Single source of truth for the grid/ruler step: both are rebuilt together
// here from the exact same value every time the view changes (zoom, pan,
// reset, resize), so they can never drift apart the way they would if each
// recomputed its own step independently on different render passes.
function applyView() {
	const svg = document.getElementById('preview-svg');
	const view = state.view;
	if (!view) return;
	svg.setAttribute('viewBox', `${view.minX} ${view.minY} ${view.width} ${view.height}`);

	const step = niceStep(view.width, svg.clientWidth || 800);
	buildRulers(view, step);

	const oldGrid = svg.querySelector('#grid-group');
	if (oldGrid) {
		oldGrid.replaceWith(buildGrid(view, step));
	}

	// The content group's Y-flip pivot depends on view.minY/height, which pan
	// and zoom change constantly — if left at the value from the last full
	// renderPreview(), shapes and grid stay mirrored around a stale point while
	// the viewBox and rulers move around the current one, so content visibly
	// drifts out of sync with the rulers (scrolling the wrong way) as soon as
	// you pan or zoom. Recompute and reapply it here every time, too.
	const flipGroup = svg.querySelector('#flip-group');
	if (flipGroup) {
		flipGroup.setAttribute('transform', `matrix(1 0 0 -1 0 ${view.minY + view.minY + view.height})`);
	}
}

function shapesGroup(shapes, view) {
	// Reflection pivot: y' = pivot - y, keeps content inside the same [minY,maxY]
	// band while flipping our Y-up data into the correct visual orientation.
	const maxY = view.minY + view.height;
	const pivot = view.minY + maxY;

	const g = document.createElementNS(SVG_NS, 'g');
	g.setAttribute('id', 'flip-group');
	g.setAttribute('transform', `matrix(1 0 0 -1 0 ${pivot})`);

	// Placeholder grid group; applyView() (called right after this function
	// returns, from renderPreview()) replaces it using the authoritative step.
	g.appendChild(buildGrid(view, niceStep(view.width, document.getElementById('preview-svg').clientWidth || 800)));

	shapes.forEach((s) => {
		if (!s.shape) return;
		const el = shapeToSvgElement(s.shape);
		if (!el) return;
		const title = document.createElementNS(SVG_NS, 'title');
		title.textContent = `${s.elementName} (${s.subType})`;
		el.appendChild(title);

		if (s.isEditing) {
			el.classList.add('selected-shape', 'editable-shape');
			el.addEventListener('mousedown', (e) => startShapeDrag(e, bodyApplyDelta(s.subType)));
		} else if (s.layerIndex >= 0) {
			el.classList.add('selectable-shape');
			el.addEventListener('mousedown', (e) => {
				e.stopPropagation();
				selectElement(s.layerIndex, s.elementIndex);
			});
		}
		if (s.isCandidate) {
			el.classList.add('candidate-shape');
		}
		g.appendChild(el);

		// Handles are appended after (so on top of, for hit-testing) the shape
		// body they belong to — otherwise the body's own mousedown would win
		// over an overlapping handle and a resize/vertex drag would silently
		// turn into a move.
		if (s.isEditing) {
			addEditableHandles(g, view, s.subType, state.candidateProperties);
		}
	});

	return g;
}

// ---------------- Direct canvas editing ----------------

// Re-queries #flip-group by id rather than accepting it as a cached
// argument: renderPreview() replaces that element outright (svg.innerHTML =
// '') on every drag frame, so a reference captured once at drag-start would
// point at a detached node after the first redraw, and getScreenCTM() on a
// detached element returns null.
function svgPointFromEvent(e) {
	const svg = document.getElementById('preview-svg');
	const flipGroup = svg.querySelector('#flip-group');
	const pt = svg.createSVGPoint();
	pt.x = e.clientX;
	pt.y = e.clientY;
	return pt.matrixTransform(flipGroup.getScreenCTM().inverse());
}

// Mirrors the server's per-subtype geometry (ProjectService.extractGeometry)
// just enough to redraw instantly while dragging, without a round trip to
// /api/preview/element on every mousemove — that endpoint is still used
// (debounced, via renderForm()'s trailing livePreview()) once the drag ends,
// so validation errors still surface the same way as editing the form.
function localShapeFromProperties(subType, props) {
	if (subType === 'Rectangle') {
		const c = props.corner;
		return {
			type: 'polygon',
			points: [
				{ x: c.x, y: c.y },
				{ x: c.x, y: c.y + props.height },
				{ x: c.x + props.width, y: c.y + props.height },
				{ x: c.x + props.width, y: c.y },
			],
		};
	}
	if (subType === 'Circle') {
		return { type: 'circle', center: { x: props.center.x, y: props.center.y }, radius: props.radius };
	}
	if (subType === 'ArcPath') {
		return {
			type: 'arc',
			from: { x: props.from.x, y: props.from.y },
			to: { x: props.to.x, y: props.to.y },
			radius: props.radius,
			direction: props.direction,
		};
	}
	if (subType === 'PolyLineElement') {
		return { type: 'polyline', points: props.points.map((p) => ({ x: p.x, y: p.y })) };
	}
	return null;
}

function bodyApplyDelta(subType) {
	return (props, dx, dy) => {
		if (subType === 'Rectangle') {
			props.corner.x += dx;
			props.corner.y += dy;
		} else if (subType === 'Circle') {
			props.center.x += dx;
			props.center.y += dy;
		} else if (subType === 'ArcPath') {
			props.from.x += dx;
			props.from.y += dy;
			props.to.x += dx;
			props.to.y += dy;
		} else if (subType === 'PolyLineElement') {
			props.points.forEach((p) => {
				p.x += dx;
				p.y += dy;
			});
		}
	};
}

function addEditableHandles(g, view, subType, properties) {
	// Sized as a fraction of the current view span rather than a fixed mm
	// radius, so handles stay a roughly constant on-screen size at any zoom
	// level (view.width shrinks exactly as fast as pixels-per-mm grows).
	const handleR = view.width * 0.012;

	function handle(x, y, applyDelta) {
		const el = document.createElementNS(SVG_NS, 'circle');
		el.setAttribute('cx', x);
		el.setAttribute('cy', y);
		el.setAttribute('r', handleR);
		el.setAttribute('class', 'shape-handle');
		el.addEventListener('mousedown', (e) => startShapeDrag(e, applyDelta));
		g.appendChild(el);
	}

	if (subType === 'Rectangle') {
		const c = properties.corner;
		handle(c.x + properties.width, c.y + properties.height, (props, dx, dy) => {
			props.width = Math.max(0.1, props.width + dx);
			props.height = Math.max(0.1, props.height + dy);
		});
	} else if (subType === 'Circle') {
		const c = properties.center;
		handle(c.x + properties.radius, c.y, (props, dx, dy, cur) => {
			props.radius = Math.max(0.1, Math.hypot(cur.x - props.center.x, cur.y - props.center.y));
		});
	} else if (subType === 'ArcPath') {
		handle(properties.from.x, properties.from.y, (props, dx, dy, cur) => {
			props.from.x = cur.x;
			props.from.y = cur.y;
		});
		handle(properties.to.x, properties.to.y, (props, dx, dy, cur) => {
			props.to.x = cur.x;
			props.to.y = cur.y;
		});
	} else if (subType === 'PolyLineElement') {
		properties.points.forEach((p, idx) => {
			handle(p.x, p.y, (props, dx, dy, cur) => {
				props.points[idx].x = cur.x;
				props.points[idx].y = cur.y;
			});
		});
	}
}

// Drives both the "move whole shape" (dragging the shape body) and
// "adjust one point/dimension" (dragging a handle) cases: applyDelta mutates
// a fresh copy of the properties in place given the drag offset so far.
// Persisting is left to the existing "Enregistrer la forme" button — mouseup
// only resyncs the form fields and re-validates, matching how typing into the
// form already behaves, so a stray drag can't silently overwrite the project.
function startShapeDrag(e, applyDelta) {
	e.stopPropagation();
	e.preventDefault();
	if (state.formMode !== 'element' || !state.candidateProperties) return;

	const startPt = svgPointFromEvent(e);
	const startProps = JSON.parse(JSON.stringify(state.candidateProperties));
	const subType = state.editingSubType;

	function onMove(ev) {
		const cur = svgPointFromEvent(ev);
		const dx = cur.x - startPt.x;
		const dy = cur.y - startPt.y;
		const next = JSON.parse(JSON.stringify(startProps));
		applyDelta(next, dx, dy, cur);
		state.candidateProperties = next;
		state.candidatePreview = {
			valid: true,
			layerIndex: state.selectedElement !== null ? state.selectedLayer : -1,
			elementIndex: state.selectedElement !== null ? state.selectedElement : -1,
			elementName: state.candidateName,
			subType,
			isEditing: true,
			shape: localShapeFromProperties(subType, next),
		};
		renderPreview();
	}

	function onUp() {
		window.removeEventListener('mousemove', onMove);
		window.removeEventListener('mouseup', onUp);
		renderForm();
	}

	window.addEventListener('mousemove', onMove);
	window.addEventListener('mouseup', onUp);
}

function renderPreview() {
	const svg = document.getElementById('preview-svg');
	svg.innerHTML = '';

	const shapes = currentShapes();
	const bbox = computeBBox(shapes);
	ensureView(bbox);

	svg.appendChild(shapesGroup(shapes, state.view));
	applyView();
}

function setupCanvasInteraction() {
	const svg = document.getElementById('preview-svg');

	svg.addEventListener(
		'wheel',
		(e) => {
			e.preventDefault();
			if (!state.view) return;
			const rect = svg.getBoundingClientRect();
			const mouseXpx = e.clientX - rect.left;
			const mouseYpx = e.clientY - rect.top;
			const view = state.view;
			const userX = view.minX + (mouseXpx / rect.width) * view.width;
			const userY = view.minY + (mouseYpx / rect.height) * view.height;
			const factor = e.deltaY > 0 ? 1.15 : 1 / 1.15;
			const newWidth = Math.min(MAX_SPAN_MM, Math.max(MIN_SPAN_MM, view.width * factor));
			const actualFactor = newWidth / view.width;
			const newHeight = view.height * actualFactor;
			view.minX = userX - (mouseXpx / rect.width) * newWidth;
			view.minY = userY - (mouseYpx / rect.height) * newHeight;
			view.width = newWidth;
			view.height = newHeight;
			applyView();
		},
		{ passive: false }
	);

	let panState = null;
	svg.addEventListener('mousedown', (e) => {
		if (!state.view) return;
		panState = { startX: e.clientX, startY: e.clientY, view0: { ...state.view } };
		svg.classList.add('grabbing');
	});
	window.addEventListener('mousemove', (e) => {
		if (!panState) return;
		const rect = svg.getBoundingClientRect();
		const dxPx = e.clientX - panState.startX;
		const dyPx = e.clientY - panState.startY;
		state.view.minX = panState.view0.minX - (dxPx / rect.width) * panState.view0.width;
		state.view.minY = panState.view0.minY - (dyPx / rect.height) * panState.view0.height;
		applyView();
	});
	window.addEventListener('mouseup', () => {
		if (panState) {
			panState = null;
			svg.classList.remove('grabbing');
		}
	});
	// Safety net: if the button is released outside the window (or the window
	// loses focus mid-drag) and the mouseup above never fires, don't leave the
	// canvas panning on the next unrelated mouse move.
	window.addEventListener('blur', () => {
		if (panState) {
			panState = null;
			svg.classList.remove('grabbing');
		}
	});

	window.addEventListener('resize', () => {
		if (!state.view) return;
		matchPanelAspect(state.view);
		applyView();
	});
}

// ---------------- Wiring ----------------

document.getElementById('btn-add-layer').onclick = addLayer;

document.getElementById('btn-save-meta').onclick = async () => {
	const meta = {
		projectName: document.getElementById('meta-projectName').value,
		bitHead: document.getElementById('meta-bitHead').value,
		safeLevel: parseFloat(document.getElementById('meta-safeLevel').value) || 0,
		passIncrement: parseFloat(document.getElementById('meta-passIncrement').value) || 0,
		feedRate: parseFloat(document.getElementById('meta-feedRate').value) || 0,
		power: parseFloat(document.getElementById('meta-power').value) || 0,
	};
	try {
		await apiFetch('/api/project/meta', { method: 'PUT', body: JSON.stringify(meta) });
		toast('Métadonnées mises à jour.');
		await refresh();
	} catch (e) {
		toast(e.message, true);
	}
};

document.getElementById('btn-save').onclick = async () => {
	try {
		const result = await apiFetch('/api/project/save', { method: 'POST' });
		toast(`Projet enregistré : ${result.savedJsonPath}`);
	} catch (e) {
		toast(e.message, true);
	}
};

document.getElementById('btn-generate').onclick = async () => {
	try {
		const result = await apiFetch('/api/project/generate', { method: 'POST' });
		toast(`G-code généré : ${result.gcodePath}`);
	} catch (e) {
		toast(e.message, true);
	}
};

document.getElementById('btn-zoom-in').onclick = () => zoomBy(1 / 1.4);
document.getElementById('btn-zoom-out').onclick = () => zoomBy(1.4);
document.getElementById('btn-zoom-reset').onclick = resetView;

setupCanvasInteraction();

refresh().catch((e) => toast(e.message, true));
