const state = {
	project: null,
	shapes: [],
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

		header.append(
			title,
			passesSpan,
			button('Renommer', () => promptRenameLayer(li, layer)),
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

async function promptRenameLayer(layerIndex, layer) {
	const name = prompt('Nom de la couche', layer.layerName);
	if (name === null) return;
	const passesStr = prompt('Nombre de passes', String(layer.passes));
	if (passesStr === null) return;
	try {
		await apiFetch(`/api/layers/${layerIndex}`, {
			method: 'PUT',
			body: JSON.stringify({ layerName: name, passes: parseInt(passesStr, 10) || 0 }),
		});
		toast('Couche mise à jour.');
		await refresh();
	} catch (e) {
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
	state.selectedLayer = layerIndex;
	state.selectedElement = null;
	state.editingSubType = 'Rectangle';
	state.candidateName = 'forme';
	state.candidateProperties = JSON.parse(JSON.stringify(DEFAULT_PROPERTIES.Rectangle));
	renderTree();
	renderForm();
}

function selectElement(layerIndex, elementIndex) {
	const element = state.project.layers[layerIndex].elements[elementIndex];
	state.selectedLayer = layerIndex;
	state.selectedElement = elementIndex;
	state.editingSubType = element.subType;
	state.candidateName = element.name;
	state.candidateProperties = JSON.parse(JSON.stringify(element.properties));
	renderTree();
	renderForm();
}

function cancelForm() {
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
function livePreview() {
	clearTimeout(livePreviewTimer);
	livePreviewTimer = setTimeout(async () => {
		const candidate = buildCandidateElement();
		try {
			const preview = await apiFetch('/api/preview/element', { method: 'POST', body: JSON.stringify(candidate) });
			state.candidatePreview = preview;
			const feedback = document.getElementById('form-feedback');
			if (feedback) {
				feedback.textContent = preview.valid ? '' : `Invalide : ${preview.error}`;
				feedback.className = preview.valid ? 'ok' : 'error';
			}
		} catch (e) {
			state.candidatePreview = null;
		}
		renderPreview();
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

function renderPreview() {
	const svg = document.getElementById('preview-svg');
	svg.innerHTML = '';

	const shapes = [...state.shapes];
	if (state.candidatePreview && state.candidatePreview.valid) {
		shapes.push({ ...state.candidatePreview, isCandidate: true });
	}

	const bbox = computeBBox(shapes);
	const width = bbox.maxX - bbox.minX;
	const height = bbox.maxY - bbox.minY;
	svg.setAttribute('viewBox', `${bbox.minX} ${bbox.minY} ${width} ${height}`);

	// Reflection pivot: y' = pivot - y, keeps content inside the same [minY,maxY]
	// band while flipping our Y-up data into the correct visual orientation.
	const pivot = bbox.minY + bbox.maxY;

	const g = document.createElementNS(SVG_NS, 'g');
	g.setAttribute('transform', `matrix(1 0 0 -1 0 ${pivot})`);

	shapes.forEach((s) => {
		if (!s.shape) return;
		const el = shapeToSvgElement(s.shape);
		if (!el) return;
		const title = document.createElementNS(SVG_NS, 'title');
		title.textContent = `${s.elementName} (${s.subType})`;
		el.appendChild(title);
		if (s.layerIndex === state.selectedLayer && s.elementIndex === state.selectedElement) {
			el.classList.add('selected-shape');
		}
		if (s.isCandidate) {
			el.classList.add('candidate-shape');
		}
		g.appendChild(el);
	});

	svg.appendChild(g);
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

refresh().catch((e) => toast(e.message, true));
