package com.menkaix.project;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors; // Added for stream operations in logging

import org.slf4j.Logger; // Added
import org.slf4j.LoggerFactory; // Added

import com.menkaix.elements.Element;
import com.menkaix.elements.factory.ElementFactory;
import com.menkaix.pcbgcode.utilities.DuplicateLayerNameException;
import com.menkaix.pcbgcode.utilities.MissingPropertyException;
import com.menkaix.pcbgcode.utilities.UnknownElementException;
import com.menkaix.project.values.BitHead;

public class GcodeProjectDefinition implements Serializable {

	private static final Logger LOGGER = LoggerFactory.getLogger(GcodeProjectDefinition.class); // Added logger

	/**
	 *
	 */
	private static final long serialVersionUID = 2312883204979979860L;

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
				LOGGER.error("Missing property while creating element from base: {}", baseElement, e);
			} catch (UnknownElementException e) {
				LOGGER.error("Unknown element type encountered for base: {}", baseElement, e);
			}

		}

	}

	public GcodeProject generate() {
		LOGGER.info("Generating GcodeProject '{}'...", projectName);
		ExecutorService executorService = Executors.newCachedThreadPool();

		GcodeProject ans = new GcodeProject(projectName, bitHead);
		ans.setProjectFolder(projectFolder);
		ans.setSafeLevel(safeLevel);
		ans.setPassIncrement(passIncrement);
		ans.setFeedRate(feedRate);
		ans.setPower(power);

		ans.getLayers().clear();
		LOGGER.debug("Cleared existing layers for project generation.");

		for (Layer layer : layers) {
			LOGGER.debug("Processing layer: {}", layer.getLayerName());
			Layer newLayer = new Layer(layer.getLayerName());
			newLayer.setPasses(layer.getPasses());

			for (Element elt : layer.getElements()) {
				LOGGER.trace("Submitting element for creation: {}", elt);
				executorService.execute(new FactoryElementsRunnable(newLayer.getElements(), elt));
			}

			// Shutdown the executor service and wait for tasks to complete for this layer
			executorService.shutdown();
			try {
				LOGGER.debug("Waiting for element creation tasks to complete for layer: {}", layer.getLayerName());
				if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) { // Increased timeout
					LOGGER.warn(
							"Element creation thread pool did not terminate in time for layer: {}. Forcing shutdown.",
							layer.getLayerName());
					executorService.shutdownNow();
				} else {
					LOGGER.debug("Element creation tasks completed for layer: {}", layer.getLayerName());
				}
			} catch (InterruptedException e) {
				LOGGER.warn("Element creation thread pool termination interrupted for layer: {}", layer.getLayerName(),
						e);
				executorService.shutdownNow(); // Force shutdown on interrupt
				// Restore the interrupted status
				Thread.currentThread().interrupt();
			}
			// Re-initialize executor for the next layer if needed (or move outside loop if
			// shared)
			// For now, assuming a new pool per layer might be intended, but let's re-init
			executorService = Executors.newCachedThreadPool(); // Re-initialize for next layer

			try {
				ans.addLayer(newLayer);
				LOGGER.debug("Added layer '{}' to the project.", newLayer.getLayerName());
			} catch (DuplicateLayerNameException e) {
				// Log the error and existing layer names for debugging
				String existingLayers = ans.getLayers().stream()
						.map(Layer::getLayerName)
						.collect(Collectors.joining(", "));
				LOGGER.error("Attempted to add duplicate layer name: {}. Existing layers: [{}]. Skipping this layer.",
						newLayer.getLayerName(), existingLayers, e);
			}
		}
		// Final shutdown for the last executor instance
		executorService.shutdown();
		try {
			if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
				executorService.shutdownNow();
			}
		} catch (InterruptedException e) {
			executorService.shutdownNow();
			Thread.currentThread().interrupt();
		}

		LOGGER.info("GcodeProject '{}' generation complete.", projectName);
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
		LOGGER.trace("Searching for layer with name: {}", string);
		for (Layer layer : layers) {
			if (layer.getLayerName().equalsIgnoreCase(string)) {
				LOGGER.trace("Found layer: {}", string);
				return layer;
			}
		}
		LOGGER.trace("Layer not found: {}", string);
		return null;
	}

}
