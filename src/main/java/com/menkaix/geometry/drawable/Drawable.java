package com.menkaix.geometry.drawable;

import java.awt.Color;
import java.awt.Graphics;
import java.io.Serializable;

public interface Drawable extends Serializable {
	
	public void update();
	public void draw(Graphics graphics) ;
	public void setColor(Color color) ;
	

}
