package com.menkaix.geometry.drawable;

import java.awt.Color;
import java.awt.Graphics;

import com.menkaix.geometry.basics.SimplePoint;
import com.menkaix.geometry.basics.Vector;

public class DrawVector extends Vector implements Drawable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5606262124600938218L;
	public static final int RADIUS = 4;

	private Color color = Color.WHITE;

	public DrawVector(double x1, double y1, double x2, double y2) {
		super(x1, y1, x2, y2);
		// TODO Auto-generated constructor stub
	}

	public DrawVector(double x, double y, double z) {
		super(x, y, z);
		// TODO Auto-generated constructor stub
	}
	
	public DrawVector(SimplePoint pOrigin, SimplePoint pEnd) {
		super(pOrigin, pEnd) ;
	}

	public void draw(Graphics graphics) {

		graphics.setColor(color);
		
		graphics.drawLine((int) origin.getX(), (int) origin.getY(), (int) end.getX(), (int) end.getY());

		// X ey Y pour le coin supérieur gauche
		graphics.drawOval((int) origin.getX() - RADIUS / 2, (int) origin.getY() - RADIUS / 2, RADIUS, RADIUS);

		Vector fleche1 = (Vector) (this.clone());
		Vector fleche2 = (Vector) (this.clone());

		fleche1.scale(2 * RADIUS / fleche1.mod());
		fleche2.scale(2 * RADIUS / fleche2.mod());

		fleche1.rotate2D(Math.PI * 0.8);
		fleche2.rotate2D(-Math.PI * 0.8);

		fleche1.setOrigin(end);
		fleche2.setOrigin(end);

		graphics.drawLine((int) fleche1.getOrigin().getX(), (int) fleche1.getOrigin().getY(),
				(int) fleche1.getEnd().getX(), (int) fleche1.getEnd().getY());
		graphics.drawLine((int) fleche2.getOrigin().getX(), (int) fleche2.getOrigin().getY(),
				(int) fleche2.getEnd().getX(), (int) fleche2.getEnd().getY());

	}

	public void setColor(Color color) {
		
		this.color = color;
		
	}

	public void update() {
		// TODO Auto-generated method stub
		
	}

}
