package com.menkaix.pcbgcode.main;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.menkaix.elements.ArcPath;
import com.menkaix.elements.Circle;
import com.menkaix.elements.PolyLineElement;
import com.menkaix.elements.Rectangle;
import com.menkaix.geometry.components.SimplePoint;
import com.menkaix.pcbgcode.utilities.DuplicateLayerNameException;
import com.menkaix.project.GcodeProject;
import com.menkaix.project.GcodeProjectDefinition;
import com.menkaix.project.Layer;
import com.menkaix.project.RotationDirection;
import com.menkaix.project.values.BitHead;

/**
 *
 * 
 */
public class Main {

	private static void staticContent() {

		GcodeProject prj = new GcodeProject("output-sample", BitHead.LASER);
		prj.setPassIncrement(-0.15);

		Layer layer = prj.getLayer("default");
		layer.setPasses(100);

		layer.addElement(new Rectangle("rectangle", new SimplePoint(10, 5), 12.5, 17.5));
		layer.addElement(new Circle(new SimplePoint(30, 21), 1.5));

		Layer labels = new Layer("labels");
		labels.setPasses(1);

		labels.addElement(
				new ArcPath(new SimplePoint(30, 10), new SimplePoint(35, 15), 12, RotationDirection.CLOCKWISE));

		PolyLineElement line = new PolyLineElement();

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

	}

	private static void jsonContent() {
		String s;
		try {
			s = Files.readString(Path.of("input-sample.json"));

			Gson gson = (new GsonBuilder()).create();

			GcodeProjectDefinition prjDef = gson.fromJson(s, GcodeProjectDefinition.class);

			GcodeProject prj = prjDef.generate();

			prj.saveJson("");
			prj.writeGcode();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {

		jsonContent();
		//staticContent();

		System.out.println("done");

	}

}
