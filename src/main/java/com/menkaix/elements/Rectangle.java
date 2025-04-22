package com.menkaix.elements;

import java.util.ArrayList;
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

		geometry.addPoint(corner.getX(), corner.getY());
		geometry.addPoint(corner.getX(), corner.getY() + height);
		geometry.addPoint(corner.getX() + width, corner.getY() + height);
		geometry.addPoint(corner.getX() + width, corner.getY());

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

	public void setBehaviours(ArrayList<Behaviour> behaviours) {
		setBehaviours(behaviours);
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
