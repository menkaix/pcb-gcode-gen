package com.menkaix.geometry.colliders;

import com.menkaix.geometry.components.SimplePoint;
import com.menkaix.geometry.components.Vector;

public class Parallellepipede extends PolygonCollider {
	
	public Parallellepipede(SimplePoint o,SimplePoint i, SimplePoint j) {
		
		Vector oi = new Vector(o, i);
		Vector oj = new Vector(o, j);
		
		Vector ik = oj.clone();
		ik.setOrigin(i);
		
		Vector jk = oi.clone();
		jk.setOrigin(j);
				
		
		edges.add(oi);
		edges.add(oj);
		edges.add(ik);
		edges.add(jk);
		
	}
	

}
