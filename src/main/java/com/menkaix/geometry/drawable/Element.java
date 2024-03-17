package com.menkaix.geometry.drawable;

import java.awt.Color;
import java.awt.Graphics;
import java.io.Serializable;
import java.util.List;

import com.menkaix.project.Behaviour;

public interface Element extends Serializable {
	
	public void update();
	public void draw(Graphics graphics) ;
	public void setColor(Color color) ;
	public List<Behaviour> getBehaviours();
	

}
