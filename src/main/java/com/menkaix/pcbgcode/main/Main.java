package com.menkaix.pcbgcode.main ;

import com.menkaix.elements.PolyLineElement;
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
    	
    	GcodeProject prj = new GcodeProject("output-sample",BitHead.LASER);
    	
    	Layer work = new Layer("work");
    	work.setPasses(15);
    	
    	work.getElements().add(new Rectangle("Power Switch", 10, 10, 10, 12)) ;
    	
    	PolyLineElement line = new PolyLineElement();
    	
    	line.addPoint(15, 20);
    	line.addPoint(18, 20);
    	line.addPoint(22, 14);
    	
    	work.getElements().add(line);
    	
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

