package com.menkaix.geometry.basic;

import java.util.ArrayList;
import java.util.List;

import com.menkaix.geometry.components.SimplePoint;
import com.menkaix.project.behaviours.Geometry;

public class SimpleLine implements Geometry {

	private List<SimplePoint> points;

	@Override
	public List<SimplePoint> getPoints() {
		return points;
	}

	public void addPoint(double x, double y) {
		synchronized (points) {
			points.add(new SimplePoint(x, y));
		}
	}

	@Override
	public int getPointsCount() {
		int ans;
		synchronized (points) {
			ans = points.size();
		}

		return ans;
	}

	@Override
	public void update() {

	}

	public SimpleLine() {
		points = new ArrayList<SimplePoint>();
	}

}
