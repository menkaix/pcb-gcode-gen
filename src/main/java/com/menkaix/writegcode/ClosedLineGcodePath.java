package com.menkaix.writegcode;

import com.menkaix.geometry.components.SimplePoint;
import com.menkaix.project.GcodeProject;
import com.menkaix.project.behaviours.GcodeBehaviour;
import com.menkaix.project.behaviours.Geometry;
import com.menkaix.project.values.BitHead;

public class ClosedLineGcodePath implements GcodeBehaviour {

	private Geometry geometry;
	private Double feedRate;
	private Double power;

	public ClosedLineGcodePath(Geometry geometry) {
		setGeometry(geometry);
	}

	@Override
	public void update() {
		// TODO Auto-generated method stub

	}

	@Override
	public String getGcode(GcodeProject project) {
		
		if(geometry==null) {
			return "(null geometry in polygon)";
		}
		
		if(geometry.getPoints().size()<=0) {
			return "(empty geometry in polygon)";
		}

		String ans = "\n";
		
		if(feedRate==null) {
			feedRate = project.getFeedRate();
		}
		
		if(power==null) {
			power = project.getPower();
		}

		// retrait ici en cas de fraiseuse (avant S0)
		if (project.getBitHead() == BitHead.ROTOR) {
			ans += "G0 Z" + project.getSafeLevel() + "\n";
		}

		ans += "G0 X" + geometry.getPoints().get(0).getX() + " Y" + geometry.getPoints().get(0).getY() + "\n";
		
		ans += "S" + power + "\n";

		for (int i = 0; i < geometry.getPoints().size(); i++) {
			
			ans += "G1 X" + geometry.getPoints().get(i).getX() + " Y" + geometry.getPoints().get(i).getY() + " Z"
					+ (project.getPass() * project.getPassIncrement()) + " F" + feedRate + "\n";

		}

		ans += "G1 X" + geometry.getPoints().get(0).getX() + " Y" + geometry.getPoints().get(0).getY() + " Z"
				+ (project.getPass() * project.getPassIncrement()) + " F" + feedRate + "\n";

		// retrait ici en cas de fraiseuse (avant S0)
		if (project.getBitHead() == BitHead.ROTOR) {
			ans += "G0 Z" + project.getSafeLevel() + "\n";
		}
		ans += "S0\n";
		return ans;
	}

	public Geometry getGeometry() {
		return geometry;
	}

	public void setGeometry(Geometry geometry) {
		this.geometry = geometry;
	}

	public Double getFeedRate() {
		return feedRate;
	}

	public void setFeedRate(Double feedRate) {
		this.feedRate = feedRate;
	}

	public Double getPower() {
		return power;
	}

	public void setPower(Double power) {
		this.power = power;
	}

}
