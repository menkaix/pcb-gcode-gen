package com.menkaix.pcbgcode.main;

import com.menkaix.elements.ArcPath;
import com.menkaix.elements.Circle;
import com.menkaix.elements.PolyLineElement;
import com.menkaix.elements.Rectangle;
import com.menkaix.geometry.components.SimplePoint;
import com.menkaix.pcbgcode.utilities.DuplicateLayerNameException;
import com.menkaix.project.BitHead;
import com.menkaix.project.GcodeProject;
import com.menkaix.project.Layer;
import com.menkaix.project.RotationDirection;

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
		
		ArcPath arc = new ArcPath(
				new SimplePoint(25,30), 
				new SimplePoint(34,14), 200, RotationDirection.CLOCKWISE);
				

		PolyLineElement line = new PolyLineElement();
		line.setElementName("coude");
		line.addPoint(25, 30);
		line.addPoint(22, 20);
		line.addPoint(34, 14);
		
		Circle circle = new Circle(new SimplePoint(27, 10), 5);

		work.getElements().add(new Rectangle("Power Switch", 10, 10, 10, 12));
		work.getElements().add(line);
		work.getElements().add(arc);
		work.getElements().add(circle);

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
