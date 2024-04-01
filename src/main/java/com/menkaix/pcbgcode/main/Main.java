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
		//prj.setPassIncrement(-0.025);
		
		Layer layer = prj.getLayer("default") ;
		layer.setPasses(15);
		
		layer.addElement(new Rectangle("rectangle", new SimplePoint(10, 5), 12.5, 17.5));
		layer.addElement(new Circle(new SimplePoint(30, 21), 1.5));
		
		Layer labels = new Layer("labels");
		labels.setPasses(1);
		
		labels.addElement(new ArcPath(new SimplePoint(30, 10), new SimplePoint(35,15), 12, RotationDirection.CLOCKWISE));
		
		PolyLineElement line = new PolyLineElement() ;
		
		line.addPoint(30, 10);
		line.addPoint(45, 12);
		line.addPoint(35, 15);
		
		
		labels.addElement(line);
		
		try {
			prj.addLayer(labels);
		} catch (DuplicateLayerNameException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		prj.saveJson("");
		prj.writeGcode();
		
//		String s;
//		try {
//			s = Files.readString(Path.of("input-sample.json"));
//			
//			Gson gson = (new GsonBuilder()).create();
//			
//			GcodeProject prj = gson.fromJson(s, GcodeProject.class) ;
//
//			prj.saveJson("");
//			prj.writeGcode();
//
//			
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
		
		System.out.println("done");

	}

}
