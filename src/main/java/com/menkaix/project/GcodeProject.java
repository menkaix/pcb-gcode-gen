package com.menkaix.project;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.menkaix.elements.Element;
import com.menkaix.pcbgcode.utilities.DuplicateLayerNameException;
import com.menkaix.writegcode.GcodeFileWriter;

public class GcodeProject implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2312883204979979860L;
	private BitHead bitHead = BitHead.LASER;
	private List<Layer> layers = new ArrayList<Layer>();
	private String projectName = "cool";
	private String projectFolder = "";
	
	public void saveJson(String fileName) {
		Gson gson = new GsonBuilder().setPrettyPrinting().create() ;
		
		String jsonProject = gson.toJson(this);
		
		try {
			Files.writeString(Path.of(fileName,projectName+".json"), jsonProject);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void writeGcode() {

		GcodeFileWriter gfw = new GcodeFileWriter(Paths.get(projectFolder, projectName+".nc").toString());

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

	
	public void addLayer(Layer newLayer) throws DuplicateLayerNameException {
		if(layers == null) {
			layers = new ArrayList<Layer>() ;
		}
		
		for (Layer layer : layers) {
			if(layer.getLayerName().equalsIgnoreCase(newLayer.getLayerName())) {
				throw new DuplicateLayerNameException();
			}
		}
		
		layers.add(newLayer) ;
		
	}
	
	public void removeLayer(Layer newLayer) {
		if(layers == null) {
			layers = new ArrayList<Layer>() ;
		}
		
		for (int i=0 ; i<layers.size() ; i++) {
			Layer layer = layers.get(i);
			if(layer.getLayerName().equalsIgnoreCase(newLayer.getLayerName())) {
				layers.remove(i);
				break ;
			}
		}
		
		layers.add(newLayer) ;
		
	}
	
	public GcodeProject(String name, BitHead head) {
		setProjectName(name);
		setBitHead(head);
		try {
			addLayer(new Layer("default"));
		} catch (DuplicateLayerNameException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
