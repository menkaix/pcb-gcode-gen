package com.menkaix.geometry.drawable;

import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.List;

import com.menkaix.geometry.basics.SimplePoint;
import com.menkaix.project.Behaviour;

public class PolyLine implements Element {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7648952201152086454L;

	public final int SELECT_DIST = 20;

	List<SimplePoint> points;
	DrawPoint handler;

	public void addPoint(double x, double y) {
		synchronized (points) {
			points.add(new SimplePoint(x, y));
		}
	}

	public void dragpoint(int x, int y) {

		SimplePoint pick = pointCorner(x, y);

		if (pick != null) {
			pick.setX(x);
			pick.setY(y);
		}
	}

	public SimplePoint pointCorner(int x, int y) {

		for (SimplePoint p : points) {
			if (SimplePoint.distance(p, new SimplePoint(x, y)) < SELECT_DIST) {

				handler = new DrawPoint(p);

				return p;
			}
		}

		handler = null;
		return null;
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

	public void draw(Graphics graphics) {
//		System.out.println("draw line " + toDraw.size());
		synchronized (points) {

			for (int i = 0; i < points.size() - 1; i++) {
				graphics.drawLine((int) points.get(i).getX(), (int) points.get(i).getY(),
						(int) points.get(i + 1).getX(), (int) points.get(i + 1).getY());
			}

		}

		if (handler != null) {
			handler.draw(graphics);
		}

	}

	public void setColor(Color color) {
		// TODO Auto-generated method stub

	}

	public PolyLine() {
		points = new ArrayList<SimplePoint>();

	}

	private ArrayList<Behaviour> behaviours = new ArrayList<Behaviour>();

	@Override
	public List<Behaviour> getBehaviours() {
		// TODO Auto-generated method stub
		return behaviours;
	}

}
