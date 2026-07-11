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

	private boolean tabsEnabled = false;

	private int tabCount = 4;

	private double tabWidth = 2.0;

	private boolean excludeFromGcode = false;

	/**
	 * Z depth (mm) that every {@code HoleElement} on this layer plunges to; a
	 * hole only carries its X/Y drilling position, the plunge depth is a
	 * per-layer setting so every hole drilled on the same layer/material
	 * thickness shares it. Negative, following this project's convention that
	 * Z decreases going into the material (see {@code passIncrement}).
	 */
	private double holeDepth = -1.6;

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

	public boolean isTabsEnabled() {
		return tabsEnabled;
	}

	public void setTabsEnabled(boolean tabsEnabled) {
		this.tabsEnabled = tabsEnabled;
	}

	public int getTabCount() {
		return tabCount;
	}

	public void setTabCount(int tabCount) {
		this.tabCount = tabCount;
	}

	public double getTabWidth() {
		return tabWidth;
	}

	public void setTabWidth(double tabWidth) {
		this.tabWidth = tabWidth;
	}

	public boolean isExcludeFromGcode() {
		return excludeFromGcode;
	}

	public void setExcludeFromGcode(boolean excludeFromGcode) {
		this.excludeFromGcode = excludeFromGcode;
	}

	public double getHoleDepth() {
		return holeDepth;
	}

	public void setHoleDepth(double holeDepth) {
		this.holeDepth = holeDepth;
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
