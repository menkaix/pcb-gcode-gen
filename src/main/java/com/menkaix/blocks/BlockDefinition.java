package com.menkaix.blocks;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.menkaix.elements.Element;

/**
 * A library block loaded from a JSON file: a named, fixed list of native
 * shape definitions (same {@code {name, subType, properties}} schema as a
 * {@code Layer}'s element list), authored in the block's own local
 * coordinate frame (origin at 0,0). A {@code Block} instance element places
 * this whole group with a single position/rotation via
 * {@link BlockResolver}.
 */
public class BlockDefinition implements Serializable {

	private static final long serialVersionUID = 1L;

	private String blockName;

	private List<Element> elements = new ArrayList<>();

	public String getBlockName() {
		return blockName;
	}

	public void setBlockName(String blockName) {
		this.blockName = blockName;
	}

	public List<Element> getElements() {
		return elements;
	}

	public void setElements(List<Element> elements) {
		this.elements = elements;
	}

}
