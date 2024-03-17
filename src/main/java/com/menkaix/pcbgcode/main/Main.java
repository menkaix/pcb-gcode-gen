package com.menkaix.pcbgcode.main ;

import com.menkaix.elements.Rectangle;
import com.menkaix.pcbgcode.utilities.DuplicateLayerNameException;
import com.menkaix.project.BitHead;
import com.menkaix.project.GcodeProject;
import com.menkaix.project.Layer;
import com.menkaix.writegcode.GcodeFileWriter;

/**
 *
 * 
 */
public class Main {
    
    public static void main(String[] args) {
    	
    	GcodeProject prj = new GcodeProject("mangatsika",BitHead.LASER);
    	
    	Layer work = new Layer("work");
    	work.setPasses(15);
    	
    	work.getElements().add(new Rectangle("Power Switch", 10, 10, 10, 12)) ;
    	
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

