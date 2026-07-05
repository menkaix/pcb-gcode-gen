package com.menkaix.elements;

import java.util.ArrayList;
import java.util.List;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;

import com.menkaix.geometry.basic.BezierCurve;
import com.menkaix.geometry.basic.PolyLine;
import com.menkaix.geometry.components.SimplePoint;
import com.menkaix.pcbgcode.utilities.MissingPropertyException;

/**
 * A PCB copper trace: a centerline path (polyline or bezier) with a width.
 * The centerline is buffered (offset by width/2 on both sides, round joins
 * and caps) into a JTS polygon representing the actual copper footprint.
 *
 * <p>
 * A bare TraceElement's {@link #getBehaviours()} intentionally contains only
 * the centerline geometry marker and no {@code GcodeBehaviour}: an isolated
 * trace never emits G-code by itself. Traces that touch or overlap must be
 * unioned across the whole layer first (see {@code TraceMerger}) so that an
 * isolation cut never runs through copper shared between two connected
 * traces; only the resulting merged islands (as {@code TraceIslandElement})
 * carry the actual cut paths. Any code path that iterates a layer's elements
 * looking for GcodeBehaviours before {@code TraceMerger.mergeInPlace()} has
 * run will silently skip Trace elements.
 */
public class TraceElement extends Element {

	private static final long serialVersionUID = 5471186404608886349L;

	private transient List<SimplePoint> centerlinePoints = new ArrayList<>();
	private transient double width;
	private transient org.locationtech.jts.geom.Geometry bufferedGeometry;

	@Override
	public void reloadBehaviour() throws MissingPropertyException {
		checkMandatoryProperties("baseType", "points", "width");

		String baseType = getProperty("baseType").toString();
		width = Double.parseDouble(getProperty("width").toString());
		if (width <= 0) {
			throw new MissingPropertyException("width must be > 0, got " + width);
		}

		List<SimplePoint> points = new ArrayList<>();

		@SuppressWarnings("unchecked")
		ArrayList<Object> objs = (ArrayList<Object>) getProperty("points");
		for (Object object : objs) {
			points.add(pointFromMap(object));
		}

		com.menkaix.project.behaviours.Geometry centerline;
		if ("polyline".equals(baseType)) {
			if (points.size() < 2) {
				throw new MissingPropertyException("points must contain at least 2 points for a polyline trace; got "
						+ points.size());
			}
			PolyLine polyLine = new PolyLine();
			polyLine.getPoints().addAll(points);
			centerline = polyLine;
		} else if ("bezier".equals(baseType)) {
			if (points.size() < 4 || (points.size() - 1) % 3 != 0) {
				throw new MissingPropertyException(
						"points must contain a start anchor followed by groups of 3 (control, control, anchor); got "
								+ points.size());
			}
			centerline = new BezierCurve(points);
		} else {
			throw new MissingPropertyException("baseType must be 'polyline' or 'bezier', got '" + baseType + "'");
		}

		centerlinePoints = centerline.getPoints();
		bufferedGeometry = bufferCenterline(centerlinePoints, width);

		getBehaviours().clear();
		getBehaviours().add(centerline);
	}

	private static org.locationtech.jts.geom.Geometry bufferCenterline(List<SimplePoint> points, double width) {
		GeometryFactory geometryFactory = new GeometryFactory();

		List<Coordinate> coords = new ArrayList<>();
		Coordinate last = null;
		for (SimplePoint p : points) {
			Coordinate c = new Coordinate(p.getX(), p.getY());
			if (last == null || !last.equals2D(c)) {
				coords.add(c);
			}
			last = c;
		}

		if (coords.size() < 2) {
			throw new IllegalArgumentException(
					"trace centerline must contain at least 2 distinct points; got " + coords.size());
		}

		LineString line = geometryFactory.createLineString(coords.toArray(new Coordinate[0]));
		return line.buffer(width / 2.0);
	}

	public List<SimplePoint> getCenterlinePoints() {
		return centerlinePoints;
	}

	public double getWidth() {
		return width;
	}

	public org.locationtech.jts.geom.Geometry getBufferedGeometry() {
		return bufferedGeometry;
	}

	public TraceElement() {
		super();
	}

}
