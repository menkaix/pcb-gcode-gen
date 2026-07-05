package com.menkaix.web;

/**
 * Minimal request body for creating/renaming a layer. Kept separate from the
 * domain {@link com.menkaix.project.Layer} class, which has no no-arg
 * constructor: Gson would fall back to unsafe field allocation and leave its
 * "elements" list null when a request body omits that key.
 */
public class LayerRequest {

	public String layerName;
	public int passes;
	public boolean tabsEnabled;
	public int tabCount;
	public double tabWidth;
	public boolean excludeFromGcode;

}
