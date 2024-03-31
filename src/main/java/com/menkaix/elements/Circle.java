package com.menkaix.elements;

import java.util.ArrayList;
import java.util.List;

import com.menkaix.geometry.basic.PointCouple;
import com.menkaix.geometry.components.SimplePoint;
import com.menkaix.project.Behaviour;
import com.menkaix.project.Geometry;
import com.menkaix.project.RotationDirection;
import com.menkaix.writegcode.ArcGcodePath;
import com.menkaix.writegcode.ClosedLineGcodePath;

public class Circle extends Element {

	private static final long serialVersionUID = -3192960479843484058L;

	private transient ArrayList<Behaviour> behaviours = new ArrayList<Behaviour>();

	private String name = "circle";

	private SimplePoint center;
	private double radius;

	public Circle(SimplePoint center, double radius) {

		setCenter(center);
		setRadius(radius);

		updateGeomtry();
	}

	private void updateGeomtry() {

		for (Behaviour behaviour : behaviours) {
			if (behaviour instanceof Geometry) {
				behaviours.remove(behaviour);
			}
		}
		
		SimplePoint p1 = new SimplePoint(center.getX()-radius, center.getY());
		SimplePoint p2 = new SimplePoint(center.getX()+radius, center.getY());
		
		PointCouple points = new PointCouple(p1, p2);
		
		behaviours.add(new ArcGcodePath(points, RotationDirection.CLOCKWISE, radius));
		behaviours.add(new ArcGcodePath(points, RotationDirection.COUNTER_CLOCKWISE, radius));

	}

	@Override
	public String getElementName() {

		return name;
	}

	@Override
	public void setElementName(String name) {
		this.name = name;
	}

	@Override
	public List<Behaviour> getBehaviours() {

		return behaviours;
	}

	public SimplePoint getCenter() {
		return center;
	}

	public void setCenter(SimplePoint center) {
		this.center = center;
	}

	public double getRadius() {
		return radius;
	}

	public void setRadius(double radius) {
		this.radius = radius;
	}

}
