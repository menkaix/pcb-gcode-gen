package com.menkaix.blocks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.menkaix.elements.Element;
import com.menkaix.geometry.components.SimplePoint;
import com.menkaix.pcbgcode.utilities.MissingPropertyException;

/**
 * Turns a {@code Block} instance element ({@code blockId}/{@code position}/
 * {@code rotation}) into the raw (subType + properties, not yet
 * {@code ElementFactory}-resolved) child elements it places: every child's
 * own point-valued properties are rigidly rotated around the block's local
 * origin (0,0) and translated by the block's position, while every other
 * property (dimensions, text, radius...) is copied unchanged - the
 * components' own properties stay fixed, only the group's placement moves.
 *
 * <p>
 * A handful of native shapes ({@code Rectangle}, {@code TextElement}, and
 * {@code Block} itself when nested) synthesize their geometry from a single
 * anchor point plus a local template - width/height, glyph outlines, a
 * nested block's own children - oriented by their own {@code rotation}
 * property. For those, the block's rotation must additionally be added to
 * that {@code rotation} property, or only the anchor moves and the shape
 * itself never turns with the block. Every other native shape
 * ({@code PolyLineElement}, {@code BezierElement}, {@code TraceElement},
 * {@code ArcPath}) stores its actual geometry as an explicit list/pair of
 * points and derives its own rotation pivot from those points at reload
 * time, so rotating the points alone already produces the correct composite
 * rotation; adding to {@code rotation} there would rotate it twice.
 */
public class BlockResolver {

	private static final Set<String> ANCHOR_TEMPLATE_TYPES = Set.of("Rectangle", "TextElement", "Block");

	private BlockResolver() {
	}

	public static List<Element> resolveTransformedChildren(Element blockElement) throws MissingPropertyException {

		String blockId = requireString(blockElement, "blockId");
		SimplePoint translation = requirePoint(blockElement, "position");

		double rotationDegrees = 0.0;
		Object rotationRaw = blockElement.getProperty("rotation");
		if (rotationRaw != null) {
			rotationDegrees = Double.parseDouble(rotationRaw.toString());
		}

		BlockLibraryService library = BlockLibraryService.getInstance();
		BlockDefinition definition = library == null ? null : library.getBlockDefinition(blockId);
		if (definition == null) {
			throw new MissingPropertyException("unknown block: " + blockId);
		}

		List<Element> result = new ArrayList<>();
		for (Element childDef : definition.getElements()) {
			Element transformed = new Element();
			transformed.setElementName(childDef.getElementName());
			transformed.setSubType(childDef.getSubType());

			HashMap<String, Object> newProperties = new HashMap<>();
			for (Map.Entry<String, Object> entry : childDef.getProperties().entrySet()) {
				newProperties.put(entry.getKey(), transformValue(entry.getValue(), rotationDegrees, translation));
			}
			if (ANCHOR_TEMPLATE_TYPES.contains(childDef.getSubType())) {
				addRotation(newProperties, rotationDegrees);
			}
			transformed.setProperties(newProperties);
			result.add(transformed);
		}
		return result;
	}

	private static Object transformValue(Object value, double rotationDegrees, SimplePoint translation) {
		if (value instanceof Map) {
			Map<?, ?> map = (Map<?, ?>) value;
			if (map.containsKey("x") && map.containsKey("y")) {
				return transformPoint(map, rotationDegrees, translation);
			}
			return value;
		}
		if (value instanceof List) {
			List<Object> transformedList = new ArrayList<>();
			for (Object item : (List<?>) value) {
				transformedList.add(transformValue(item, rotationDegrees, translation));
			}
			return transformedList;
		}
		return value;
	}

	private static Map<String, Object> transformPoint(Map<?, ?> map, double rotationDegrees, SimplePoint translation) {
		SimplePoint point = new SimplePoint(toDouble(map.get("x")), toDouble(map.get("y")));
		if (rotationDegrees != 0.0) {
			point = SimplePoint.rotate(point, new SimplePoint(0, 0, 0), rotationDegrees);
		}
		point = SimplePoint.add(point, translation);

		Map<String, Object> ans = new LinkedHashMap<>();
		ans.put("x", point.getX());
		ans.put("y", point.getY());
		if (map.containsKey("z")) {
			ans.put("z", toDouble(map.get("z")));
		}
		return ans;
	}

	private static void addRotation(HashMap<String, Object> properties, double rotationDegrees) {
		if (rotationDegrees == 0.0) {
			return;
		}
		Object existing = properties.get("rotation");
		double current = existing == null ? 0.0 : Double.parseDouble(existing.toString());
		properties.put("rotation", current + rotationDegrees);
	}

	private static double toDouble(Object value) {
		return ((Number) value).doubleValue();
	}

	private static String requireString(Element element, String key) throws MissingPropertyException {
		Object value = element.getProperty(key);
		if (value == null) {
			throw new MissingPropertyException("missing property -" + key);
		}
		return value.toString();
	}

	private static SimplePoint requirePoint(Element element, String key) throws MissingPropertyException {
		Object value = element.getProperty(key);
		if (value == null) {
			throw new MissingPropertyException("missing property -" + key);
		}
		if (value instanceof SimplePoint) {
			return (SimplePoint) value;
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> map = (Map<String, Object>) value;
		if (!map.containsKey("x") || !map.containsKey("y")) {
			throw new MissingPropertyException("missing property -" + key);
		}
		return new SimplePoint(toDouble(map.get("x")), toDouble(map.get("y")));
	}

}
