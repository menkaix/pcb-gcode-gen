package com.menkaix.elements;

import java.awt.Polygon;
import java.util.ArrayList;
import java.util.List;

import com.menkaix.geometry.basic.PolyGone;
import com.menkaix.project.Behaviour;
import com.menkaix.project.ClosedLineGcodePath;

/**
 * 
 * 
 * rectangle which is drawn clockwise from origin
 * 
 */
public class Rectangle implements Element{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -7163875443583767850L;

	ArrayList<Behaviour> behaviours = new ArrayList<Behaviour>() ;
	
	public String elementName ;
	public void setBehaviours(ArrayList<Behaviour> behaviours) {
		this.behaviours = behaviours;
	}

	private double x = 0;
	private double y = 0;
	private double width = 10;
	private double height = 10;
	
	public Rectangle() {
		
	}
	
	public Rectangle(String name, double x, double y, double width, double height) {
		
		setX(x);
		setY(y);
		setWidth(width);
		setHeight(height);
		
		PolyGone polygonGeometry = new PolyGone();
		polygonGeometry.addPoint(x, y);
		polygonGeometry.addPoint(x+width, y);
		polygonGeometry.addPoint(x+width, y+height);
		polygonGeometry.addPoint(x, y+height);
		
		behaviours.add(polygonGeometry);
		behaviours.add(new ClosedLineGcodePath(polygonGeometry));
	}

	@Override
	public String getElementName() {
		
		return elementName;
	}

	@Override
	public void setElementName(String name) {
		elementName = name ;
	}

	@Override
	public List<Behaviour> getBehaviours() {
		
		return behaviours;
	}

	public double getX() {
		return x;
	}

	public void setX(double x) {
		this.x = x;
	}

	public double getY() {
		return y;
	}

	public void setY(double y) {
		this.y = y;
	}

	public double getWidth() {
		return width;
	}

	public void setWidth(double width) {
		this.width = width;
	}

	public double getHeight() {
		return height;
	}

	public void setHeight(double height) {
		this.height = height;
	}

}
