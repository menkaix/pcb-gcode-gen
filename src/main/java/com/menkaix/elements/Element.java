package com.menkaix.elements;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

import com.menkaix.pcbgcode.utilities.MissingPropertyException;
import com.menkaix.pcbgcode.utilities.UnknownElementException;
import com.menkaix.project.Behaviour;

public class Element implements Serializable {

	private static final long serialVersionUID = 194916028736735923L;

	private String name;
	private String subType;
	
	private WeakHashMap<String, Object> properties = new WeakHashMap<String, Object>() ;

	private transient List<Behaviour> behaviours = new ArrayList<Behaviour>();
	
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
