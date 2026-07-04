package com.menkaix.elements;

import com.menkaix.geometry.components.SimplePoint;
import com.menkaix.pcbgcode.utilities.MissingPropertyException;
import com.menkaix.project.RotationDirection;
import com.menkaix.writegcode.CircleGcodePath;

public class Circle extends Element {

	private static final long serialVersionUID = -3192960479843484058L;

	// private SimplePoint center;
	// private double radius;

	private void updateGeometry() {
		getBehaviours().clear();
		getBehaviours().add(new CircleGcodePath(getCenter(), getRadius(), RotationDirection.CLOCKWISE));
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

		updateGeometry();

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

		updateGeometry();
	}

}
