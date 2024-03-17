package com.menkaix.elements;

import java.io.Serializable;
import java.util.List;

import com.menkaix.project.Behaviour;

public interface Element extends Serializable {
	
	public String getElementName() ;
	public void setElementName(String name) ;
	
	public List<Behaviour> getBehaviours();
	

}
