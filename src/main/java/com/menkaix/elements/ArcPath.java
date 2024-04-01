package com.menkaix.elements;

import com.menkaix.geometry.basic.PointCouple;
import com.menkaix.geometry.components.SimplePoint;
import com.menkaix.project.Geometry;
import com.menkaix.project.RotationDirection;
import com.menkaix.writegcode.ArcGcodePath;

public class ArcPath   extends  Element{

	/**
	 * 
	 */
	private static final long serialVersionUID = -6371348113906900012L;
	
	private transient Geometry geometry ;
	
//	private SimplePoint from ;
//	private SimplePoint to ;
//	private double radius ;
	
	public ArcPath(SimplePoint from, SimplePoint to, double radius, RotationDirection direction) {
		
		setProperty("from", from);
		setProperty("to", to);
		setProperty("radius", radius);
		setProperty("direction", direction);
		
		
		geometry = new PointCouple(from, to);
		
		behaviours.add(geometry);
		behaviours.add(new ArcGcodePath(geometry, direction, radius));
		
		
	}

	public SimplePoint getFrom() {
		return (SimplePoint)getProperty("from");
	}

	public void setFrom(SimplePoint from) {
		setProperty("from", from); 
	}

	public SimplePoint getTo() {
		return (SimplePoint)getProperty("to");
	}

	public void setTo(SimplePoint to) {
		setProperty("to", to); 
	}

	public double getRadius() {
		return (Double)getProperty("radius");
	}

	public void setRadius(double radius) {
		setProperty("radius", radius); 
	}

	public Geometry getGeometry() {
		return geometry;
	}

	public void setGeometry(Geometry geometry) {
		this.geometry = geometry;
	}

}
