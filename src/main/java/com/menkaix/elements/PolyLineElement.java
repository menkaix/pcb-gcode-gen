package com.menkaix.elements;

import java.util.ArrayList;
import java.util.List;

import com.menkaix.geometry.basic.PolyLine;
import com.menkaix.geometry.components.SimplePoint;
import com.menkaix.project.Behaviour;
import com.menkaix.project.Geometry;
import com.menkaix.writegcode.LineGcodePath;

public class PolyLineElement extends Element {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3991844004612215411L;

	private transient Geometry geometry;

	public void addPoint(double x, double y) {
		geometry.addPoint(x, y);
	}

	public void removePointAt(int index) {
		try {
			geometry.getPoints().remove(index);
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

		geometry = new PolyLine();
		behaviours.add(geometry);
		behaviours.add(new LineGcodePath(geometry));

	}

	
	

	

}
