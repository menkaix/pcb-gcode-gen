package com.menkaix.geometry.components;

import java.io.Serializable;

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
	

	@Override
	public String toString() {
		return "("+x+" ; "+y+" ; "+z+")" ;
	}

}
