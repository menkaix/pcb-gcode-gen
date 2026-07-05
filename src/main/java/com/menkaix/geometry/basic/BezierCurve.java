package com.menkaix.geometry.basic;

import java.util.ArrayList;
import java.util.List;

import com.menkaix.geometry.components.SimplePoint;
import com.menkaix.project.behaviours.Geometry;

/**
 * A chain of one or more cubic Bezier segments, defined by a control-point
 * list of the form [anchor, ctrl1, ctrl2, anchor, ctrl1, ctrl2, anchor, ...]
 * (1 + 3*n points for n segments). {@link #getPoints()} exposes the
 * tessellated (flattened) curve so it can be fed to the existing line-based
 * GcodeBehaviour implementations unchanged.
 */
public class BezierCurve implements Geometry {

	public static final int DEFAULT_SEGMENTS_PER_CURVE = 24;

	private final List<SimplePoint> controlPoints;
	private final int segmentsPerCurve;
	private List<SimplePoint> tessellated;

	public BezierCurve(List<SimplePoint> controlPoints) {
		this(controlPoints, DEFAULT_SEGMENTS_PER_CURVE);
	}

	public BezierCurve(List<SimplePoint> controlPoints, int segmentsPerCurve) {
		this.controlPoints = controlPoints;
		this.segmentsPerCurve = Math.max(1, segmentsPerCurve);
		tessellate();
	}

	private void tessellate() {
		tessellated = new ArrayList<>();
		if (controlPoints.isEmpty()) {
			return;
		}

		tessellated.add(controlPoints.get(0));

		for (int i = 1; i + 2 < controlPoints.size(); i += 3) {
			SimplePoint p0 = controlPoints.get(i - 1);
			SimplePoint c1 = controlPoints.get(i);
			SimplePoint c2 = controlPoints.get(i + 1);
			SimplePoint p1 = controlPoints.get(i + 2);

			for (int s = 1; s <= segmentsPerCurve; s++) {
				double t = (double) s / segmentsPerCurve;
				tessellated.add(cubicPoint(p0, c1, c2, p1, t));
			}
		}
	}

	private SimplePoint cubicPoint(SimplePoint p0, SimplePoint c1, SimplePoint c2, SimplePoint p1, double t) {
		double u = 1 - t;
		double x = u * u * u * p0.getX() + 3 * u * u * t * c1.getX() + 3 * u * t * t * c2.getX()
				+ t * t * t * p1.getX();
		double y = u * u * u * p0.getY() + 3 * u * u * t * c1.getY() + 3 * u * t * t * c2.getY()
				+ t * t * t * p1.getY();
		return new SimplePoint(x, y);
	}

	public List<SimplePoint> getControlPoints() {
		return controlPoints;
	}

	@Override
	public List<SimplePoint> getPoints() {
		return tessellated;
	}

	@Override
	public int getPointsCount() {
		return tessellated.size();
	}

	@Override
	public void addPoint(double x, double y) {
		controlPoints.add(new SimplePoint(x, y));
		tessellate();
	}

	@Override
	public void update() {
		tessellate();
	}

}
