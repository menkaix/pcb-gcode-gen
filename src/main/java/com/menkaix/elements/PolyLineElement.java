package com.menkaix.elements;

import java.util.ArrayList;
import java.util.List;

import com.menkaix.geometry.basic.PolyLine;
import com.menkaix.geometry.components.SimplePoint;
import com.menkaix.pcbgcode.utilities.MissingPropertyException;
import com.menkaix.writegcode.LineGcodePath;

import org.slf4j.Logger; // Added
import org.slf4j.LoggerFactory; // Added

//TODO

public class PolyLineElement extends Element {

	private static final Logger LOGGER = LoggerFactory.getLogger(PolyLineElement.class); // Added

	/**
	 *
	 */
	private static final long serialVersionUID = 3991844004612215411L;

	private transient PolyLine geometry;

	private transient List<SimplePoint> points = new ArrayList<SimplePoint>();

	private void updateGeometry() {

		geometry = new PolyLine();
		// geometry.getPoints().clear();
		geometry.getPoints().addAll(points);

		getProperties().put("points", points);

		getBehaviours().clear();

		getBehaviours().add(geometry);
		getBehaviours().add(new LineGcodePath(geometry));

	}

	@Override
	public void reloadBehaviour() throws MissingPropertyException {

		checkMandatoryProperties("points");
		points.clear();

		// points = (List<SimplePoint>) getProperty("points") ;

		@SuppressWarnings("unchecked")
		ArrayList<Object> objs = (ArrayList<Object>) getProperty("points");

		for (Object object : objs) {
			SimplePoint pt = pointFromMap(object);
			points.add(pt);
		}

		updateGeometry();

	}

	public void addPoint(double x, double y) {
		// geometry.addPoint(x, y);
		points.add(new SimplePoint(x, y));
		updateGeometry();
	}

	public void removePointAt(int index) {
		try {
			points.remove(index);
			updateGeometry();
		} catch (IndexOutOfBoundsException e) {
			LOGGER.warn("Attempted to remove point at invalid index: {}", index, e);
		}
	}

	public void addPointAt(double x, double y, int index) {
		try {
			// Note: Adding directly to geometry.points might bypass the intended update
			// logic
			// Consider adding to 'points' list and calling updateGeometry() if consistency
			// is needed
			geometry.getPoints().add(index, new SimplePoint(x, y));
		} catch (IndexOutOfBoundsException e) {
			LOGGER.warn("Attempted to add point at invalid index: {}. Point ({}, {})", index, x, y, e);
		}
	}

	public PolyLineElement() {

		updateGeometry();

	}

}
