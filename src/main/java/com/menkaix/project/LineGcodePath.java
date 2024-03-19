package com.menkaix.project;

import com.menkaix.geometry.components.SimplePoint;

public class LineGcodePath implements GcodeBehaviour {
	
	private Geometry geometry ;
	private double feedRate = 100 ;
	private double power = 1000 ;
	
	public LineGcodePath(Geometry geometry) {
		setGeometry(geometry);
	}

	@Override
	public void update() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getGcode(GcodeProject project) {
		
		String ans = "\n" ;
		
		
		
		ans += "G0 X"+geometry.getPoints().get(0).getX()+" Y"+geometry.getPoints().get(0).getY()+" Z"+(project.getPass()*project.getPassIncrement())+"\n" ;
		ans += "S"+power+"\n" ;
		
		for(int i = 1 ; i<geometry.getPoints().size() ; i++) {
			SimplePoint point = geometry.getPoints().get(i);
			
			ans += "G1 X"+geometry.getPoints().get(i).getX()+" Y"+geometry.getPoints().get(i).getY()+" Z"+(project.getPass()*project.getPassIncrement())+" F"+feedRate+"\n" ;
			
		}
			
		
		//retrait ici en cas de fraiseuse (avant S0)
		if(project.getBitHead() == BitHead.ROTOR) {
			ans += "G0 Z"+project.getSafeLevel() + "\n" ;
		}		
		ans += "S0\n" ;
		return ans;
	}

	public Geometry getGeometry() {
		return geometry;
	}

	public void setGeometry(Geometry geometry) {
		this.geometry = geometry;
	}

	public double getFeedRate() {
		return feedRate;
	}

	public void setFeedRate(double feedRate) {
		this.feedRate = feedRate;
	}

	public double getPower() {
		return power;
	}

	public void setPower(double power) {
		this.power = power;
	}

}
