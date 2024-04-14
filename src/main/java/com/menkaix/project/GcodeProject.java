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
import com.menkaix.project.behaviours.Behaviour;
import com.menkaix.project.behaviours.GcodeBehaviour;
import com.menkaix.project.values.BitHead;
import com.menkaix.writegcode.GcodeFileWriter;

public class GcodeProject implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2312883204979979860L;

	private transient int pass = 0;

	private BitHead bitHead = BitHead.LASER;
	private List<Layer> layers = new ArrayList<Layer>();
	private String projectName = "cool";
	private String projectFolder = "";

	private double safeLevel = 1;
	private double passIncrement = 0;

	private Double feedRate;
	private Double power;

	public void writeGcode() {
	
		GcodeFileWriter gfw = new GcodeFileWriter(Paths.get(projectFolder, projectName + ".nc").toString());
	
		gfw.initializeGcode();
	
		int maxPasses = 0;
	
		for (Layer layer : layers) {
	
			maxPasses = Math.max(maxPasses, layer.getPasses());
	
		}
	
		for (pass = 0; pass < maxPasses; pass++) {
	
			for (Layer layer : layers) {
	
				if(pass>layer.getPasses()) continue ;
				
				//gfw.getGcodes().add("("+layer.getLayerName()+")");
				
				for (Element gcodeObject : layer.getElements()) {
					
					if(gcodeObject==null) {
						System.err.println("err: gcodeObject list is null");
						continue ;
					}
					
					if(gcodeObject.getBehaviours()==null) {
						System.err.println(gcodeObject.getElementName()+": err: behaviour list is null");
						continue ;
					}
	
					//gfw.getGcodes().add("("+layer.getLayerName()+"/"+gcodeObject.getElementName()+")");
					
					for (Behaviour behaviour : gcodeObject.getBehaviours()) {
						if (behaviour instanceof GcodeBehaviour) {
							gfw.getGcodes().add(((GcodeBehaviour) behaviour).getGcode(this));
						}
					}
					
					//gfw.getGcodes().add("(end "+layer.getLayerName()+"/"+gcodeObject.getElementName()+")");
	
				}
				
				//gfw.getGcodes().add("(end "+layer.getLayerName()+")");
	
			}
	
		}
	
		gfw.finalizeGcode();
	
		gfw.write();
	
	}

	public void addLayer(Layer newLayer) throws DuplicateLayerNameException {
		if (layers == null) {
			layers = new ArrayList<Layer>();
		}
	
		for (Layer layer : layers) {
			if (layer.getLayerName().equalsIgnoreCase(newLayer.getLayerName())) {
				throw new DuplicateLayerNameException();
			}
		}
	
		layers.add(newLayer);
	
	}

	public void removeLayer(Layer newLayer) {
		if (layers == null) {
			layers = new ArrayList<Layer>();
		}
	
		for (int i = 0; i < layers.size(); i++) {
			Layer layer = layers.get(i);
			if (layer.getLayerName().equalsIgnoreCase(newLayer.getLayerName())) {
				layers.remove(i);
				break;
			}
		}
	
		layers.add(newLayer);
	
	}

	public void saveJson(String fileName) {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();

		String jsonProject = gson.toJson(this);

		try {
			Files.writeString(Path.of(fileName, projectName + ".json"), jsonProject);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public int getPass() {
		return pass;
	}

	public void setPass(int pass) {
		this.pass = pass;
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

	public double getSafeLevel() {
		return safeLevel;
	}

	public void setSafeLevel(double safeLevel) {
		this.safeLevel = safeLevel;
	}

	public double getPassIncrement() {
		return passIncrement;
	}

	public void setPassIncrement(double passIncrement) {
		this.passIncrement = passIncrement;
	}

	public Double getFeedRate() {
		return feedRate;
	}

	public void setFeedRate(Double feedRate) {
		this.feedRate = feedRate;
	}

	public Double getPower() {
		return power;
	}

	public void setPower(Double power) {
		this.power = power;
	}

	public Layer getLayer(String string) {

		for (Layer layer : layers) {
			if (layer.getLayerName().equalsIgnoreCase(string)) {
				return layer;
			}
		}

		return null;
	}

	public GcodeProject(String name, BitHead head) {
		setProjectName(name);
		setBitHead(head);
		setFeedRate(500d);
		setPower(1000d);
		try {
			addLayer(new Layer("default"));
		} catch (DuplicateLayerNameException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
