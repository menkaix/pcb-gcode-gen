package com.menkaix.project;

public class ClosedLineGcodePath implements GcodeBehaviour {
	
	private Geometry geometry ;
	private double feedRate = 100 ;
	private double power = 1000 ;
	
	public ClosedLineGcodePath(Geometry geometry) {
		setGeometry(geometry);
	}

	@Override
	public void update() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getGcode() {
		// TODO Auto-generated method stub
		return null;
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
