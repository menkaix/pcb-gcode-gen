const state = {
	project: null,
	shapes: [],
	view: null, // { minX, minY, width, height } in mm; persists across re-renders until reset
	formMode: null, // null | 'layer' | 'element'
	editingLayerIndex: null,
	candidateLayerName: '',
	candidatePasses: 0,
	candidateTabsEnabled: false,
	candidateTabCount: 4,
	candidateTabWidth: 2,
	candidateHoleDepth: -1.6,
	selectedLayer: null,
	selectedElement: null,
	// Layer indices currently hidden from the canvas. Purely a client-side view
	// preference (not persisted with the project): unlike excludeFromGcode,
	// hiding a layer never changes what gets generated, only what's drawn.
	hiddenLayers: new Set(),
	// Pure display mirroring (e.g. to preview a PCB's underside): never touched
	// on the actual element properties, only on the render/interaction
	// transform, so toggling either flag back off always returns to the exact
	// same view — see flipTransform().
	flipH: false,
	flipV: false,
	editingSubType: null,
	candidateName: '',
	candidateProperties: null,
	candidatePreview: null,
	// Whether the small "Rotation" popover form is currently open for the
	// selected/new element. Reset whenever the selection changes (see
	// selectElement/startNewElement/cancelForm) so it never leaks across shapes.
	rotationFormOpen: false,
	// Blocks currently known from the last /api/blocks/reload or /api/blocks
	// call (id, blockName, repoUrl, componentCount) — populates the Block
	// form's blockId datalist and the "Bibliothèques de blocs" panel.
	blocks: [],
	// Per-repository ok/error status from the last reload, keyed by URL.
	blockRepoStatus: {},
	// Which of tree/preview/form is shown on the tablet/mobile tab bar; the
	// desktop grid ignores this and shows all three panels at once.
	activePanel: 'tree',
};

const DEFAULT_PROPERTIES = {
	Rectangle: { corner: { x: 0, y: 0, z: 0 }, width: 10, height: 10, rotation: 0 },
	Circle: { center: { x: 0, y: 0, z: 0 }, radius: 5 },
	ArcPath: {
		from: { x: 0, y: 0, z: 0 },
		to: { x: 10, y: 0, z: 0 },
		radius: 10,
		direction: 'CLOCKWISE',
		rotation: 0,
	},
	PolyLineElement: { points: [{ x: 0, y: 0, z: 0 }, { x: 10, y: 0, z: 0 }], rotation: 0 },
	BezierElement: {
		points: [
			{ x: 0, y: 0, z: 0 },
			{ x: 5, y: 15, z: 0 },
			{ x: 15, y: 15, z: 0 },
			{ x: 20, y: 0, z: 0 },
		],
		rotation: 0,
	},
	TextElement: {
		position: { x: 0, y: 0, z: 0 },
		text: 'Texte',
		fontSize: 10,
		fontFamily: 'SansSerif',
		bold: false,
		italic: false,
		rotation: 0,
	},
	TraceElement: {
		baseType: 'polyline',
		width: 1,
		points: [{ x: 0, y: 0, z: 0 }, { x: 10, y: 0, z: 0 }],
		rotation: 0,
	},
	HoleElement: {
		position: { x: 0, y: 0 },
	},
	Block: {
		blockId: '',
		position: { x: 0, y: 0 },
		rotation: 0,
	},
};

// Shape types whose geometry actually changes under rotation (Circle and
// HoleElement are single point/radius shapes, rotation-invariant around
// their own center, so the Rotation button is hidden for them).
const ROTATABLE_TYPES = new Set([
	'Rectangle',
	'ArcPath',
	'PolyLineElement',
	'BezierElement',
	'TextElement',
	'TraceElement',
	'Block',
]);

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
	BezierElement: {
		fields: [
			{ key: 'points', label: 'Points de contrôle (mm) : ancre, ctrl1, ctrl2, ancre, ...', type: 'pointList' },
		],
	},
	TextElement: {
		fields: [
			{ key: 'position', label: 'Position (mm, origine du texte)', type: 'point' },
			{ key: 'text', label: 'Texte', type: 'text' },
			{ key: 'fontSize', label: 'Taille de police (mm)', type: 'number' },
			{ key: 'fontFamily', label: 'Police (nom système ou chemin .ttf/.otf)', type: 'text', datalist: 'font-list' },
			{ key: 'bold', label: 'Gras', type: 'checkbox' },
			{ key: 'italic', label: 'Italique', type: 'checkbox' },
		],
	},
	TraceElement: {
		fields: [
			{ key: 'baseType', label: 'Type de tracé de base', type: 'select', options: ['polyline', 'bezier'] },
			{ key: 'width', label: 'Largeur de la piste (mm)', type: 'number' },
			{ key: 'points', label: 'Points (mm)', type: 'pointList' },
		],
	},
	HoleElement: {
		fields: [
			// No Z field: a hole's plunge depth is the owning layer's holeDepth
			// (see the layer form), not a per-element property.
			{ key: 'position', label: 'Position (mm)', type: 'point2d' },
		],
	},
	Block: {
		fields: [
			{ key: 'blockId', label: 'Bloc', type: 'text', datalist: 'block-list-datalist' },
			{ key: 'position', label: 'Position (mm)', type: 'point2d' },
		],
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

// Returns an <svg><use> pointing into the inline sprite defined in index.html.
function icon(name) {
	const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
	svg.setAttribute('class', 'icon');
	svg.setAttribute('aria-hidden', 'true');
	const use = document.createElementNS('http://www.w3.org/2000/svg', 'use');
	use.setAttribute('href', '#i-' + name);
	svg.appendChild(use);
	return svg;
}

// A button with an empty label but an icon becomes a square icon-only button
// (.icon-btn); pass a `title` so it still has a tooltip and accessible name.
function button(label, onClick, cls = '', title = '', iconName = '') {
	const b = document.createElement('button');
	b.type = 'button';
	if (iconName) b.appendChild(icon(iconName));
	if (label) {
		const span = document.createElement('span');
		span.textContent = label;
		b.appendChild(span);
	}
	b.className = iconName && !label ? `${cls} icon-btn`.trim() : cls;
	b.onclick = onClick;
	if (title) {
		b.title = title;
		if (!label) b.setAttribute('aria-label', title);
	}
	return b;
}

// ---------------- Dialogs (replace prompt()/confirm() for consistent touch/desktop UX) ----------------

// Resolves true/false. The dialog's own <form method="dialog"> sets
// dialog.returnValue to the clicked submit button's `value` (or '' if closed
// via Escape/backdrop), so a plain equality check tells confirm from cancel.
function confirmDialog(message) {
	const dialog = document.getElementById('confirm-dialog');
	document.getElementById('confirm-dialog-message').textContent = message;
	document.getElementById('confirm-dialog-input-wrap').classList.add('hidden');
	return new Promise((resolve) => {
		function onClose() {
			dialog.removeEventListener('close', onClose);
			resolve(dialog.returnValue === 'confirmed');
		}
		dialog.addEventListener('close', onClose);
		dialog.showModal();
	});
}

// Resolves the entered string, or null if cancelled — mirrors prompt()'s
// contract so call sites only need `if (name === null) return;`.
function promptDialog(message, defaultValue = '') {
	const dialog = document.getElementById('confirm-dialog');
	const input = document.getElementById('confirm-dialog-input');
	document.getElementById('confirm-dialog-message').textContent = message;
	document.getElementById('confirm-dialog-input-wrap').classList.remove('hidden');
	input.value = defaultValue;
	return new Promise((resolve) => {
		function onClose() {
			dialog.removeEventListener('close', onClose);
			resolve(dialog.returnValue === 'confirmed' ? input.value : null);
		}
		dialog.addEventListener('close', onClose);
		dialog.showModal();
		input.focus();
		input.select();
	});
}

// ---------------- Load / refresh ----------------

async function refresh() {
	const [project, shapes] = await Promise.all([apiFetch('/api/project'), apiFetch('/api/preview')]);
	state.project = project;
	state.shapes = shapes;
	renderMeta();
	renderTree();
	renderPreview();
	renderBlockRepositories();
}

// Populated once at startup; only used to suggest values for the TextElement
// fontFamily field via a <datalist>, so a stale list across a long session is
// harmless (worst case: a newly-installed font is missing from suggestions).
async function loadFontList() {
	try {
		const fonts = await apiFetch('/api/fonts');
		const datalist = document.getElementById('font-list');
		datalist.innerHTML = '';
		fonts.forEach((name) => {
			const option = document.createElement('option');
			option.value = name;
			datalist.appendChild(option);
		});
	} catch (e) {
		// Non-critical: the fontFamily field still works as a plain text input.
	}
}

// ---------------- Block libraries ----------------

// Refreshes the blockId <datalist> and the "Bibliothèques de blocs" panel's
// block list from the in-memory cache (no git clone/pull) - cheap enough to
// call after every reload and on startup.
async function loadBlockList() {
	try {
		state.blocks = await apiFetch('/api/blocks');
	} catch (e) {
		state.blocks = [];
	}
	const datalist = document.getElementById('block-list-datalist');
	datalist.innerHTML = '';
	state.blocks.forEach((b) => {
		const option = document.createElement('option');
		option.value = b.id;
		option.label = `${b.blockName} (${b.componentCount} formes)`;
		datalist.appendChild(option);
	});
	renderBlockListPanel();
}

function renderBlockListPanel() {
	const container = document.getElementById('block-list');
	container.innerHTML = '';
	if (state.blocks.length === 0) {
		container.textContent = 'Aucun bloc chargé. Ajoutez un dépôt puis cliquez sur "Recharger".';
		return;
	}
	const list = document.createElement('ul');
	list.style.margin = '0';
	list.style.paddingLeft = '1.1rem';
	state.blocks.forEach((b) => {
		const li = document.createElement('li');
		li.textContent = `${b.id} — ${b.blockName} (${b.componentCount} formes)`;
		list.appendChild(li);
	});
	container.appendChild(list);
}

function renderBlockRepositories() {
	const container = document.getElementById('block-repositories');
	if (!container) return;
	container.innerHTML = '';
	const urls = (state.project && state.project.blockRepositories) || [];
	urls.forEach((url, index) => {
		const item = document.createElement('div');
		item.className = 'block-repo-item';

		const urlSpan = document.createElement('span');
		urlSpan.className = 'repo-url';
		urlSpan.textContent = url;
		urlSpan.title = url;
		item.appendChild(urlSpan);

		const status = state.blockRepoStatus[url];
		if (status) {
			const statusSpan = document.createElement('span');
			statusSpan.className = 'repo-status ' + (status.ok ? 'ok' : 'error');
			statusSpan.textContent = status.ok ? `✓ ${status.blocksFound} bloc(s)` : `✗ ${status.error}`;
			statusSpan.title = status.ok ? '' : status.error;
			item.appendChild(statusSpan);
		}

		item.appendChild(button('', () => removeBlockRepository(index), 'danger small', 'Retirer ce dépôt', 'trash'));
		container.appendChild(item);
	});
}

async function saveBlockRepositories(urls) {
	const result = await apiFetch('/api/blocks/repositories', { method: 'PUT', body: JSON.stringify({ urls }) });
	state.project.blockRepositories = result.urls;
	renderBlockRepositories();
}

async function removeBlockRepository(index) {
	const urls = (state.project.blockRepositories || []).slice();
	urls.splice(index, 1);
	try {
		await saveBlockRepositories(urls);
	} catch (e) {
		toast(e.message, true);
	}
}

document.getElementById('btn-add-block-repo').onclick = async () => {
	const input = document.getElementById('block-repo-url');
	const url = input.value.trim();
	if (!url) return;
	const urls = (state.project.blockRepositories || []).concat([url]);
	try {
		await saveBlockRepositories(urls);
		input.value = '';
	} catch (e) {
		toast(e.message, true);
	}
};

document.getElementById('btn-reload-blocks').onclick = async () => {
	try {
		const result = await apiFetch('/api/blocks/reload', { method: 'POST' });
		state.blockRepoStatus = {};
		(result.repositories || []).forEach((status) => {
			state.blockRepoStatus[status.url] = status;
		});
		state.blocks = result.blocks || [];
		renderBlockRepositories();
		renderBlockListPanel();
		await loadBlockList();
		toast('Bibliothèques de blocs rechargées.');
	} catch (e) {
		toast(e.message, true);
	}
};

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

// Groups the two lower-frequency layer actions (Modifier/Supprimer) behind a
// "⋯" <details> popover instead of two more full-width buttons in the header
// row — keeps the row from wrapping badly in the ~15-20rem tree panel, on
// both desktop and the narrower mobile/tablet layouts.
function buildLayerMenu(layerIndex, layer) {
	const details = document.createElement('details');
	details.className = 'row-menu';

	const summary = document.createElement('summary');
	summary.appendChild(icon('more-h'));
	summary.title = 'Plus d’actions';
	summary.setAttribute('aria-label', 'Plus d’actions');
	details.appendChild(summary);

	const content = document.createElement('div');
	content.className = 'row-menu-content';
	content.appendChild(
		button('Modifier', () => {
			details.open = false;
			startEditLayer(layerIndex, layer);
		}, '', '', 'pencil')
	);
	content.appendChild(
		button(
			'Supprimer',
			() => {
				details.open = false;
				deleteLayer(layerIndex);
			},
			'danger',
			'',
			'trash'
		)
	);
	details.appendChild(content);

	// Only one row menu open at a time — closing the others on open avoids a
	// pile of stale popovers left open across re-renders of the tree.
	details.addEventListener('toggle', () => {
		if (!details.open) return;
		document.querySelectorAll('#layer-tree details.row-menu[open]').forEach((other) => {
			if (other !== details) other.open = false;
		});
	});

	return details;
}

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

		const tabsSpan = document.createElement('span');
		tabsSpan.className = 'muted';
		if (layer.tabsEnabled) {
			tabsSpan.textContent = ` · ${layer.tabCount} tabs × ${layer.tabWidth}mm`;
		}

		if (state.formMode === 'layer' && state.editingLayerIndex === li) {
			layerDiv.classList.add('editing');
		}

		const hidden = state.hiddenLayers.has(li);
		if (hidden) {
			layerDiv.classList.add('layer-hidden');
		}

		header.append(
			title,
			passesSpan,
			tabsSpan,
			button(
				'',
				() => toggleLayerVisibility(li),
				'small',
				hidden ? 'Couche masquée (cliquer pour afficher)' : 'Couche visible (cliquer pour masquer)',
				hidden ? 'eye-off' : 'eye'
			),
			button(
				'',
				() => toggleLayerGcodeExclusion(li, layer),
				layer.excludeFromGcode ? 'danger small' : 'small',
				layer.excludeFromGcode ? 'Exclue du G-code (cliquer pour inclure)' : 'Incluse dans le G-code (cliquer pour exclure)',
				layer.excludeFromGcode ? 'ban' : 'check-circle'
			),
			buildLayerMenu(li, layer),
			button('Forme', () => startNewElement(li), 'small', 'Ajouter une forme à cette couche', 'plus')
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
				button('', (e) => {
					e.stopPropagation();
					deleteElement(li, ei);
				}, 'danger small', 'Supprimer cette forme', 'trash')
			);
			item.onclick = () => selectElement(li, ei);
			list.appendChild(item);
		});
		layerDiv.appendChild(list);

		container.appendChild(layerDiv);
	});
}

async function addLayer() {
	const name = await promptDialog('Nom de la nouvelle couche', 'nouvelle-couche');
	if (name === null) return;
	try {
		await apiFetch('/api/layers', {
			method: 'POST',
			body: JSON.stringify({
				layerName: name,
				passes: 1,
				tabsEnabled: false,
				tabCount: 4,
				tabWidth: 2,
				excludeFromGcode: false,
				holeDepth: -1.6,
			}),
		});
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
	state.candidateTabsEnabled = !!layer.tabsEnabled;
	state.candidateTabCount = layer.tabCount || 4;
	state.candidateTabWidth = layer.tabWidth || 2;
	state.candidateHoleDepth = layer.holeDepth ?? -1.6;
	state.selectedLayer = null;
	state.selectedElement = null;
	state.candidatePreview = null;
	renderTree();
	renderLayerForm();
	renderPreview();
	setActivePanel('form');
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

	const tabsEnabledWrap = document.createElement('div');
	tabsEnabledWrap.className = 'field';
	const tabsEnabledLbl = document.createElement('label');
	tabsEnabledLbl.textContent = 'Tabs de maintien activés';
	const tabsEnabledInput = document.createElement('input');
	tabsEnabledInput.type = 'checkbox';
	tabsEnabledInput.checked = state.candidateTabsEnabled;
	tabsEnabledInput.oninput = () => {
		state.candidateTabsEnabled = tabsEnabledInput.checked;
	};
	tabsEnabledWrap.append(tabsEnabledLbl, tabsEnabledInput);
	container.appendChild(tabsEnabledWrap);

	const tabCountWrap = document.createElement('div');
	tabCountWrap.className = 'field';
	const tabCountLbl = document.createElement('label');
	tabCountLbl.textContent = 'Nombre de tabs';
	const tabCountInput = document.createElement('input');
	tabCountInput.type = 'number';
	tabCountInput.step = '1';
	tabCountInput.min = '1';
	tabCountInput.value = state.candidateTabCount;
	tabCountInput.oninput = () => {
		state.candidateTabCount = parseInt(tabCountInput.value, 10) || 0;
	};
	tabCountWrap.append(tabCountLbl, tabCountInput);
	container.appendChild(tabCountWrap);

	const tabWidthWrap = document.createElement('div');
	tabWidthWrap.className = 'field';
	const tabWidthLbl = document.createElement('label');
	tabWidthLbl.textContent = 'Largeur du tab (mm)';
	const tabWidthInput = document.createElement('input');
	tabWidthInput.type = 'number';
	tabWidthInput.step = '0.1';
	tabWidthInput.min = '0';
	tabWidthInput.value = state.candidateTabWidth;
	tabWidthInput.oninput = () => {
		state.candidateTabWidth = parseFloat(tabWidthInput.value) || 0;
	};
	tabWidthWrap.append(tabWidthLbl, tabWidthInput);
	container.appendChild(tabWidthWrap);

	const holeDepthWrap = document.createElement('div');
	holeDepthWrap.className = 'field';
	const holeDepthLbl = document.createElement('label');
	holeDepthLbl.textContent = 'Profondeur de perçage des trous (mm)';
	const holeDepthInput = document.createElement('input');
	holeDepthInput.type = 'number';
	holeDepthInput.step = 'any';
	holeDepthInput.value = state.candidateHoleDepth;
	holeDepthInput.oninput = () => {
		state.candidateHoleDepth = parseFloat(holeDepthInput.value) || 0;
	};
	holeDepthWrap.append(holeDepthLbl, holeDepthInput);
	container.appendChild(holeDepthWrap);

	const feedback = document.createElement('div');
	feedback.id = 'form-feedback';
	container.appendChild(feedback);

	const btnRow = document.createElement('div');
	btnRow.className = 'form-actions';
	btnRow.append(
		button('Enregistrer la couche', submitLayerEdit, 'primary', '', 'save'),
		button('Annuler', cancelForm, '', '', 'x')
	);
	container.appendChild(btnRow);
}

async function submitLayerEdit() {
	try {
		await apiFetch(`/api/layers/${state.editingLayerIndex}`, {
			method: 'PUT',
			body: JSON.stringify({
				layerName: state.candidateLayerName,
				passes: state.candidatePasses,
				tabsEnabled: state.candidateTabsEnabled,
				tabCount: state.candidateTabCount,
				tabWidth: state.candidateTabWidth,
				holeDepth: state.candidateHoleDepth,
			}),
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

function toggleLayerVisibility(layerIndex) {
	if (state.hiddenLayers.has(layerIndex)) {
		state.hiddenLayers.delete(layerIndex);
	} else {
		state.hiddenLayers.add(layerIndex);
	}
	renderTree();
	renderPreview();
}

async function toggleLayerGcodeExclusion(layerIndex, layer) {
	try {
		await apiFetch(`/api/layers/${layerIndex}`, {
			method: 'PUT',
			body: JSON.stringify({
				layerName: layer.layerName,
				passes: layer.passes,
				tabsEnabled: layer.tabsEnabled,
				tabCount: layer.tabCount,
				tabWidth: layer.tabWidth,
				excludeFromGcode: !layer.excludeFromGcode,
				holeDepth: layer.holeDepth,
			}),
		});
		await refresh();
	} catch (e) {
		toast(e.message, true);
	}
}

async function deleteLayer(layerIndex) {
	if (!(await confirmDialog('Supprimer cette couche et toutes ses formes ?'))) return;
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
	if (!(await confirmDialog('Supprimer cette forme ?'))) return;
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
	state.rotationFormOpen = false;
	renderTree();
	renderForm();
	setActivePanel('form');
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
	state.rotationFormOpen = false;
	renderTree();
	renderForm();
	setActivePanel('form');
}

function cancelForm() {
	state.formMode = null;
	state.editingLayerIndex = null;
	state.selectedLayer = null;
	state.selectedElement = null;
	state.candidatePreview = null;
	state.rotationFormOpen = false;
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
		state.rotationFormOpen = false;
		renderForm();
	};
	typeWrap.append(typeLbl, typeSelect);
	container.appendChild(typeWrap);

	const schema = FORM_SCHEMAS[state.editingSubType];
	const props = state.candidateProperties;
	schema.fields.forEach((f) => {
		if (f.type === 'point') renderPointField(container, f.label, props[f.key], livePreview);
		else if (f.type === 'point2d') renderPointField(container, f.label, props[f.key], livePreview, ['x', 'y']);
		else if (f.type === 'number') renderNumberField(container, f.label, props, f.key, livePreview);
		else if (f.type === 'select') renderSelectField(container, f.label, props, f.key, f.options, livePreview);
		else if (f.type === 'pointList') renderPointListField(container, f.label, props[f.key], livePreview);
		else if (f.type === 'text') renderTextField(container, f.label, props, f.key, livePreview, f.datalist);
		else if (f.type === 'checkbox') renderCheckboxField(container, f.label, props, f.key, livePreview);
	});

	if (ROTATABLE_TYPES.has(state.editingSubType) && state.rotationFormOpen) {
		renderRotationPanel(container, props);
	}

	const feedback = document.createElement('div');
	feedback.id = 'form-feedback';
	container.appendChild(feedback);

	const btnRow = document.createElement('div');
	btnRow.className = 'form-actions';
	if (ROTATABLE_TYPES.has(state.editingSubType)) {
		btnRow.append(
			button('Rotation', () => {
				state.rotationFormOpen = !state.rotationFormOpen;
				renderForm();
			}, '', '', 'rotate-cw')
		);
	}
	btnRow.append(
		button(isNew ? 'Ajouter' : 'Enregistrer la forme', submitElement, 'primary', '', isNew ? 'plus' : 'save'),
		button('Annuler', cancelForm, '', '', 'x')
	);
	container.appendChild(btnRow);

	livePreview();
}

// Small popover-style form opened by the "Rotation" button: a single angle
// input (degrees, positive = clockwise as displayed on screen) that stages
// props.rotation on the candidate element, same as any other field — still
// requires "Enregistrer la forme" to persist, and drives the live preview so
// the rotation is visible immediately while typing.
function renderRotationPanel(container, props) {
	const panel = document.createElement('div');
	panel.className = 'field rotation-panel';

	const lbl = document.createElement('label');
	lbl.textContent = 'Rotation (°, sens horaire = positif)';
	panel.appendChild(lbl);

	const input = document.createElement('input');
	input.type = 'number';
	input.step = 'any';
	input.value = props.rotation ?? 0;
	input.oninput = () => {
		props.rotation = parseFloat(input.value) || 0;
		livePreview();
	};
	panel.appendChild(input);

	panel.appendChild(
		button('', () => {
			state.rotationFormOpen = false;
			renderForm();
		}, 'small', 'Fermer le panneau de rotation', 'x')
	);

	container.appendChild(panel);
}

function renderPointField(container, label, value, onChange, axes = ['x', 'y', 'z']) {
	const wrap = document.createElement('div');
	wrap.className = 'field point-field';
	const lbl = document.createElement('label');
	lbl.textContent = label;
	wrap.appendChild(lbl);
	axes.forEach((axis) => {
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

function renderTextField(container, label, obj, key, onChange, datalistId) {
	const wrap = document.createElement('div');
	wrap.className = 'field';
	const lbl = document.createElement('label');
	lbl.textContent = label;
	const input = document.createElement('input');
	input.type = 'text';
	input.value = obj[key] ?? '';
	if (datalistId) input.setAttribute('list', datalistId);
	input.oninput = () => {
		obj[key] = input.value;
		onChange();
	};
	wrap.append(lbl, input);
	container.appendChild(wrap);
}

function renderCheckboxField(container, label, obj, key, onChange) {
	const wrap = document.createElement('div');
	wrap.className = 'field checkbox-field';
	const lbl = document.createElement('label');
	const input = document.createElement('input');
	input.type = 'checkbox';
	input.checked = !!obj[key];
	input.onchange = () => {
		obj[key] = input.checked;
		onChange();
	};
	lbl.append(input, document.createTextNode(label));
	wrap.appendChild(lbl);
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
			button('', () => {
				points.splice(idx, 1);
				renderForm();
			}, 'danger small', 'Supprimer ce point', 'trash')
		);
		wrap.appendChild(row);
	});

	wrap.appendChild(
		button('Point', () => {
			points.push({ x: 0, y: 0, z: 0 });
			renderForm();
		}, 'small', 'Ajouter un point', 'plus')
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
// Purely a visual marker size: HoleElement carries no diameter (the drill bit
// determines that, not the shape), so this is not a real dimension.
const HOLE_MARKER_RADIUS_MM = 0.4;

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
		} else if (shp.type === 'text' || shp.type === 'trace') {
			(shp.contours || []).forEach((contour) => contour.forEach((p) => extend(p.x, p.y)));
		} else if (shp.type === 'hole') {
			extend(shp.position.x, shp.position.y, HOLE_MARKER_RADIUS_MM);
		} else if (shp.points) {
			// Covers polygons, polylines, and bezier control points; a cubic
			// Bezier curve always stays within its control points' convex hull,
			// so bounding the raw control points is a safe (if slightly loose)
			// bound on the flattened curve without needing to tessellate here.
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
	if (shape.type === 'bezier') {
		const pts = shape.points;
		let d = `M ${pts[0].x} ${pts[0].y}`;
		for (let i = 1; i + 2 < pts.length; i += 3) {
			d += ` C ${pts[i].x} ${pts[i].y} ${pts[i + 1].x} ${pts[i + 1].y} ${pts[i + 2].x} ${pts[i + 2].y}`;
		}
		const el = document.createElementNS(SVG_NS, 'path');
		el.setAttribute('d', d);
		el.setAttribute('class', 'shape shape-arc');
		return el;
	}
	if (shape.type === 'hole') {
		const el = document.createElementNS(SVG_NS, 'circle');
		el.setAttribute('cx', shape.position.x);
		el.setAttribute('cy', shape.position.y);
		el.setAttribute('r', HOLE_MARKER_RADIUS_MM);
		el.setAttribute('class', 'shape shape-hole');
		return el;
	}
	if (shape.type === 'text' || shape.type === 'trace') {
		const d = (shape.contours || [])
			.filter((c) => c.length > 0)
			.map((c) => `M ${c[0].x} ${c[0].y} ` + c.slice(1).map((p) => `L ${p.x} ${p.y}`).join(' ') + ' Z')
			.join(' ');
		const el = document.createElementNS(SVG_NS, 'path');
		el.setAttribute('d', d || 'M 0 0 Z');
		el.setAttribute('class', 'shape');
		return el;
	}
	if (shape.type === 'group') {
		// A Block instance: its children were already rotated/translated
		// server-side (ProjectService.extractGeometry), so each is just rendered
		// with its own renderer and grouped, no extra transform needed here.
		const g = document.createElementNS(SVG_NS, 'g');
		g.setAttribute('class', 'shape-group');
		(shape.shapes || []).forEach((child) => {
			const childEl = shapeToSvgElement(child);
			if (childEl) g.appendChild(childEl);
		});
		return g;
	}
	return null;
}

// Resolves the Layer (with its tabsEnabled/tabCount/tabWidth settings) a
// preview shape belongs to. A brand-new, not-yet-saved element (isCandidate)
// carries layerIndex: -1 from currentShapes(), so its target layer is read
// from state.selectedLayer instead.
function layerForShape(s) {
	const layers = state.project && state.project.layers;
	if (!layers) return null;
	if (s.layerIndex >= 0) return layers[s.layerIndex] || null;
	if (s.isCandidate && state.selectedLayer !== null) return layers[state.selectedLayer] || null;
	return null;
}

function closedLoopWithDistances(points) {
	const loop = points.map((p) => ({ x: p.x, y: p.y }));
	loop.push(loop[0]);
	const cum = [0];
	for (let i = 1; i < loop.length; i++) {
		cum.push(cum[i - 1] + Math.hypot(loop[i].x - loop[i - 1].x, loop[i].y - loop[i - 1].y));
	}
	return { loop, cum };
}

function pointAtDistance(loop, cum, target) {
	const last = loop.length - 1;
	for (let i = 0; i < last; i++) {
		if (target <= cum[i + 1]) {
			const segLen = cum[i + 1] - cum[i];
			const t = segLen <= 0 ? 0 : (target - cum[i]) / segLen;
			const a = loop[i];
			const b = loop[i + 1];
			return { x: a.x + t * (b.x - a.x), y: a.y + t * (b.y - a.y) };
		}
	}
	return loop[last];
}

// Mirrors CircleGcodePath's tab-splitting math: tabCount tabs of angular
// width tabWidth/radius, evenly spaced, with the tool always cutting
// clockwise (Circle elements are always emitted as CLOCKWISE server-side).
function circleTabMarkers(shape, layer) {
	const tabCount = layer.tabCount;
	const tabWidth = layer.tabWidth;
	const radius = shape.radius;
	if (!(tabCount > 0) || !(tabWidth > 0) || !(radius > 0)) return [];

	const anglePerSlot = (2 * Math.PI) / tabCount;
	let tabAngle = tabWidth / radius;
	if (tabAngle >= anglePerSlot) tabAngle = anglePerSlot * 0.5;
	const cutAngle = anglePerSlot - tabAngle;
	const dirSign = -1;
	const theta0 = Math.PI;

	const markers = [];
	for (let k = 0; k < tabCount; k++) {
		const startAngle = theta0 + dirSign * (k * anglePerSlot + cutAngle);
		const endAngle = startAngle + dirSign * tabAngle;
		markers.push({
			arc: true,
			radius,
			from: {
				x: shape.center.x + radius * Math.cos(startAngle),
				y: shape.center.y + radius * Math.sin(startAngle),
			},
			to: { x: shape.center.x + radius * Math.cos(endAngle), y: shape.center.y + radius * Math.sin(endAngle) },
		});
	}
	return markers;
}

// Mirrors ClosedLineGcodePath's tab-splitting math: tabCount tabs of arc
// length tabWidth, evenly spaced by perimeter distance around the polygon.
function polygonTabMarkers(shape, layer) {
	const tabCount = layer.tabCount;
	const tabWidth = layer.tabWidth;
	if (!(tabCount > 0) || !(tabWidth > 0) || !shape.points || shape.points.length < 2) return [];

	const { loop, cum } = closedLoopWithDistances(shape.points);
	const perimeter = cum[cum.length - 1];
	if (!(perimeter > 0)) return [];

	const slot = perimeter / tabCount;
	let cutLength = slot - tabWidth;
	if (cutLength <= 0) cutLength = slot * 0.5;

	const markers = [];
	for (let k = 0; k < tabCount; k++) {
		const tabStart = k * slot + cutLength;
		const tabEnd = (k + 1) * slot;
		markers.push({ arc: false, from: pointAtDistance(loop, cum, tabStart), to: pointAtDistance(loop, cum, tabEnd) });
	}
	return markers;
}

function tabMarkersForShape(shape, layer) {
	if (!layer || !layer.tabsEnabled) return [];
	if (shape.type === 'circle') return circleTabMarkers(shape, layer);
	if (shape.type === 'polygon') return polygonTabMarkers(shape, layer);
	if (shape.type === 'text') {
		// Mirrors TextElement.reloadBehaviour(): one independent closed
		// ClosedLineGcodePath per glyph contour (so letters with counters, like
		// "O" or "A", get their own tabs on both the outer and inner outline).
		const markers = [];
		(shape.contours || []).forEach((contour) => {
			markers.push(...polygonTabMarkers({ points: contour }, layer));
		});
		return markers;
	}
	return [];
}

function buildTabMarkerElements(shape, layer) {
	return tabMarkersForShape(shape, layer).map((m) => {
		const el = document.createElementNS(SVG_NS, 'path');
		// Sweep flag matches the 'arc' shape type's convention: clockwise data
		// (as emitted for Circle tabs) renders correctly under this group's
		// Y-flip transform with sweep=0.
		const d = m.arc
			? `M ${m.from.x} ${m.from.y} A ${m.radius} ${m.radius} 0 0 0 ${m.to.x} ${m.to.y}`
			: `M ${m.from.x} ${m.from.y} L ${m.to.x} ${m.to.y}`;
		el.setAttribute('d', d);
		el.setAttribute('class', 'tab-marker');
		return el;
	});
}

function currentShapes() {
	const editing = state.formMode === 'element';
	const isExistingEdit = editing && state.selectedElement !== null;

	// While editing an existing element, drop its saved (server) shape and show
	// only the live candidate in its place — otherwise the two would overlap
	// during a canvas drag instead of the one shape visibly moving.
	const shapes = state.shapes.filter(
		(s) =>
			!state.hiddenLayers.has(s.layerIndex) &&
			!(isExistingEdit && s.layerIndex === state.selectedLayer && s.elementIndex === state.selectedElement)
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
		// Mirrors flipTransform()'s x handling, so a tick always sits above the
		// same physical mm mark on the canvas below, flipped or not.
		const xDisplay = state.flipH ? view.minX + view.minX + view.width - x : x;
		const px = ((xDisplay - view.minX) / view.width) * pxW;
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
		// Mirrors flipTransform()'s y handling (flipV cancels the mandatory
		// Y-up -> Y-down flip instead of compounding with it).
		const py = state.flipV ? ((y - view.minY) / view.height) * pxH : ((maxY - y) / view.height) * pxH;
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

// Single source of truth for the #flip-group transform: y always gets the
// mandatory Y-up (data) -> Y-down (SVG) flip, plus an optional extra mirror
// on either axis around the current view's own center, driven by
// state.flipH/flipV. Every shape (including text/trace contours, which are
// plain paths) lives inside this one <g>, so mirroring it here is enough to
// keep every shape type consistent — no per-type special-casing needed. Both
// shapesGroup() and applyView() call this so they can never disagree on the
// current transform, exactly like niceStep() is shared for the grid/rulers.
function flipTransform(view) {
	const pivotX = view.minX + (view.minX + view.width);
	const pivotY = view.minY + (view.minY + view.height);
	const a = state.flipH ? -1 : 1;
	const e = state.flipH ? pivotX : 0;
	const d = state.flipV ? 1 : -1;
	const f = state.flipV ? 0 : pivotY;
	return `matrix(${a} 0 0 ${d} ${e} ${f})`;
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

	// The content group's flip pivots depend on view.min*/width/height, which
	// pan and zoom change constantly — if left at the value from the last full
	// renderPreview(), shapes and grid stay mirrored around a stale point while
	// the viewBox and rulers move around the current one, so content visibly
	// drifts out of sync with the rulers (scrolling the wrong way) as soon as
	// you pan or zoom. Recompute and reapply it here every time, too.
	const flipGroup = svg.querySelector('#flip-group');
	if (flipGroup) {
		flipGroup.setAttribute('transform', flipTransform(view));
	}
}

function shapesGroup(shapes, view) {
	const g = document.createElementNS(SVG_NS, 'g');
	g.setAttribute('id', 'flip-group');
	// Placeholder transform; applyView() (called right after this function
	// returns, from renderPreview()) recomputes and reapplies the authoritative
	// one via the same flipTransform() helper.
	g.setAttribute('transform', flipTransform(view));

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
			el.addEventListener('pointerdown', (e) => startShapeDrag(e, bodyApplyDelta(s.subType)));
		} else if (s.layerIndex >= 0) {
			el.classList.add('selectable-shape');
			el.addEventListener('pointerdown', (e) => {
				e.stopPropagation();
				selectElement(s.layerIndex, s.elementIndex);
			});
		}
		if (s.isCandidate) {
			el.classList.add('candidate-shape');
		}
		g.appendChild(el);

		// Handles are appended after (so on top of, for hit-testing) the shape
		// body they belong to — otherwise the body's own pointerdown would win
		// over an overlapping handle and a resize/vertex drag would silently
		// turn into a move.
		if (s.isEditing) {
			addEditableHandles(g, view, s.subType, state.candidateProperties);
		}

		const layer = layerForShape(s);
		buildTabMarkerElements(s.shape, layer).forEach((marker) => g.appendChild(marker));
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
function localShapeFromProperties(subType, props, dx, dy, baseShape) {
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
	if (subType === 'BezierElement') {
		return { type: 'bezier', points: props.points.map((p) => ({ x: p.x, y: p.y })) };
	}
	if (subType === 'TextElement') {
		// Glyph outlines aren't reproducible client-side (no font engine here),
		// so a whole-shape drag instead translates the last server-rendered
		// contours by the drag offset, rather than going blank until the
		// debounced /api/preview/element round trip resolves after mouseup.
		if (!baseShape || baseShape.type !== 'text' || !Array.isArray(baseShape.contours)) return null;
		return {
			type: 'text',
			contours: baseShape.contours.map((contour) => contour.map((p) => ({ x: p.x + dx, y: p.y + dy }))),
		};
	}
	if (subType === 'TraceElement') {
		// The buffered stroke outline (with its true width) isn't reproducible
		// client-side (no polygon-offset library here), so this shows just the
		// thin centerline as an instant drag placeholder; the debounced
		// /api/preview/element round trip fills in the real buffered outline.
		return { type: 'polyline', points: props.points.map((p) => ({ x: p.x, y: p.y })) };
	}
	if (subType === 'HoleElement') {
		return { type: 'hole', position: { x: props.position.x, y: props.position.y } };
	}
	if (subType === 'Block') {
		// A block's own geometry (its resolved children) isn't reproducible
		// client-side (it depends on the block library JSON), so a whole-shape
		// drag instead translates the last server-rendered group by the drag
		// offset, the same trick TextElement uses above for its glyph contours.
		if (!baseShape || baseShape.type !== 'group') return null;
		return translateShape(baseShape, dx, dy);
	}
	return null;
}

// Recursively translates a /api/preview shape by (dx, dy), used to redraw a
// Block's group instantly while dragging (see localShapeFromProperties).
function translateShape(shape, dx, dy) {
	if (!shape) return null;
	if (shape.type === 'polygon' || shape.type === 'polyline') {
		return { type: shape.type, points: shape.points.map((p) => ({ x: p.x + dx, y: p.y + dy })) };
	}
	if (shape.type === 'circle') {
		return { type: 'circle', center: { x: shape.center.x + dx, y: shape.center.y + dy }, radius: shape.radius };
	}
	if (shape.type === 'arc') {
		return {
			type: 'arc',
			from: { x: shape.from.x + dx, y: shape.from.y + dy },
			to: { x: shape.to.x + dx, y: shape.to.y + dy },
			radius: shape.radius,
			direction: shape.direction,
		};
	}
	if (shape.type === 'bezier') {
		return { type: 'bezier', points: shape.points.map((p) => ({ x: p.x + dx, y: p.y + dy })) };
	}
	if (shape.type === 'text' || shape.type === 'trace') {
		return {
			type: shape.type,
			contours: (shape.contours || []).map((c) => c.map((p) => ({ x: p.x + dx, y: p.y + dy }))),
		};
	}
	if (shape.type === 'hole') {
		return { type: 'hole', position: { x: shape.position.x + dx, y: shape.position.y + dy } };
	}
	if (shape.type === 'group') {
		return { type: 'group', shapes: (shape.shapes || []).map((child) => translateShape(child, dx, dy)) };
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
		} else if (subType === 'PolyLineElement' || subType === 'BezierElement' || subType === 'TraceElement') {
			props.points.forEach((p) => {
				p.x += dx;
				p.y += dy;
			});
		} else if (subType === 'TextElement' || subType === 'HoleElement' || subType === 'Block') {
			props.position.x += dx;
			props.position.y += dy;
		}
	};
}

const COARSE_POINTER = window.matchMedia('(pointer: coarse)').matches;

function addEditableHandles(g, view, subType, properties) {
	// Sized as a fraction of the current view span rather than a fixed mm
	// radius, so handles stay a roughly constant on-screen size at any zoom
	// level (view.width shrinks exactly as fast as pixels-per-mm grows).
	// Touch pointers get a bigger fraction — a fingertip needs a larger visual
	// (and hit-test) target than a mouse cursor to reliably grab a handle.
	const handleR = view.width * (COARSE_POINTER ? 0.022 : 0.012);

	function handle(x, y, applyDelta) {
		const el = document.createElementNS(SVG_NS, 'circle');
		el.setAttribute('cx', x);
		el.setAttribute('cy', y);
		el.setAttribute('r', handleR);
		el.setAttribute('class', 'shape-handle');
		el.addEventListener('pointerdown', (e) => startShapeDrag(e, applyDelta));
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
	} else if (subType === 'PolyLineElement' || subType === 'BezierElement' || subType === 'TraceElement') {
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

	const svg = document.getElementById('preview-svg');
	const pointerId = e.pointerId;
	// setPointerCapture redirects every subsequent event for this pointer to
	// svg regardless of where it moves on screen — the touch/mouse equivalent
	// of the old window-level mousemove/mouseup listeners, but it also keeps
	// working if the pointer leaves the window, so no blur safety net needed.
	svg.setPointerCapture(pointerId);

	const startPt = svgPointFromEvent(e);
	const startProps = JSON.parse(JSON.stringify(state.candidateProperties));
	const subType = state.editingSubType;
	// Captured once, at drag start: the baseline shape TextElement's drag
	// translation is applied to (see localShapeFromProperties).
	const baseShape = state.candidatePreview && state.candidatePreview.valid ? state.candidatePreview.shape : null;

	function onMove(ev) {
		if (ev.pointerId !== pointerId) return;
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
			shape: localShapeFromProperties(subType, next, dx, dy, baseShape),
		};
		renderPreview();
	}

	function onUp(ev) {
		if (ev.pointerId !== pointerId) return;
		svg.removeEventListener('pointermove', onMove);
		svg.removeEventListener('pointerup', onUp);
		svg.removeEventListener('pointercancel', onUp);
		if (svg.hasPointerCapture(pointerId)) svg.releasePointerCapture(pointerId);
		renderForm();
	}

	// Listeners live on svg (never replaced) rather than the handle/shape
	// element itself, which renderPreview() tears down and rebuilds on every
	// drag frame (svg.innerHTML = '').
	svg.addEventListener('pointermove', onMove);
	svg.addEventListener('pointerup', onUp);
	svg.addEventListener('pointercancel', onUp);
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

	// Pan (one pointer) and pinch-zoom (two pointers) share the same pointer
	// tracking map: a mouse drag and a single-finger touch drag both land in
	// panState, and a second finger touching down promotes the gesture to
	// pinchState instead. Using Pointer Events (rather than separate
	// mouse/touch handlers) means both device classes go through one code
	// path, and setPointerCapture makes the old window-level mouseup/blur
	// safety net unnecessary — a captured pointer keeps reporting to svg even
	// once it leaves the window, and losing capture unexpectedly fires
	// pointercancel, which endPointer() already handles the same as pointerup.
	const activePointers = new Map();
	let panState = null;
	let pinchState = null;

	function currentPoints() {
		return [...activePointers.values()];
	}

	svg.addEventListener('pointerdown', (e) => {
		if (!state.view) return;
		svg.setPointerCapture(e.pointerId);
		activePointers.set(e.pointerId, { x: e.clientX, y: e.clientY });

		if (activePointers.size === 1) {
			panState = { startX: e.clientX, startY: e.clientY, view0: { ...state.view } };
			svg.classList.add('grabbing');
		} else if (activePointers.size === 2) {
			panState = null;
			svg.classList.remove('grabbing');
			const [p1, p2] = currentPoints();
			pinchState = {
				startDist: Math.max(1, Math.hypot(p2.x - p1.x, p2.y - p1.y)),
				view0: { ...state.view },
			};
		}
	});

	svg.addEventListener('pointermove', (e) => {
		if (!activePointers.has(e.pointerId)) return;
		activePointers.set(e.pointerId, { x: e.clientX, y: e.clientY });

		if (pinchState && activePointers.size >= 2) {
			const [p1, p2] = currentPoints();
			const rect = svg.getBoundingClientRect();
			const dist = Math.max(1, Math.hypot(p2.x - p1.x, p2.y - p1.y));
			const midXpx = (p1.x + p2.x) / 2 - rect.left;
			const midYpx = (p1.y + p2.y) / 2 - rect.top;
			const view0 = pinchState.view0;
			// Re-derive the zoom center from view0 (the view as it was when the
			// pinch started) every move, using the current on-screen midpoint —
			// that keeps the mm point under the fingers fixed even as the
			// midpoint itself drifts while the fingers also pan together.
			const userX = view0.minX + (midXpx / rect.width) * view0.width;
			const userY = view0.minY + (midYpx / rect.height) * view0.height;
			const factor = pinchState.startDist / dist;
			const newWidth = Math.min(MAX_SPAN_MM, Math.max(MIN_SPAN_MM, view0.width * factor));
			const actualFactor = newWidth / view0.width;
			const newHeight = view0.height * actualFactor;
			state.view.minX = userX - (midXpx / rect.width) * newWidth;
			state.view.minY = userY - (midYpx / rect.height) * newHeight;
			state.view.width = newWidth;
			state.view.height = newHeight;
			applyView();
			return;
		}

		if (panState && activePointers.size === 1) {
			const rect = svg.getBoundingClientRect();
			const dxPx = e.clientX - panState.startX;
			const dyPx = e.clientY - panState.startY;
			state.view.minX = panState.view0.minX - (dxPx / rect.width) * panState.view0.width;
			state.view.minY = panState.view0.minY - (dyPx / rect.height) * panState.view0.height;
			applyView();
		}
	});

	function endPointer(e) {
		if (!activePointers.has(e.pointerId)) return;
		activePointers.delete(e.pointerId);
		if (svg.hasPointerCapture(e.pointerId)) svg.releasePointerCapture(e.pointerId);

		if (activePointers.size < 2) {
			pinchState = null;
		}
		if (activePointers.size === 1) {
			// One finger remains after a pinch — resume panning from its current
			// position rather than the (now stale) original pan start point.
			const [p] = currentPoints();
			panState = { startX: p.x, startY: p.y, view0: { ...state.view } };
			svg.classList.add('grabbing');
		} else if (activePointers.size === 0) {
			panState = null;
			svg.classList.remove('grabbing');
		}
	}

	svg.addEventListener('pointerup', endPointer);
	svg.addEventListener('pointercancel', endPointer);

	// Recomputes the view/rulers whenever the panel's own pixel size changes,
	// for any reason — window resize, but also a tab switch or header
	// collapse/expand on narrow layouts, none of which fire a window 'resize'
	// event the old listener relied on.
	let resizeFrame = null;
	const resizeObserver = new ResizeObserver(() => {
		if (!state.view) return;
		if (resizeFrame) cancelAnimationFrame(resizeFrame);
		resizeFrame = requestAnimationFrame(() => {
			matchPanelAspect(state.view);
			applyView();
		});
	});
	resizeObserver.observe(svg);
}

// ---------------- Import / export ----------------

function downloadBlob(filename, blob) {
	const url = URL.createObjectURL(blob);
	const link = document.createElement('a');
	link.href = url;
	link.download = filename;
	document.body.appendChild(link);
	link.click();
	link.remove();
	URL.revokeObjectURL(url);
}

function downloadJson(filename, data) {
	downloadBlob(filename, new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' }));
}

function downloadText(filename, text) {
	downloadBlob(filename, new Blob([text], { type: 'text/plain' }));
}

async function importProjectFile(file) {
	let parsed;
	try {
		parsed = JSON.parse(await file.text());
	} catch (e) {
		toast(`Fichier JSON invalide : ${e.message}`, true);
		return;
	}
	try {
		await apiFetch('/api/project', { method: 'PUT', body: JSON.stringify(parsed) });
		// Layer/element indices from before the import may no longer point to
		// anything meaningful, so drop any open selection instead of trying to
		// keep it.
		cancelForm();
		await refresh();
		toast('Projet importé.');
	} catch (e) {
		toast(e.message, true);
	}
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

document.getElementById('btn-import').onclick = () => document.getElementById('file-import').click();

document.getElementById('file-import').onchange = async (e) => {
	const file = e.target.files[0];
	e.target.value = ''; // allow re-selecting the same file later
	if (file) {
		await importProjectFile(file);
	}
};

document.getElementById('btn-export-project').onclick = () => {
	const name = (state.project && state.project.projectName) || 'projet';
	downloadJson(`${name}.json`, state.project);
};

document.getElementById('btn-export-gcode').onclick = async () => {
	try {
		const result = await apiFetch('/api/project/gcode');
		downloadText(`${result.projectName || 'projet'}.nc`, result.gcode);
	} catch (e) {
		toast(e.message, true);
	}
};

// Single-slot save in the browser's localStorage: saving always overwrites
// the previous snapshot, loading pushes it back to the server like an import.
const LOCAL_PROJECT_KEY = 'pcb-gcode-gen.project';

document.getElementById('btn-save').onclick = () => {
	try {
		localStorage.setItem(LOCAL_PROJECT_KEY, JSON.stringify(state.project));
		toast(`Projet « ${state.project.projectName || 'sans nom'} » enregistré dans le navigateur.`);
	} catch (e) {
		toast(`Impossible d'enregistrer le projet : ${e.message}`, true);
	}
};

document.getElementById('btn-load').onclick = async () => {
	const raw = localStorage.getItem(LOCAL_PROJECT_KEY);
	if (raw === null) {
		toast('Aucun projet enregistré dans le navigateur.', true);
		return;
	}
	let parsed;
	try {
		parsed = JSON.parse(raw);
	} catch (e) {
		toast(`Sauvegarde illisible : ${e.message}`, true);
		return;
	}
	const name = parsed.projectName || 'sans nom';
	if (!(await confirmDialog(`Charger le projet « ${name} » ? Le projet en cours sera remplacé.`))) {
		return;
	}
	try {
		await apiFetch('/api/project', { method: 'PUT', body: JSON.stringify(parsed) });
		// Same as importProjectFile(): indices from before the load may no
		// longer point to anything meaningful, so drop any open selection.
		cancelForm();
		await refresh();
		toast(`Projet « ${name} » chargé.`);
	} catch (e) {
		toast(e.message, true);
	}
};

document.getElementById('btn-zoom-in').onclick = () => zoomBy(1 / 1.4);
document.getElementById('btn-zoom-out').onclick = () => zoomBy(1.4);
document.getElementById('btn-zoom-reset').onclick = resetView;

function toggleFlip(axis) {
	const btn = document.getElementById(axis === 'h' ? 'btn-flip-h' : 'btn-flip-v');
	if (axis === 'h') {
		state.flipH = !state.flipH;
		btn.classList.toggle('active', state.flipH);
	} else {
		state.flipV = !state.flipV;
		btn.classList.toggle('active', state.flipV);
	}
	applyView();
}

document.getElementById('btn-flip-h').onclick = () => toggleFlip('h');
document.getElementById('btn-flip-v').onclick = () => toggleFlip('v');

// ---------------- Panel navigation (tablet/mobile tab bar) ----------------

function setActivePanel(panel) {
	state.activePanel = panel;
	document.body.dataset.activePanel = panel;
	document.querySelectorAll('.panel-tab').forEach((btn) => {
		btn.classList.toggle('active', btn.dataset.panel === panel);
	});
}

document.querySelectorAll('.panel-tab').forEach((btn) => {
	btn.addEventListener('click', () => setActivePanel(btn.dataset.panel));
});

setActivePanel(state.activePanel);

// The tablet layout (640-1023px) pins the preview panel permanently above
// the tree/form tabs and has no "preview" tab of its own (see style.css) —
// if a resize lands here while "preview" was the active tab (e.g. coming
// from mobile), fall back to "tree" so a tab bar entry is always selected.
const tabletQuery = window.matchMedia('(min-width: 640px) and (max-width: 1023px)');
function syncActivePanelForBreakpoint() {
	if (tabletQuery.matches && state.activePanel === 'preview') {
		setActivePanel('tree');
	}
}
tabletQuery.addEventListener('change', syncActivePanelForBreakpoint);
syncActivePanelForBreakpoint();

// ---------------- Header: collapsible meta panel + file menu ----------------

// Meta fields default open on desktop (≥1024px, matching the pre-responsive
// behavior) and collapsed on tablet/mobile, but stay a native <details> so
// the user can always toggle it manually afterwards — this only sets the
// initial state and re-syncs when a resize crosses the breakpoint.
const metaDetails = document.getElementById('meta-details');
const wideQuery = window.matchMedia('(min-width: 1024px)');
function syncMetaDetailsForBreakpoint() {
	metaDetails.open = wideQuery.matches;
}
wideQuery.addEventListener('change', syncMetaDetailsForBreakpoint);
syncMetaDetailsForBreakpoint();

const fileMenu = document.querySelector('.file-menu');
const fileMenuToggle = document.getElementById('btn-file-menu-toggle');
const fileMenuContent = document.getElementById('file-menu-content');

function closeFileMenu() {
	fileMenu.classList.remove('open');
	fileMenuToggle.setAttribute('aria-expanded', 'false');
}

fileMenuToggle.addEventListener('click', () => {
	const isOpen = fileMenu.classList.toggle('open');
	fileMenuToggle.setAttribute('aria-expanded', String(isOpen));
});

// Any action inside the menu (import/export) should close it, same as a
// native <select> or menu closes after picking an item.
fileMenuContent.addEventListener('click', (e) => {
	// closest() rather than tagName: the buttons contain <svg>/<span> children,
	// so e.target is usually one of those, not the <button> itself.
	if (e.target.closest('button')) closeFileMenu();
});

document.addEventListener('click', (e) => {
	if (fileMenu.classList.contains('open') && !fileMenu.contains(e.target)) {
		closeFileMenu();
	}
});

document.addEventListener('keydown', (e) => {
	if (e.key === 'Escape' && fileMenu.classList.contains('open')) {
		closeFileMenu();
		fileMenuToggle.focus();
	}
});

setupCanvasInteraction();

refresh().catch((e) => toast(e.message, true));
loadFontList();
loadBlockList();
