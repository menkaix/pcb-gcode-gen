package com.menkaix.project;

import java.util.ArrayList;
import java.util.List;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.operation.union.UnaryUnionOp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.menkaix.elements.Element;
import com.menkaix.elements.TraceElement;
import com.menkaix.elements.TraceIslandElement;
import com.menkaix.geometry.basic.PolyGone;
import com.menkaix.geometry.components.SimplePoint;

/**
 * Merges the copper footprints of every {@link TraceElement} in a resolved
 * layer into "islands": overlapping or touching traces are unioned into one
 * continuous copper shape, so that isolation cuts are only ever made around
 * the merged boundary and never run through copper shared between two
 * connected traces. Disjoint trace groups remain separate islands.
 *
 * <p>
 * Must run after every element in the layer has been resolved by
 * {@code ElementFactory} (so all {@code TraceElement.getBufferedGeometry()}
 * are populated) and before the layer is handed off to G-code generation,
 * since bare {@code TraceElement}s carry no {@code GcodeBehaviour} of their
 * own.
 */
public final class TraceMerger {

	private static final Logger LOGGER = LoggerFactory.getLogger(TraceMerger.class);

	private TraceMerger() {
	}

	public static void mergeInPlace(Layer layer) {
		List<Element> elements = layer.getElements();

		List<TraceElement> traces = new ArrayList<>();
		for (Element element : elements) {
			if (element instanceof TraceElement) {
				traces.add((TraceElement) element);
			}
		}

		if (traces.isEmpty()) {
			return;
		}

		LOGGER.debug("Merging {} trace(s) in layer '{}'.", traces.size(), layer.getLayerName());

		List<org.locationtech.jts.geom.Geometry> bufferedGeometries = new ArrayList<>();
		for (TraceElement trace : traces) {
			bufferedGeometries.add(trace.getBufferedGeometry());
		}

		org.locationtech.jts.geom.Geometry merged = UnaryUnionOp.union(bufferedGeometries);

		List<TraceIslandElement> islands = new ArrayList<>();
		for (int i = 0; i < merged.getNumGeometries(); i++) {
			org.locationtech.jts.geom.Geometry geometryN = merged.getGeometryN(i);
			if (!(geometryN instanceof Polygon)) {
				continue;
			}
			Polygon polygon = (Polygon) geometryN;

			List<PolyGone> rings = new ArrayList<>();
			rings.add(toPolyGone(polygon.getExteriorRing()));
			for (int h = 0; h < polygon.getNumInteriorRing(); h++) {
				rings.add(toPolyGone(polygon.getInteriorRingN(h)));
			}

			islands.add(new TraceIslandElement(rings));
		}

		elements.removeIf(element -> element instanceof TraceElement);
		elements.addAll(islands);

		LOGGER.debug("Layer '{}': {} trace(s) merged into {} island(s).", layer.getLayerName(), traces.size(),
				islands.size());
	}

	/**
	 * JTS rings repeat their first coordinate as the last one to close the loop;
	 * {@link PolyGone}/{@code ClosedLineGcodePath} already close the loop
	 * themselves (same convention as {@code TextElement}'s glyph contours), so
	 * that duplicated closing coordinate is dropped here.
	 */
	private static PolyGone toPolyGone(LineString ring) {
		PolyGone polyGone = new PolyGone();
		Coordinate[] coordinates = ring.getCoordinates();
		int count = coordinates.length;
		if (count > 1 && coordinates[0].equals2D(coordinates[count - 1])) {
			count--;
		}
		for (int i = 0; i < count; i++) {
			Coordinate c = coordinates[i];
			polyGone.addPoint(c.x, c.y);
		}
		return polyGone;
	}

}
