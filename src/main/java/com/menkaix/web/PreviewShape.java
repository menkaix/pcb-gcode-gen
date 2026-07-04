package com.menkaix.web;

import java.util.Map;

/**
 * Flat, renderer-friendly view of one element's resolved geometry, used by the
 * frontend to draw an SVG preview without re-implementing any shape math in JS.
 */
public class PreviewShape {

	public int layerIndex = -1;
	public String layerName;
	public int elementIndex = -1;
	public String elementName;
	public String subType;
	public boolean valid;
	public String error;
	public Map<String, Object> shape;

}
