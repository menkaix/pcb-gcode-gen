package com.menkaix.web;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.menkaix.elements.ArcPath;
import com.menkaix.elements.Circle;
import com.menkaix.elements.Element;
import com.menkaix.elements.PolyLineElement;
import com.menkaix.elements.Rectangle;
import com.menkaix.elements.factory.ElementFactory;
import com.menkaix.geometry.components.SimplePoint;
import com.menkaix.pcbgcode.utilities.DuplicateLayerNameException;
import com.menkaix.pcbgcode.utilities.MissingPropertyException;
import com.menkaix.pcbgcode.utilities.UnknownElementException;
import com.menkaix.project.GcodeProject;
import com.menkaix.project.GcodeProjectDefinition;
import com.menkaix.project.Layer;

/**
 * Holds the single in-memory, editable project definition for the local web
 * GUI. This is a local, single-user tool: a coarse lock around every public
 * method favors correctness over throughput.
 */
@Service
public class ProjectService {

	private static final Logger LOGGER = LoggerFactory.getLogger(ProjectService.class);

	private final Object lock = new Object();
	private final Gson gson = new GsonBuilder().create();

	private GcodeProjectDefinition definition = new GcodeProjectDefinition();

	public void loadFromFile(String path) {
		synchronized (lock) {
			try {
				String content = Files.readString(Path.of(path));
				definition = gson.fromJson(content, GcodeProjectDefinition.class);
				LOGGER.info("Loaded project definition from: {}", path);
			} catch (IOException e) {
				LOGGER.warn("Could not read project file '{}', starting with an empty project.", path, e);
				definition = new GcodeProjectDefinition();
			}
		}
	}

	public GcodeProjectDefinition getDefinition() {
		synchronized (lock) {
			return definition;
		}
	}

	public GcodeProjectDefinition updateMeta(GcodeProjectDefinition meta) {
		synchronized (lock) {
			if (meta.getBitHead() != null) {
				definition.setBitHead(meta.getBitHead());
			}
			if (meta.getProjectName() != null) {
				definition.setProjectName(meta.getProjectName());
			}
			if (meta.getProjectFolder() != null) {
				definition.setProjectFolder(meta.getProjectFolder());
			}
			definition.setSafeLevel(meta.getSafeLevel());
			definition.setPassIncrement(meta.getPassIncrement());
			if (meta.getFeedRate() != null) {
				definition.setFeedRate(meta.getFeedRate());
			}
			if (meta.getPower() != null) {
				definition.setPower(meta.getPower());
			}
			return definition;
		}
	}

	// ---------------- Layers ----------------

	public List<Map<String, Object>> listLayers() {
		synchronized (lock) {
			List<Map<String, Object>> ans = new ArrayList<>();
			List<Layer> layers = definition.getLayers();
			for (int i = 0; i < layers.size(); i++) {
				ans.add(summarizeLayer(i, layers.get(i)));
			}
			return ans;
		}
	}

	public Layer getLayerAt(int index) {
		synchronized (lock) {
			checkLayerIndex(index);
			return definition.getLayers().get(index);
		}
	}

	public Map<String, Object> addLayer(LayerRequest request) throws DuplicateLayerNameException {
		synchronized (lock) {
			Layer layer = new Layer(request.layerName);
			layer.setPasses(request.passes);
			definition.addLayer(layer);
			return summarizeLayer(definition.getLayers().size() - 1, layer);
		}
	}

	public Map<String, Object> updateLayer(int index, LayerRequest request) throws DuplicateLayerNameException {
		synchronized (lock) {
			checkLayerIndex(index);
			List<Layer> layers = definition.getLayers();
			for (int i = 0; i < layers.size(); i++) {
				if (i != index && layers.get(i).getLayerName().equalsIgnoreCase(request.layerName)) {
					throw new DuplicateLayerNameException();
				}
			}
			Layer layer = layers.get(index);
			layer.setLayerName(request.layerName);
			layer.setPasses(request.passes);
			return summarizeLayer(index, layer);
		}
	}

	public void removeLayerAt(int index) {
		synchronized (lock) {
			checkLayerIndex(index);
			definition.removeLayerAt(index);
		}
	}

	private Map<String, Object> summarizeLayer(int index, Layer layer) {
		Map<String, Object> ans = new LinkedHashMap<>();
		ans.put("index", index);
		ans.put("layerName", layer.getLayerName());
		ans.put("passes", layer.getPasses());
		ans.put("elementCount", layer.getElements() == null ? 0 : layer.getElements().size());
		return ans;
	}

	// ---------------- Elements ----------------

	public List<Element> listElements(int layerIndex) {
		synchronized (lock) {
			return getLayerAt(layerIndex).getElements();
		}
	}

	public Element getElementAt(int layerIndex, int elementIndex) {
		synchronized (lock) {
			Layer layer = getLayerAt(layerIndex);
			checkElementIndex(layer, elementIndex);
			return layer.getElements().get(elementIndex);
		}
	}

	public int addElement(int layerIndex, Element element) throws MissingPropertyException, UnknownElementException {
		synchronized (lock) {
			Layer layer = getLayerAt(layerIndex);
			validateElement(element);
			layer.getElements().add(element);
			return layer.getElements().size() - 1;
		}
	}

	public void updateElement(int layerIndex, int elementIndex, Element element)
			throws MissingPropertyException, UnknownElementException {
		synchronized (lock) {
			Layer layer = getLayerAt(layerIndex);
			checkElementIndex(layer, elementIndex);
			validateElement(element);
			layer.getElements().set(elementIndex, element);
		}
	}

	public void removeElement(int layerIndex, int elementIndex) {
		synchronized (lock) {
			Layer layer = getLayerAt(layerIndex);
			checkElementIndex(layer, elementIndex);
			layer.getElements().remove(elementIndex);
		}
	}

	private void validateElement(Element element) throws MissingPropertyException, UnknownElementException {
		// Result discarded: this only exercises the same reloadBehaviour() validation
		// that ElementFactory.create() already performs for the batch/CLI pipeline.
		ElementFactory.create(element);
	}

	private void checkLayerIndex(int index) {
		List<Layer> layers = definition.getLayers();
		if (layers == null || index < 0 || index >= layers.size()) {
			throw new ResourceNotFoundException("Layer index out of range: " + index);
		}
	}

	private void checkElementIndex(Layer layer, int index) {
		List<Element> elements = layer.getElements();
		if (elements == null || index < 0 || index >= elements.size()) {
			throw new ResourceNotFoundException("Element index out of range: " + index);
		}
	}

	// ---------------- Save / generate ----------------

	public Map<String, Object> save() {
		synchronized (lock) {
			GcodeProject project = definition.generate();
			project.saveJson("");
			Map<String, Object> ans = new LinkedHashMap<>();
			ans.put("savedJsonPath", project.getProjectName() + ".json");
			ans.put("layerCount", project.getLayers().size());
			ans.put("elementCount", countElements(project));
			return ans;
		}
	}

	public Map<String, Object> generateAndWrite() {
		synchronized (lock) {
			GcodeProject project = definition.generate();
			project.saveJson("");
			project.writeGcode();
			Map<String, Object> ans = new LinkedHashMap<>();
			ans.put("savedJsonPath", project.getProjectName() + ".json");
			ans.put("gcodePath", project.getProjectName() + ".nc");
			ans.put("layerCount", project.getLayers().size());
			ans.put("elementCount", countElements(project));
			return ans;
		}
	}

	private int countElements(GcodeProject project) {
		int count = 0;
		for (Layer layer : project.getLayers()) {
			count += layer.getElements().size();
		}
		return count;
	}

	// ---------------- Preview ----------------

	public List<PreviewShape> previewAll() {
		synchronized (lock) {
			List<PreviewShape> shapes = new ArrayList<>();
			List<Layer> layers = definition.getLayers();
			for (int li = 0; li < layers.size(); li++) {
				Layer layer = layers.get(li);
				List<Element> elements = layer.getElements();
				for (int ei = 0; ei < elements.size(); ei++) {
					shapes.add(buildPreview(li, layer.getLayerName(), ei, elements.get(ei)));
				}
			}
			return shapes;
		}
	}

	public PreviewShape previewCandidate(Element element) {
		return buildPreview(-1, null, -1, element);
	}

	private PreviewShape buildPreview(int layerIndex, String layerName, int elementIndex, Element raw) {
		PreviewShape preview = new PreviewShape();
		preview.layerIndex = layerIndex;
		preview.layerName = layerName;
		preview.elementIndex = elementIndex;
		preview.elementName = raw.getElementName();
		preview.subType = raw.getSubType();
		try {
			Element resolved = ElementFactory.create(raw);
			preview.valid = true;
			preview.shape = extractGeometry(resolved);
		} catch (Exception e) {
			preview.valid = false;
			preview.error = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
		}
		return preview;
	}

	private Map<String, Object> extractGeometry(Element resolved) {
		Map<String, Object> shape = new LinkedHashMap<>();

		if (resolved instanceof Rectangle) {
			Rectangle rectangle = (Rectangle) resolved;
			shape.put("type", "polygon");
			shape.put("points", toPointMaps(rectangle.getGeometry().getPoints()));

		} else if (resolved instanceof Circle) {
			Circle circle = (Circle) resolved;
			shape.put("type", "circle");
			shape.put("center", toPointMap(circle.getCenter()));
			shape.put("radius", circle.getRadius());

		} else if (resolved instanceof ArcPath) {
			ArcPath arc = (ArcPath) resolved;
			// ArcPath.getFrom()/getTo() cast the raw property directly, which is still a
			// Gson LinkedTreeMap after JSON deserialization (init() never writes the
			// resolved SimplePoint back into properties). getGeometry() holds the
			// properly resolved PointCouple instead, so use that.
			List<SimplePoint> fromTo = arc.getGeometry().getPoints();
			shape.put("type", "arc");
			shape.put("from", toPointMap(fromTo.get(0)));
			shape.put("to", toPointMap(fromTo.get(1)));
			shape.put("radius", arc.getRadius());
			Object direction = arc.getProperty("direction");
			shape.put("direction", direction == null ? null : direction.toString());

		} else if (resolved instanceof PolyLineElement) {
			@SuppressWarnings("unchecked")
			List<SimplePoint> points = (List<SimplePoint>) resolved.getProperty("points");
			shape.put("type", "polyline");
			shape.put("points", toPointMaps(points));

		} else {
			shape.put("type", "unknown");
		}

		return shape;
	}

	private List<Map<String, Double>> toPointMaps(List<SimplePoint> points) {
		List<Map<String, Double>> ans = new ArrayList<>();
		for (SimplePoint point : points) {
			ans.add(toPointMap(point));
		}
		return ans;
	}

	private Map<String, Double> toPointMap(SimplePoint point) {
		Map<String, Double> ans = new LinkedHashMap<>();
		ans.put("x", point.getX());
		ans.put("y", point.getY());
		return ans;
	}

}
