package com.menkaix.writegcode;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GcodeFileWriter {

	private String filePath;
	private List<String> gcodes;

	public void initializeGcode() {
		gcodes.add("M3 S0"); //set rotation clockwise, no spin
//		gcodes.add("S0");
		gcodes.add("G0 X0 Y0"); //
//		gcodes.add("S1000");

	}

	public void finalizeGcode() {

		gcodes.add("S0");
		gcodes.add("M5 S0");

	}

	
	public void write() {
		File file = new File(getFilePath());
		
		try {
			
			BufferedWriter bfWriter = new BufferedWriter(new FileWriter(file));
			
			for (String gcode : gcodes) {
				bfWriter.write(gcode+"\n");
			}
			
			bfWriter.close();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	//============ GETTERS AND SETTERS ============
	
	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public List<String> getGcodes() {
		return gcodes;
	}

	public void setGcodes(List<String> gcodes) {
		this.gcodes = gcodes;
	}

	//============= CONSTRUCTORS ==============
	
	public GcodeFileWriter(String path) {
		setFilePath(path);
		setGcodes(new ArrayList<String>());
	}

}
