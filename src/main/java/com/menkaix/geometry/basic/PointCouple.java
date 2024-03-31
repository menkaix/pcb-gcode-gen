package com.menkaix.geometry.basic;

import java.util.ArrayList;
import java.util.List;

import com.menkaix.geometry.components.SimplePoint;
import com.menkaix.project.Geometry;

public class PointCouple implements Geometry {

	private List<SimplePoint> points = new ArrayList<SimplePoint>() ;
	
	public PointCouple(SimplePoint a, SimplePoint b) {
		points.add(a);
		points.add(b);
	}
	
	
	@Override
	public void update() {
				
	}

	@Override
	public List<SimplePoint> getPoints() {
		
		return points;
	}

	@Override
	public int getPointsCount() {
		// TODO Auto-generated method stub
		return points.size();
	}

	@Override
	public void addPoint(double x, double y) {
		
		points.add(new SimplePoint(x,y)) ;
		
	}

}
