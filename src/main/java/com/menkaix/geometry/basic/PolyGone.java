package com.menkaix.geometry.basic;

import java.util.ArrayList;
import java.util.List;

import com.menkaix.geometry.components.SimplePoint;
import com.menkaix.project.Geometry;

public class PolyGone implements Geometry {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7648952201152086454L;

	public final int SELECT_DIST = 20;

	private List<SimplePoint> points;

	public void addPoint(double x, double y) {
		synchronized (points) {

			points.add(new SimplePoint(x, y));

		}
	}

	public int getPointsCount() {
		int ans;
		synchronized (points) {
			ans = points.size();
		}

		return ans;
	}

	public void update() {

	}

	@Override
	public List<SimplePoint> getPoints() {
		// TODO Auto-generated method stub
		return points;
	}

	public PolyGone() {
		points = new ArrayList<SimplePoint>();

	}

}
