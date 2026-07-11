package com.menkaix.elements;

import com.menkaix.geometry.components.SimplePoint;
import com.menkaix.pcbgcode.utilities.MissingPropertyException;
import com.menkaix.writegcode.HoleGcodePath;

/**
 * A single drilled hole, positioned only by its X/Y coordinate. Unlike every
 * other element, it carries no Z of its own: the plunge depth is
 * {@code Layer.holeDepth}, a per-layer setting (see {@link
 * com.menkaix.project.Layer}), so every hole on the same layer/material
 * thickness shares it.
 */
public class HoleElement extends Element {

	private static final long serialVersionUID = 5075514908001220587L;

	private void updateGeometry() {
		getBehaviours().clear();
		getBehaviours().add(new HoleGcodePath(getPosition()));
	}

	@Override
	public void reloadBehaviour() throws MissingPropertyException {
		checkMandatoryProperties("position");

		SimplePoint position = pointFromMap(getProperty("position"));
		setPosition(position);

		updateGeometry();
	}

	public SimplePoint getPosition() {
		return (SimplePoint) getProperty("position");
	}

	public void setPosition(SimplePoint position) {
		setProperty("position", position);
	}

	public HoleElement() {
		super();
	}

}
