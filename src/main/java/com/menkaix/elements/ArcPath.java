package com.menkaix.elements;

import java.util.ArrayList;
import java.util.List;

import com.menkaix.geometry.basic.PointCouple;
import com.menkaix.geometry.components.SimplePoint;
import com.menkaix.project.Behaviour;
import com.menkaix.project.Geometry;
import com.menkaix.project.RotationDirection;
import com.menkaix.writegcode.ArcGcodePath;

public class ArcPath   implements Element{

	/**
	 * 
	 */
	private static final long serialVersionUID = -6371348113906900012L;
	private String name;
	private List<Behaviour> behaviours = new ArrayList<Behaviour>();
	
	private SimplePoint from ;
	private SimplePoint to ;
	private double radius ;
	
	private Geometry geometry ;
	
	public ArcPath(SimplePoint from, SimplePoint to, double radius, RotationDirection direction) {
		
		setGeometry(new PointCouple(from, to));
		
		behaviours.add(geometry);
		behaviours.add(new ArcGcodePath(geometry, direction, radius));
		
		
	}

	@Override
	public String getElementName() {
		
		return this.name;
	}

	@Override
	public void setElementName(String name) {
		this.name = name ;
		
	}

	@Override
	public List<Behaviour> getBehaviours() {
		
		return this.behaviours  ;
	}

	public SimplePoint getFrom() {
		return from;
	}

	public void setFrom(SimplePoint from) {
		this.from = from;
	}

	public SimplePoint getTo() {
		return to;
	}

	public void setTo(SimplePoint to) {
		this.to = to;
	}

	public double getRadius() {
		return radius;
	}

	public void setRadius(double radius) {
		this.radius = radius;
	}

	public Geometry getGeometry() {
		return geometry;
	}

	public void setGeometry(Geometry geometry) {
		this.geometry = geometry;
	}

}
