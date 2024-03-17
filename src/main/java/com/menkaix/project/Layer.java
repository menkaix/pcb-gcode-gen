package com.menkaix.project;

import java.util.List;

public class Layer {
	
	private int passes ;
	
	private List<GcodeBehaviour> elements ;

	public int getPasses() {
		return passes;
	}

	public void setPasses(int passes) {
		this.passes = passes;
	}

	public List<GcodeBehaviour> getElements() {
		return elements;
	}

	public void setElements(List<GcodeBehaviour> elements) {
		this.elements = elements;
	}
	

}
