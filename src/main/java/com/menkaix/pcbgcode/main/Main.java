package com.menkaix.pcbgcode.main;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.menkaix.project.GcodeProject;

/**
 *
 * 
 */
public class Main {

	public static void main(String[] args) {

		//GcodeProject prj = new GcodeProject("output-sample", BitHead.LASER);
		String s;
		try {
			s = Files.readString(Path.of("input-sample.json"));
			
			Gson gson = (new GsonBuilder()).create();
			
			GcodeProject prj = gson.fromJson(s, GcodeProject.class) ;

			prj.saveJson("");
			prj.writeGcode();

			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		System.out.println("done");

	}

}
