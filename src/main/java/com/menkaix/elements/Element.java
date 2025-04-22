package com.menkaix.elements;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import com.menkaix.geometry.components.SimplePoint;
import com.menkaix.pcbgcode.utilities.MissingPropertyException;
import com.menkaix.pcbgcode.utilities.UnknownElementException;
import com.menkaix.project.GcodeProject;
import com.menkaix.project.behaviours.Behaviour;
import com.menkaix.project.behaviours.GcodeBehaviour;
import com.menkaix.project.values.BitHead;

import org.slf4j.Logger; // Added
import org.slf4j.LoggerFactory; // Added

public class Element implements Serializable {

	private static final Logger LOGGER = LoggerFactory.getLogger(Element.class); // Added

	private static final long serialVersionUID = 194916028736735923L;

	private String name;
	private String subType;

	private WeakHashMap<String, Object> properties = new WeakHashMap<String, Object>();

	private transient List<Behaviour> behaviours = new ArrayList<Behaviour>();

	protected void checkMandatoryProperties(String... propertyNames) throws MissingPropertyException {

		ArrayList<String> missing = new ArrayList<String>();

		for (String string : propertyNames) {

			if (!properties.containsKey(string))
				missing.add(string);

		}

		if (missing.size() > 0) {
			String message = "missing property ";
			for (String string : missing) {
				message += " -" + string;
			}
			throw new MissingPropertyException(message);
		}

	}

	protected SimplePoint pointFromMap(Object mapIn) throws MissingPropertyException {

		if (mapIn instanceof SimplePoint) {
			return (SimplePoint) mapIn;
		}

		@SuppressWarnings("unchecked")
		Map<String, Double> map = (Map<String, Double>) mapIn;

		if (!map.containsKey("x"))
			throw new MissingPropertyException("missing property X");
		if (!map.containsKey("y"))
			throw new MissingPropertyException("missing property Y");
		// if(!map.containsKey("x")) throw new MissingPropertyException("missing
		// property X") ;

		SimplePoint ans = new SimplePoint();
		ans.setX(map.get("x"));
		ans.setY(map.get("y"));

		if (map.containsKey("z")) {
			ans.setZ(map.get("z"));
		}

		return ans;

	}

	public String previewGcode() {
		// This method seems intended for debugging/preview purposes.
		// Using logger here, but consider if this method is still needed long-term.
		LOGGER.debug("Generating G-code preview for element: {}", getElementName());
		StringBuilder ansBuilder = new StringBuilder("----- GCode preview for " + getElementName() + "\n");
		int i = 0;
		List<Behaviour> currentBehaviours = getBehaviours(); // Avoid repeated calls
		LOGGER.debug("Total behaviours for element '{}': {}", getElementName(), currentBehaviours.size());
		for (Behaviour behaviour : currentBehaviours) {
			LOGGER.trace("Processing behaviour #{} for element '{}'", i, getElementName());
			i++;
			if (behaviour instanceof GcodeBehaviour) {
				// Creating a new GcodeProject just for preview might be inefficient.
				// Consider passing necessary parameters directly if possible.
				try {
					String gcode = ((GcodeBehaviour) behaviour).getGcode(new GcodeProject("test", BitHead.LASER));
					ansBuilder.append(gcode).append("\n");
					LOGGER.trace("Generated G-code snippet: {}", gcode.trim());
				} catch (Exception e) {
					LOGGER.error("Error generating G-code snippet for behaviour {} in element {}",
							behaviour.getClass().getSimpleName(), getElementName(), e);
					ansBuilder.append("(Error generating G-code for behaviour: ")
							.append(behaviour.getClass().getSimpleName()).append(")\n");
				}
			} else {
				LOGGER.trace("Skipping non-GcodeBehaviour: {}", behaviour.getClass().getSimpleName());
			}
		}

		ansBuilder.append("----- end GCode preview for ").append(getElementName());
		LOGGER.debug("G-code preview generation complete for element: {}", getElementName());
		return ansBuilder.toString();
	}

	public void reloadBehaviour() throws MissingPropertyException, UnknownElementException {

		throw new UnknownElementException();
	}

	public Element() {
		setElementName(this.getClass().getSimpleName());
		setSubType(this.getClass().getSimpleName());
	}

	public Object getProperty(String key) {
		return properties.get(key);
	}

	public void setProperty(String k, Object value) {
		properties.put(k, value);
	}

	public List<Behaviour> getBehaviours() {

		return this.behaviours;
	}

	public String getElementName() {

		return this.name;
	}

	public void setElementName(String name) {
		this.name = name;

	}

	public WeakHashMap<String, Object> getProperties() {
		return properties;
	}

	public void setProperties(WeakHashMap<String, Object> properties) {
		this.properties = properties;
	}

	public String getSubType() {
		return subType;
	}

	public void setSubType(String subType) {
		this.subType = subType;
	}

}
