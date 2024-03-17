package com.menkaix.elements;

import java.awt.Polygon;
import java.util.ArrayList;
import java.util.List;

import com.menkaix.geometry.basic.PolyGone;
import com.menkaix.project.Behaviour;
import com.menkaix.project.ClosedLineGcodePath;

/**
 * 
 * 
 * rectangle which is drawn clockwise from origin
 * 
 */
public class Rectangle implements Element{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -7163875443583767850L;

	ArrayList<Behaviour> behaviours = new ArrayList<Behaviour>() ;
	
	public String name ;
	
	public Rectangle(String name, double x, double y, double width, double height) {
		
		PolyGone polygonGeometry = new PolyGone();
		polygonGeometry.addPoint(x, y);
		polygonGeometry.addPoint(x+width, y);
		polygonGeometry.addPoint(x+width, y+height);
		polygonGeometry.addPoint(x, y+height);
		
		behaviours.add(polygonGeometry);
		behaviours.add(new ClosedLineGcodePath(polygonGeometry));
	}

	@Override
	public String getElementName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Behaviour> getBehaviours() {
		// TODO Auto-generated method stub
		return behaviours;
	}

}
