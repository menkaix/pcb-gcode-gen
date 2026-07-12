package com.menkaix.geometry.components;

import java.io.Serializable;
import java.util.List;

public class SimplePoint implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = -8529482457454328700L;
	protected Double x;
	protected Double y;
	protected Double z;

	public SimplePoint() {

	}

	public SimplePoint(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public SimplePoint(double x, double y) {
		this.x = x;
		this.y = y;
		this.z = Double.valueOf(0);
	}


	public Double getX() {
		return x;
	}

	public void setX(Double x) {
		this.x = x;
	}

	public Double getY() {
		return y;
	}

	public void setY(Double y) {
		this.y = y;
	}

	public Double getZ() {
		return z;
	}

	public void setZ(Double z) {
		this.z = z;
	}

	
	

	public static double distance(SimplePoint p1, SimplePoint p2) {
		return Math.sqrt((p2.x - p1.x) * (p2.x - p1.x) + (p2.y - p1.y) * (p2.y - p1.y) + (p2.z - p1.z) * (p2.z - p1.z));

	}
	
	public static SimplePoint middle(SimplePoint p1, SimplePoint p2) {

		return new SimplePoint((p2.x + p1.x) / 2, (p2.y + p1.y) / 2, (p2.z + p1.z) / 2);

	}

	public static SimplePoint add(SimplePoint a, SimplePoint b) {

		SimplePoint ans = new SimplePoint();

		ans.x = a.x + b.x;
		ans.y = a.y + b.y;
		ans.z = a.z + b.z;

		return ans;

	}

	public static SimplePoint scale(SimplePoint a, double b) {

		SimplePoint ans = new SimplePoint();

		ans.x = a.x * b;
		ans.y = a.y * b;
		ans.z = a.z * b;

		return ans;

	}

	public static SimplePoint substract(SimplePoint a, SimplePoint b) {

		SimplePoint ans = new SimplePoint();

		ans.x = a.x - b.x;
		ans.y = a.y - b.y;
		ans.z = a.z - b.z;

		return ans;

	}
	
	public boolean equals(SimplePoint other, double epsilon) {
		return distance(this, other)<=epsilon;
	}

	public static SimplePoint centroid(List<SimplePoint> points) {
		double sx = 0, sy = 0, sz = 0;
		for (SimplePoint p : points) {
			sx += p.x;
			sy += p.y;
			sz += p.z;
		}
		int n = points.size();
		return new SimplePoint(sx / n, sy / n, sz / n);
	}

	/**
	 * Rotates a point around a pivot by an angle in degrees, positive being
	 * clockwise as displayed on screen (this project's mm coordinates are
	 * Y-up; the UI flips Y back to Y-down for on-screen rendering, which is
	 * why clockwise-on-screen is {@code cos, +sin / -sin, cos} here rather
	 * than the textbook Y-up counter-clockwise-positive form).
	 */
	public static SimplePoint rotate(SimplePoint p, SimplePoint pivot, double angleDegreesClockwise) {
		double rad = Math.toRadians(angleDegreesClockwise);
		double cos = Math.cos(rad);
		double sin = Math.sin(rad);
		double dx = p.x - pivot.x;
		double dy = p.y - pivot.y;
		double nx = pivot.x + dx * cos + dy * sin;
		double ny = pivot.y - dx * sin + dy * cos;
		return new SimplePoint(nx, ny, p.z);
	}


	@Override
	public String toString() {
		return "("+x+" ; "+y+" ; "+z+")" ;
	}

}
