package com.menkaix.elements;

import java.util.ArrayList;
import java.util.List;

import com.menkaix.geometry.basic.PolyLine;
import com.menkaix.geometry.components.SimplePoint;
import com.menkaix.pcbgcode.utilities.MissingPropertyException;
import com.menkaix.project.behaviours.Behaviour;
import com.menkaix.project.behaviours.Geometry;
import com.menkaix.writegcode.LineGcodePath;

//TODO

public class PolyLineElement extends Element {

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
		
		//points = (List<SimplePoint>) getProperty("points") ;
		
		ArrayList<Object> objs = (ArrayList<Object>) getProperty("points") ;
		
		for (Object object : objs) {
			SimplePoint pt = pointFromMap(object) ;
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
			e.printStackTrace();
		}
	}

	public void addPointAt(double x, double y, int index) {
		try {
			geometry.getPoints().add(index, new SimplePoint(x, y));
		} catch (IndexOutOfBoundsException e) {
			e.printStackTrace();
		}
	}

	public PolyLineElement() {

		updateGeometry();

		

	}

}
