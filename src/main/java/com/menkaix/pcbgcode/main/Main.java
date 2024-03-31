package com.menkaix.pcbgcode.main;

import com.menkaix.elements.Circle;
import com.menkaix.geometry.components.SimplePoint;
import com.menkaix.pcbgcode.utilities.DuplicateLayerNameException;
import com.menkaix.project.BitHead;
import com.menkaix.project.GcodeProject;
import com.menkaix.project.Layer;

/**
 *
 * 
 */
public class Main {

	public static void main(String[] args) {

		GcodeProject prj = new GcodeProject("output-sample", BitHead.LASER);

		prj.setSafeLevel(0);

		Layer work = new Layer("work");
		work.setPasses(15);

		Circle circle1 = new Circle(new SimplePoint(22.5, 22.5), 20);
		Circle circle2 = new Circle(new SimplePoint(22.5, 70), 20);
		Circle circle3 = new Circle(new SimplePoint(70, 22.5), 20);
		Circle circle4 = new Circle(new SimplePoint(70, 70), 20);

		work.getElements().add(circle1);
		work.getElements().add(circle2);
		work.getElements().add(circle3);
		work.getElements().add(circle4);

		try {
			prj.addLayer(work);
		} catch (DuplicateLayerNameException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		prj.saveJson("");
		prj.writeGcode();

		System.out.println("done");

	}

}
