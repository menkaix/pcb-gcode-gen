package com.menkaix.elements;

import java.util.ArrayList;
import java.util.List;

import com.menkaix.geometry.basic.PolyGone;
import com.menkaix.project.Behaviour;
import com.menkaix.project.Geometry;
import com.menkaix.writegcode.ClosedLineGcodePath;

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
	
	private String name ;
	
	private double x = 0;
	private double y = 0;
	private double width = 10;
	private double height = 10;
	
	private Geometry geometry ;
	
	private void updateGeometry() {
		
		geometry = new PolyGone();
		geometry.addPoint(x, y);
		geometry.addPoint(x+width, y);
		geometry.addPoint(x+width, y+height);
		geometry.addPoint(x, y+height);
		
	}

	public Rectangle() {
		
	}
	
	public Rectangle(String name, double x, double y, double width, double height) {
		
		setX(x);
		setY(y);
		setWidth(width);
		setHeight(height);
		
		//updateGeometry();
		
		behaviours.add(geometry);
		behaviours.add(new ClosedLineGcodePath(geometry));
	}

	@Override
	public String getElementName() {
		
		return name;
	}

	@Override
	public void setElementName(String name) {
		this.name = name ;
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
		updateGeometry();
	}

	public double getY() {
		return y;
	}

	public void setY(double y) {
		
		this.y = y;
		updateGeometry();
	}

	public double getWidth() {
		return width;
	}

	public void setWidth(double width) {
		this.width = width;
		updateGeometry();
	}

	public double getHeight() {
		return height;
	}

	public void setHeight(double height) {
		this.height = height;
		updateGeometry();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setBehaviours(ArrayList<Behaviour> behaviours) {
		this.behaviours = behaviours;
	}

	public Geometry getGeometry() {
		return geometry;
	}

	public void setGeometry(Geometry geometry) {
		this.geometry = geometry;
	}

}
