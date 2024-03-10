package com.menkaix.geometry.drawable;

import java.awt.Color;
import java.awt.Graphics;

import com.menkaix.geometry.basics.SimplePoint;

public class DrawPoint extends SimplePoint implements Drawable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7555927266845198195L;

	public static final int SIZE = 2;
	
	private Color color = Color.WHITE ;
	
	public DrawPoint() {
		// TODO Auto-generated constructor stub
	}

	public DrawPoint(double x, double y, double z) {
		super(x, y, z);
		// TODO Auto-generated constructor stub
	}

	public DrawPoint(double x, double y) {
		super(x, y);
		// TODO Auto-generated constructor stub
	}
	
	public DrawPoint(SimplePoint o) {
		this(o.getX(), o.getY(), o.getZ()) ;
	}

	public void draw(Graphics graphics) {
		
		graphics.setColor(color);

		graphics.drawLine((int) (x - SIZE), (int) (y - SIZE), (int) (x + SIZE), (int) (y + SIZE));
		graphics.drawLine((int) (x - SIZE), (int) (y + SIZE), (int) (x + SIZE), (int) (y - SIZE));

	}

	public void setColor(Color color) {
		
		this.color = color ;
		
	}

	public void update() {
		// TODO Auto-generated method stub
		
	}

}
