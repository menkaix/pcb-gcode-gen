package com.menkaix.geometry.colliders;

import com.menkaix.geometry.components.SimplePoint;
import com.menkaix.project.behaviours.Behaviour;

public interface Collider extends Behaviour{
	
	public boolean contains(SimplePoint point) ;
	
	public boolean collides(PolygonCollider other);

}
