package com.menkaix.geometry.components;

import java.io.Serializable;

public class Vector implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1410938710601803100L;
	protected SimplePoint origin;
	protected SimplePoint end;

	protected double x;
	protected double y;
	protected double z;

	public Vector(double x1, double y1, double x2, double y2) {

		this((new SimplePoint(x1, y1)), (new SimplePoint(x2, y2)));

	}

	public Vector(SimplePoint pOrigin, SimplePoint pEnd) {

		origin = pOrigin;
		end = pEnd;

		x = end.getX() - origin.getX();
		y = end.getY() - origin.getY();
		z = end.getZ() - origin.getZ();

	}

	public Vector(double x, double y, double z) {
		setX(x);
		setY(y);
		setZ(z);
		setOrigin(new SimplePoint());
		setEnd(new SimplePoint(x + origin.getX(), y + origin.getY(), z + origin.getZ()));

	}

	public Vector(double x, double y, double z, SimplePoint origin) {
		setX(x);
		setY(y);
		setZ(z);
		setOrigin(origin);
		setEnd(new SimplePoint(x + origin.getX(), y + origin.getY(), z + origin.getZ()));

	}

	public double mod() {
		return Math.sqrt(x * x + y * y + z * z);
	}

	public double getX() {
		return x;
	}

	public double getY() {
		return y;
	}

	public double getZ() {
		return z;
	}

	public SimplePoint getOrigin() {
		return origin;
	}

	public SimplePoint getEnd() {
		return end;
	}

	public void setX(double x) {
		this.x = x;
	}

	public void setY(double y) {
		this.y = y;
	}

	public void setZ(double z) {
		this.z = z;
	}

	public void setOrigin(SimplePoint origin) {
		this.origin = origin;
		end = (new SimplePoint(x + origin.getX(), y + origin.getY(), z + origin.getZ()));
	}

	public void setEnd(SimplePoint end) {
		this.end = end;
		origin = (new SimplePoint(end.getX() - x, end.getY() - y, end.getZ() - z));
	}

	// TODO
	public void stretchEnd(SimplePoint newEnd) {

	}

	// TODO
	public void stretchOrigin(SimplePoint newEnd) {

	}

	public double dot(Vector other) {
		return getX() * other.getX() + getY() * other.getY() + getZ() * other.getZ();
	}

	public Vector cross(Vector other) {

		Vector ans = new Vector(0, 0, 0);

		ans.setX(getY() * other.getZ() - getZ() * other.getY());
		ans.setY(getZ() * other.getX() - getX() * other.getZ());
		ans.setZ(getX() * other.getY() - getY() * other.getX());

		return ans;
	}

	public double angle(Vector other) {

		double t = this.dot(other) / (this.mod() * other.mod());
		if (t >= 1) {
			return 0;
		} else if (t <= -1) {
			return Math.PI;
		} else {
			return Math.acos(t);
		}

	}

	public void scale(double s) {
		x *= s;
		y *= s;
		z *= s;

		end = (new SimplePoint(x + origin.getX(), y + origin.getY(), z + origin.getZ()));

	}

	public void scaleMid(double s) {
		double fx = x * s;
		double fy = y * s;
		double fz = z * s;

		double dx = fx - x;
		double dy = fy - y;
		double dz = fz - z;

		SimplePoint delta = new SimplePoint(dx / 2, dy / 2, dz / 2);

		SimplePoint nO = SimplePoint.substract(origin, delta);
		SimplePoint nE = SimplePoint.add(end, delta);
		
		
		
		origin = nO ;
		end = nE ;
		x=fx ;
		y=fy ;
		z=fz ;
		
		

	}

	public SimplePoint projection(SimplePoint m) {

		Vector other = new Vector(origin, m);

		double cos = Math.cos(this.angle(other));

		double k = cos * other.mod() / this.mod();

		Vector temp = this.clone();
		temp.scale(k);

		return temp.end;
	}

	public void rotate2D(double radian) {

		ComplexNumber c0 = new ComplexNumber(x, y);

		ComplexNumber c1 = c0.multiply(new ComplexNumber(Math.cos(radian), Math.sin(radian)));

		x = c1.getReal();
		y = c1.getImaginary();

		end = (new SimplePoint(x + origin.getX(), y + origin.getY(), z + origin.getZ()));

	}

	public boolean isColinear(Vector other, double delta) {
		return (((getX() * other.getY()) - (getY() * other.getX()) < delta) ? true : false);
	}

	public boolean isColinear(Vector other) {
		return isColinear(other, 0);
	}

	public boolean equals(Vector other) {
		return x == other.x && y == other.y && z == other.z;
	}

	public String toString() {
		return "[" + origin + " ; " + end + "]";
	}

	public boolean includes(SimplePoint p, double epsilon) {

		Vector t = new Vector(origin, p);
		boolean c1 = this.mod() > t.mod();
		boolean c2 = Math.abs(this.angle(t)) <= epsilon;

		return c1 && c2;

	}

	public void Normalize() {
		if (this.mod() != 0) {
			this.scale(1 / this.mod());
		}
	}
	
	public boolean doIntersect(Vector other) {
		// Find the four orientations needed for general and
		// special cases
		int o1 = orientation(this.origin, this.end, other.origin);
		int o2 = orientation(this.origin, this.end, other.end);
		int o3 = orientation(other.origin, other.end, this.origin);
		int o4 = orientation(other.origin, other.end, this.end);

		// General case
		if (o1 != o2 && o3 != o4)
			return true;

		// Special Cases
		// p1, q1 and p2 are colinear and p2 lies on segment p1q1
		if (o1 == 0 && onSegment(this.origin, other.origin, this.end))
			return true;

		// p1, q1 and q2 are colinear and q2 lies on segment p1q1
		if (o2 == 0 && onSegment(this.origin, other.origin, this.end))
			return true;

		// p2, q2 and p1 are colinear and p1 lies on segment p2q2
		if (o3 == 0 && onSegment(other.origin, this.origin, other.end))
			return true;

		// p2, q2 and q1 are colinear and q1 lies on segment p2q2
		if (o4 == 0 && onSegment(other.origin, this.end, other.end))
			return true;

		return false; // Doesn't fall in any of the above cases
	}

	public Vector clone() {

		return new Vector(x, y, z, origin);

	}

	private boolean onSegment(SimplePoint p, SimplePoint q, SimplePoint r) {
		if (q.x <= Math.max(p.x, r.x) && q.x >= Math.min(p.x, r.x) && q.y <= Math.max(p.y, r.y)
				&& q.y >= Math.min(p.y, r.y))
			return true;
	
		return false;
	}

	private int orientation(SimplePoint p, SimplePoint q, SimplePoint r) {
		// See https://www.geeksforgeeks.org/orientation-3-ordered-points/
		// for details of below formula.
		double val = (q.y - p.y) * (r.x - q.x) - (q.x - p.x) * (r.y - q.y);
	
		if (val == 0)
			return 0; // colinear
	
		return (val > 0) ? 1 : 2; // clock or counterclock wise
	}

	// TODO normalize et normalized

}
