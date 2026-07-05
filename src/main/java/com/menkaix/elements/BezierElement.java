package com.menkaix.elements;

import java.util.ArrayList;
import java.util.List;

import com.menkaix.geometry.basic.BezierCurve;
import com.menkaix.geometry.components.SimplePoint;
import com.menkaix.pcbgcode.utilities.MissingPropertyException;
import com.menkaix.writegcode.LineGcodePath;

/**
 * An open path made of one or more chained cubic Bezier segments. Properties
 * hold the raw control points as [anchor, ctrl1, ctrl2, anchor, ctrl1, ctrl2,
 * anchor, ...]; the curve is tessellated into a polyline (see
 * {@link BezierCurve}) and reuses {@link LineGcodePath} for G-code, the same
 * way {@link PolyLineElement} does for straight segments.
 */
public class BezierElement extends Element {

	private static final long serialVersionUID = 8321947710448839213L;

	private transient BezierCurve geometry;

	private transient List<SimplePoint> points = new ArrayList<>();

	private void updateGeometry() {
		geometry = new BezierCurve(points);

		getProperties().put("points", points);

		getBehaviours().clear();
		getBehaviours().add(geometry);
		getBehaviours().add(new LineGcodePath(geometry));
	}

	@Override
	public void reloadBehaviour() throws MissingPropertyException {
		checkMandatoryProperties("points");
		points.clear();

		@SuppressWarnings("unchecked")
		ArrayList<Object> objs = (ArrayList<Object>) getProperty("points");

		for (Object object : objs) {
			points.add(pointFromMap(object));
		}

		if (points.size() < 4 || (points.size() - 1) % 3 != 0) {
			throw new MissingPropertyException(
					"points must contain a start anchor followed by groups of 3 (control, control, anchor); got "
							+ points.size());
		}

		updateGeometry();
	}

	public BezierCurve getGeometry() {
		return geometry;
	}

	public BezierElement() {
		updateGeometry();
	}

}
