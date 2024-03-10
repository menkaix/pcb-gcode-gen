package com.menkaix.geometry.drawable;

import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.List;

import com.menkaix.geometry.basics.SimplePoint;

public class SimpleLine implements Drawable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 7648952201152086454L;
	
	
	List<SimplePoint> points ;
	
	List<SimplePoint> toDraw ;
	
	public void addPoint(double x, double y) {
		synchronized (points) {
			points.add(new SimplePoint(x,y));
		}
	}
	
	public int getPointsCount() {
		int ans ;
		synchronized (points) {
			ans = points.size();
		}
		
		return ans ;
	}
	
	public SimpleLine() {
		points = new ArrayList<SimplePoint>() ;
		toDraw = new ArrayList<SimplePoint>() ;
		
	}

	public void update() {
		
		synchronized (toDraw) {
			toDraw.clear();
			// TODO add interpolation here
			toDraw.addAll(points);
		}
		
		
	}

	public void draw(Graphics graphics) {
//		System.out.println("draw line " + toDraw.size());
		synchronized (toDraw) {
			
			for(int i = 0 ; i<toDraw.size()-2 ; i++) {
				graphics.drawLine((int)toDraw.get(i).getX(), (int)toDraw.get(i).getY(), (int)toDraw.get(i+1).getX(), (int)toDraw.get(i+1).getY());
			}
			
		}

	}

	public void setColor(Color color) {
		// TODO Auto-generated method stub

	}

}
