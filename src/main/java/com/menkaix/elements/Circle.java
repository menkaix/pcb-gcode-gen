package com.menkaix.elements;

import com.menkaix.geometry.basic.PointCouple;
import com.menkaix.geometry.components.SimplePoint;
import com.menkaix.pcbgcode.utilities.MissingPropertyException;
import com.menkaix.project.RotationDirection;
import com.menkaix.project.behaviours.Behaviour;
import com.menkaix.project.behaviours.Geometry;
import com.menkaix.writegcode.ArcGcodePath;

public class Circle extends Element {

	private static final long serialVersionUID = -3192960479843484058L;

	// private SimplePoint center;
	// private double radius;

	private void updateGeomtry() {

		// System.out.println("update Geometry for "+getElementName());

		for (Behaviour behaviour : getBehaviours()) {
			if (behaviour instanceof Geometry) {
				getBehaviours().remove(behaviour);
			}
		}

		SimplePoint p1 = new SimplePoint(getCenter().getX() - getRadius(), getCenter().getY());
		SimplePoint p2 = new SimplePoint(getCenter().getX() + getRadius(), getCenter().getY());

		PointCouple points = new PointCouple(p1, p2);

		// System.out.println(points);

		ArcGcodePath first = new ArcGcodePath(points, RotationDirection.CLOCKWISE, getRadius());
		ArcGcodePath second = new ArcGcodePath(points, RotationDirection.COUNTER_CLOCKWISE, getRadius());

		getBehaviours().add(first);
		getBehaviours().add(second);

		// System.out.println("preview GCode : \n"+previewGcode());

	}

	@Override
	public void reloadBehaviour() throws MissingPropertyException {

		checkMandatoryProperties("radius", "center");

		SimplePoint center = pointFromMap(getProperty("center"));
		Double radius = (Double) getProperty("radius");

		// System.out.println("center x "+center.getX()+", center y "+center.getY()+"
		// radius "+radius);

		setCenter(center);
		setRadius(radius);

		updateGeomtry();

	}

	public SimplePoint getCenter() {
		return (SimplePoint) getProperty("center");
	}

	public void setCenter(SimplePoint center) {
		setProperty("center", center);
	}

	public double getRadius() {
		return (Double) getProperty("radius");
	}

	public void setRadius(double radius) {
		setProperty("radius", radius);
	}

	// private SimplePoint center;
	// private double radius;

	public Circle() {
		super();
	}

	// private SimplePoint center;
	// private double radius;

	public Circle(SimplePoint center, double radius) {

		this();

		setCenter(center);
		setRadius(radius);

		updateGeomtry();
	}

}
