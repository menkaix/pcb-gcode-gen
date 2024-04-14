package com.menkaix.project.behaviours;

import java.util.List;

import com.menkaix.geometry.components.SimplePoint;

public interface Geometry extends Behaviour {
	
	public List<SimplePoint> getPoints();
	public int getPointsCount();
	public void addPoint(double x, double y);

}
