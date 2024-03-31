package com.menkaix.elements;

import java.io.Serializable;
import java.util.List;

import com.menkaix.project.Behaviour;

public abstract class Element implements Serializable {
	
	public  abstract  String getElementName() ;
	public abstract  void setElementName(String name) ;
	
	public  abstract  List<Behaviour> getBehaviours();
	

}
