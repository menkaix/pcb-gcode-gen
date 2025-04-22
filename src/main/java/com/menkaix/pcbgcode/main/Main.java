package com.menkaix.pcbgcode.main;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import com.menkaix.project.GcodeProject;
import com.menkaix.project.GcodeProjectDefinition;

/**
 *
 * 
 */
public class Main {

	private static void jsonContent() {
		String s;
		try {
			s = Files.readString(Path.of("input-sample-router.json"));

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
		// staticContent();

		System.out.println("done");

	}

}
