package com.menkaix.project;

import java.util.List;

import com.menkaix.geometry.GcodeObject;

public class Layer {
	
	private int passes ;
	
	private List<GcodeObject> elements ;

	public int getPasses() {
		return passes;
	}

	public void setPasses(int passes) {
		this.passes = passes;
	}

	public List<GcodeObject> getElements() {
		return elements;
	}

	public void setElements(List<GcodeObject> elements) {
		this.elements = elements;
	}
	

}
