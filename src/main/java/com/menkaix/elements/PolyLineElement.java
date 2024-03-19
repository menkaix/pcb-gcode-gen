package com.menkaix.elements;

import java.util.ArrayList;
import java.util.List;

import com.menkaix.geometry.basic.PolyLine;
import com.menkaix.project.Behaviour;
import com.menkaix.project.Geometry;
import com.menkaix.project.LineGcodePath;

public class PolyLineElement  implements Element {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3991844004612215411L;
	private String name ;
	
	private List<Behaviour> behaviours = new ArrayList<Behaviour>();
	
	private Geometry geometry ;
	
	
	
	public PolyLineElement() {
		
		geometry = new PolyLine();
		behaviours.add(geometry);
		behaviours.add(new LineGcodePath(geometry));
		
		
	}
	
	@Override
	public String getElementName() {
		// TODO Auto-generated method stub
		return name;
	}

	@Override
	public void setElementName(String name) {
		
		this.name = name ;
	}

	@Override
	public List<Behaviour> getBehaviours() {
		
		return behaviours;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}
