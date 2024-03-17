package com.menkaix.project;

import java.io.Serializable;
import java.util.List;

import com.menkaix.geometry.drawable.Element;

public class Layer  implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -2179868110874128997L;

	private int passes ;
	
	private List<Element> elements ;

	public int getPasses() {
		return passes;
	}

	public void setPasses(int passes) {
		this.passes = passes;
	}

	public List<Element> getElements() {
		return elements;
	}

	public void setElements(List<Element> elements) {
		this.elements = elements;
	}
	

}
