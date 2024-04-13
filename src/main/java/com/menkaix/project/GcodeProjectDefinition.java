package com.menkaix.project;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.menkaix.elements.Element;
import com.menkaix.elements.factory.ElementFactory;
import com.menkaix.pcbgcode.utilities.DuplicateLayerNameException;
import com.menkaix.pcbgcode.utilities.MissingPropertyException;
import com.menkaix.pcbgcode.utilities.UnknownElementException;

public class GcodeProjectDefinition implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2312883204979979860L;

	private transient int pass = 0;

	private BitHead bitHead = BitHead.LASER; //
	private List<Layer> layers = new ArrayList<Layer>(); //
	private String projectName = "cool"; //
	private String projectFolder = ""; //

	private double safeLevel = 1; //
	private double passIncrement = 0; //

	private Double feedRate; //
	private Double power; //

	private class FactoryElementsRunnable implements Runnable {

		List<Element> elementList;
		Element baseElement;

		public FactoryElementsRunnable(List<Element> list, Element base) {
			elementList = list;
			baseElement = base;
		}

		@Override
		public void run() {

			Element newElement;
			try {
				newElement = ElementFactory.create(baseElement);

				synchronized (elementList) {
					elementList.add(newElement);
				}

			} catch (MissingPropertyException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (UnknownElementException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

	}

	public GcodeProject generate() {

		ExecutorService executorService = Executors.newFixedThreadPool(30);

		GcodeProject ans = new GcodeProject(projectName, bitHead);
		ans.setProjectFolder(projectFolder);
		ans.setSafeLevel(safeLevel);
		ans.setPassIncrement(passIncrement);
		ans.setFeedRate(feedRate);
		ans.setPower(power);

		ans.getLayers().clear();

		for (Layer layer : layers) {

			Layer newLayer = new Layer(layer.getLayerName());
			newLayer.setPasses(layer.getPasses());

			for (Element elt : layer.getElements()) {
				// newLayer.addElement(ElementFactory.create(elt));
				executorService.execute(new FactoryElementsRunnable(newLayer.getElements(), elt));
			}

			try {
				if (!executorService.awaitTermination(1, TimeUnit.SECONDS)) {

				}
			} catch (InterruptedException e) {

				e.printStackTrace();
			}

			try {
				ans.addLayer(newLayer);
			} catch (DuplicateLayerNameException e) {
				System.out.println("err new Layer: " + newLayer.getLayerName());
				for (Layer layer2 : ans.getLayers()) {
					System.out.println(layer2.getLayerName());
				}
				e.printStackTrace();
			}
		}

		return ans;
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

}
