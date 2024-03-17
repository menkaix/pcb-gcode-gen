package com.menkaix.geometry.figures;

import com.menkaix.geometry.basics.SimplePoint;
import com.menkaix.project.Behaviour;

public interface Collider extends Behaviour{
	
	public boolean contains(SimplePoint point) ;
	
	public boolean collides(PolygonCollider other);

}
