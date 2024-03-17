package com.menkaix.geometry.components;

import java.io.Serializable;

public class StraightLine2D implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7791834447510350741L;
	private SimplePoint p1;
	private SimplePoint p2;

	public StraightLine2D(SimplePoint m1, SimplePoint m2) {

		setP1(m1);
		setP2(m2);

	}

	public double a() {

		return (p2.getY() - p1.getY()) / (p2.getX() - p1.getX());

	}

	public double b() {
		return p1.getY() - a() * p1.getX();
	}

	public double yFromX(double x) {
		return a() * x + b();
	}

	public double xFromY(double y) {
		return (y - b()) / a();
	}

	public SimplePoint getP1() {
		return p1;
	}

	public void setP1(SimplePoint p1) {
		this.p1 = p1;
	}

	public SimplePoint getP2() {
		return p2;
	}

	public void setP2(SimplePoint p2) {
		this.p2 = p2;
	}
	
	public static SimplePoint instersection(StraightLine2D l1, StraightLine2D l2) {
		
		if(Double.isNaN(l1.a()) && Double.isNaN(l2.a()) || l1.a()==l2.a()) {
			//NO intersect
			return null ;
		}
		
		if(Double.isNaN(l1.a())) {
			SimplePoint ans = l1.p1 ;
			ans.y = l2.yFromX(ans.x);
			return ans ;
		}
		
		if(Double.isNaN(l2.a())) {
			SimplePoint ans = l2.p1 ;
			ans.y = l1.yFromX(ans.x);
			return ans ;
		}
		
		
		double x = (l2.b()-l1.b())/(l1.a()-l2.a()) ;
		double y = l1.yFromX(x);
		
		
		return new SimplePoint(x, y) ;
	}

}
