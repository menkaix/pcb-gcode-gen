package com.menkaix.project;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.menkaix.elements.Element;

public class Layer implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2179868110874128997L;

	private String layerName;

	private int passes;

	private List<Element> elements = new ArrayList<Element>();

	public void addElement(Element elt) {
		elements.add(elt);
	}

	public Layer(String name) {
		setLayerName(name);
	}

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

	public String getLayerName() {
		return layerName;
	}

	public void setLayerName(String layerName) {
		this.layerName = layerName;
	}

}
