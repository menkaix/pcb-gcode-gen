package com.menkaix.project;

import java.io.Serializable;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.menkaix.geometry.drawable.Element;
import com.menkaix.writegcode.GcodeFileWriter;

public class GcodeProject implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2312883204979979860L;
	private BitHead bitHead = BitHead.LASER;
	private List<Layer> layers = new ArrayList<Layer>();
	private String projectName = "cool.nc";
	private String projectFolder = "";

	public void writeGcode() {

		GcodeFileWriter gfw = new GcodeFileWriter(Paths.get(projectFolder, projectName).toString());

		gfw.initializeGcode();

		for (Layer layer : layers) {
			for (Element gcodeObject : layer.getElements()) {
				
				for(Behaviour behaviour : gcodeObject.getBehaviours()) {
					if(behaviour instanceof GcodeBehaviour) {
						gfw.getGcodes().add(((GcodeBehaviour)behaviour).getGcode());
					}
				}

				

			}
		}

		gfw.finalizeGcode();

		gfw.write();

	}

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

	public String getProjectName() {
		return projectName;
	}

	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	public String getProjectFolder() {
		return projectFolder;
	}

	public void setProjectFolder(String projectFolder) {
		this.projectFolder = projectFolder;
	}

}
