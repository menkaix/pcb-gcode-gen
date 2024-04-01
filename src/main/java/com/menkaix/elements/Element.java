package com.menkaix.elements;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

import com.menkaix.project.Behaviour;

public class Element implements Serializable {

	private static final long serialVersionUID = 194916028736735923L;

	private String name;
	private String subType;
	
	protected transient List<Behaviour> behaviours = new ArrayList<Behaviour>();
	
	private WeakHashMap<String, Object> properties = new WeakHashMap<String, Object>() ;
	
	public Object getProperty(String key) {
		return properties.get(key);
	}
	
	public void setProperty(String k, Object value) {
		properties.put(k, value);
	}

	
	public Element() {
		setElementName(this.getClass().getSimpleName());
		setSubType(this.getClass().getSimpleName());
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
