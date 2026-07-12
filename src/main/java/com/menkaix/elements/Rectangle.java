package com.menkaix.elements;

import java.util.Map;

import com.menkaix.geometry.basic.PolyGone;
import com.menkaix.geometry.components.SimplePoint;
import com.menkaix.pcbgcode.utilities.MissingPropertyException;
import com.menkaix.project.behaviours.Behaviour;
import com.menkaix.project.behaviours.Geometry;
import com.menkaix.writegcode.ClosedLineGcodePath;

/**
 * 
 * 
 * rectangle which is drawn clockwise from origin
 * 
 */
public class Rectangle extends Element {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7163875443583767850L;

	private transient Geometry geometry;

	// private double x = 0;
	// private double y = 0;
	// private double width = 10;
	// private double height = 10;

	private void updateGeometry() {

		geometry = new PolyGone();

		SimplePoint corner = null;

		try {
			corner = (SimplePoint) getProperty("corner");
		} catch (ClassCastException e) {
			@SuppressWarnings("unchecked")
			Map<String, Double> cornerMap = (Map<String, Double>) getProperty("corner");

			corner = new SimplePoint(cornerMap.get("x"), cornerMap.get("y"));

		}

		Double width = (Double) getProperty("width");
		Double height = (Double) getProperty("height");

		SimplePoint p1 = new SimplePoint(corner.getX(), corner.getY());
		SimplePoint p2 = new SimplePoint(corner.getX(), corner.getY() + height);
		SimplePoint p3 = new SimplePoint(corner.getX() + width, corner.getY() + height);
		SimplePoint p4 = new SimplePoint(corner.getX() + width, corner.getY());

		double rotationDegrees = getRotationDegrees();
		if (rotationDegrees != 0.0) {
			SimplePoint center = new SimplePoint(corner.getX() + width / 2, corner.getY() + height / 2);
			p1 = SimplePoint.rotate(p1, center, rotationDegrees);
			p2 = SimplePoint.rotate(p2, center, rotationDegrees);
			p3 = SimplePoint.rotate(p3, center, rotationDegrees);
			p4 = SimplePoint.rotate(p4, center, rotationDegrees);
		}

		geometry.addPoint(p1.getX(), p1.getY());
		geometry.addPoint(p2.getX(), p2.getY());
		geometry.addPoint(p3.getX(), p3.getY());
		geometry.addPoint(p4.getX(), p4.getY());

	}

	public Rectangle() {

		super();

	}

	public Rectangle(String name, SimplePoint corner, double width, double height) {

		this();

		setElementName(name);

		setProperty("corner", corner);
		setProperty("width", width);
		setProperty("height", height);

		updateGeometry();

		getBehaviours().add(geometry);
		getBehaviours().add(new ClosedLineGcodePath(geometry));
	}

	public Geometry getGeometry() {
		return geometry;
	}

	public void setGeometry(Geometry geometry) {
		this.geometry = geometry;
	}

	@Override
	public void reloadBehaviour() throws MissingPropertyException {

		checkMandatoryProperties("corner", "width", "height");

		updateGeometry();

		getBehaviours().clear();
		getBehaviours().add(geometry);
		getBehaviours().add(new ClosedLineGcodePath(geometry));

	}

}
