package com.menkaix.project;

import java.util.List;

public class GcodeProject {
	
	private BitHead bitHead ;
	private List<Layer> layers ;
	
	
	
	public BitHead getBitHead() {
		return bitHead;
	}
	public void setBitHead(BitHead bitHead) {
		this.bitHead = bitHead;
	}
	public List<Layer> getLayers() {
		return layers;
	}
	public void setLayers(List<Layer> layers) {
		this.layers = layers;
	}

}
