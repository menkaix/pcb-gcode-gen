package com.menkaix.elements;

import java.util.Map;
import java.util.Properties;

import com.menkaix.geometry.basic.PointCouple;
import com.menkaix.geometry.components.SimplePoint;
import com.menkaix.pcbgcode.utilities.MissingPropertyException;
import com.menkaix.project.Geometry;
import com.menkaix.project.RotationDirection;
import com.menkaix.writegcode.ArcGcodePath;

public class ArcPath   extends  Element{

	/**
	 * 
	 */
	private static final long serialVersionUID = -6371348113906900012L;
	
	private transient Geometry geometry ;
	
//	private SimplePoint from ;
//	private SimplePoint to ;
//	private double radius ;
	
	public SimplePoint getFrom() {
		return (SimplePoint)getProperty("from");
	}

	public void setFrom(SimplePoint from) {
		setProperty("from", from); 
	}

	public SimplePoint getTo() {
		return (SimplePoint)getProperty("to");
	}

	public void setTo(SimplePoint to) {
		setProperty("to", to); 
	}

	public double getRadius() {
		return (Double)getProperty("radius");
	}

	public void setRadius(double radius) {
		setProperty("radius", radius); 
	}

	public Geometry getGeometry() {
		return geometry;
	}

	public void setGeometry(Geometry geometry) {
		this.geometry = geometry;
	}

	//	private SimplePoint from ;
	//	private SimplePoint to ;
	//	private double radius ;
		
		//	private SimplePoint from ;
	//	private SimplePoint to ;
	//	private double radius ;
		
		@Override
		public void reloadBehaviour() throws MissingPropertyException {
			
			if(!this.getProperties().containsKey("from")) throw new MissingPropertyException() ;
			if(!this.getProperties().containsKey("to")) throw new MissingPropertyException() ;
			if(!this.getProperties().containsKey("radius")) throw new MissingPropertyException() ;
			if(!this.getProperties().containsKey("direction")) throw new MissingPropertyException() ;
			
			init() ;
			
			
		}
		
		
		private void init() {
			SimplePoint from = null ;
			SimplePoint to = null ;
			try {
				//corner = (SimplePoint) getProperty("corner");
				from = (SimplePoint) getProperty("corner");
				to = (SimplePoint) getProperty("corner");
			}catch(ClassCastException e) {
				//Map<String, Double> cornerMap = (Map<String, Double>)getProperty("corner");
				Map<String, Double> fromMap = (Map<String, Double>)getProperty("from");
				Map<String, Double> toMap = (Map<String, Double>)getProperty("to");
				
				from = new SimplePoint(fromMap.get("x"), fromMap.get("y"));
				to = new SimplePoint(toMap.get("x"), toMap.get("y"));
				
			}
			
			
			geometry = new PointCouple(from, to);
			
			//setRadius((Double)getProperty("radius"));
			//set((RotationDirection)getProperty("direction"));
			
			getBehaviours().clear();
			
			getBehaviours().add(geometry);
			
			
			
			getBehaviours().add(new ArcGcodePath(geometry, 
					RotationDirection.valueOf(this.getProperty("direction").toString()),
					Double.parseDouble(this.getProperty("radius").toString())));
			
		}

		public ArcPath() {
			super() ;
		}

	public ArcPath(SimplePoint from, SimplePoint to, double radius, RotationDirection direction) {
		
		this() ;
		
		setProperty("from", from);
		setProperty("to", to);
		setProperty("radius", radius);
		setProperty("direction", direction);
		
		init() ;
		
		//geometry = new PointCouple(from, to);		
		//getBehaviours().add(new ArcGcodePath(geometry, direction, radius));
		
		
	}

}
