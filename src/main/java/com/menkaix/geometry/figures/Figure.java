package com.menkaix.geometry.figures;

import java.util.ArrayList;
import java.util.List;

import com.menkaix.geometry.basics.SimplePoint;
import com.menkaix.geometry.basics.Vector;

public class Figure {

	

	public List<Vector> edges = new ArrayList<Vector>();

	public boolean contains(SimplePoint point) {
		// TODO Value !!!
		Vector probe = new Vector(point, new SimplePoint(point.getX() + 1000, point.getY(), point.getZ()));

		int i = 0;
		for (Vector v : edges) {
			if (v.doIntersect(probe)) {
				i++;
			}
		}

		//TODO attention si probe passe par un sommet
		return i % 2 != 0;

	}

	public boolean collides(Figure other) {

		for (Vector myV : edges) {
			for (Vector othV : other.edges) {
				if (myV.doIntersect(othV)) {
					return true;
				}
			}
		}

		if (this.contains(other.edges.get(0).getOrigin()) || other.contains(this.edges.get(0).getOrigin())) {
			return true;
		}

		return false;
	}

}
