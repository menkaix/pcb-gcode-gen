package com.menkaix.pcbgcode ;

import com.menkaix.project.GcodeProject;
import com.menkaix.writegcode.GcodeFileWriter;

/**
 *
 * 
 */
public class Main {
    
    public static void main(String[] args) {
    	
    	GcodeProject prj = new GcodeProject();
    	
    	prj.writeGcode();
    	
    	System.out.println("done");
    	
    }
    
}

