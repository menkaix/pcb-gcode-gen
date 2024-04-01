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

//	private SimplePoint center;
//	private double radius;

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
		
		SimplePoint p1 = new SimplePoint(getCenter().getX()-getRadius(), getCenter().getY());
		SimplePoint p2 = new SimplePoint(getCenter().getX()+getRadius(), getCenter().getY());
		
		PointCouple points = new PointCouple(p1, p2);
		
		behaviours.add(new ArcGcodePath(points, RotationDirection.CLOCKWISE, getRadius()));
		behaviours.add(new ArcGcodePath(points, RotationDirection.COUNTER_CLOCKWISE, getRadius()));

	}


	@Override
	public List<Behaviour> getBehaviours() {

		return behaviours;
	}

	public SimplePoint getCenter() {
		return (SimplePoint)getProperty("center") ;
	}

	public void setCenter(SimplePoint center) {
		setProperty("center", center); 
	}

	public double getRadius() {
		return (Double)getProperty("radius") ;
	}

	public void setRadius(double radius) {
		setProperty("radius", radius); 
	}

}
