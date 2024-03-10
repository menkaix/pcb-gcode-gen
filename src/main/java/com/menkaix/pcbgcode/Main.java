package com.menkaix.pcbgcode ;

import com.menkaix.writegcode.GcodeFileWriter;

/**
 *
 * 
 */
public class Main {
    
    public static void main(String[] args) {
    	
    	GcodeFileWriter gfw = new GcodeFileWriter("test.nc") ;
    	
    	gfw.initializeGcode();
    	gfw.finalizeGcode();
    	
    	gfw.write();
    	
    }
    
}

