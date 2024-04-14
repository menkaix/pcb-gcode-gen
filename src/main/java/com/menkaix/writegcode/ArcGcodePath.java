package com.menkaix.writegcode;

import com.menkaix.project.GcodeProject;
import com.menkaix.project.RotationDirection;
import com.menkaix.project.behaviours.GcodeBehaviour;
import com.menkaix.project.behaviours.Geometry;
import com.menkaix.project.values.BitHead;

public class ArcGcodePath implements GcodeBehaviour {

	private Geometry geometry;
	private RotationDirection direction;
	private double radius;

	private Double feedRate;
	private Double power;

	@Override
	public void update() {
		// TODO Auto-generated method stub

	}

	@Override
	public String getGcode(GcodeProject project) {

		//System.out.println("----- arc gcode ------");

		String ans = "\n";

		if (feedRate == null) {
			feedRate = project.getFeedRate();
		}

		if (power == null) {
			power = project.getPower();
		}

		// retrait ici en cas de fraiseuse (avant S0)
		if (project.getBitHead() == BitHead.ROTOR) {
			ans += "G0 Z" + project.getSafeLevel() + "\n";
		}

		ans += "G0 X" + geometry.getPoints().get(0).getX() + " Y" + geometry.getPoints().get(0).getY() + "\n";

		ans += "S" + power + "\n";

		ans += "G1 X" + geometry.getPoints().get(0).getX() + " Y" + geometry.getPoints().get(0).getY() + " Z"
				+ (project.getPass() * project.getPassIncrement()) + " F" + feedRate + "\n";

		for (int i = 1; i < geometry.getPoints().size(); i++) {

			if (geometry.getPoints().get(i) == null) {
				return "(err this point is null)";
			}

			if (geometry.getPoints().get(i).getX() == null || geometry.getPoints().get(i).getY() == null) {
				return "(X or Y is null)";
			}

			String gcodeDirection = "";

			if (direction == RotationDirection.CLOCKWISE) {
				gcodeDirection = "G2";
			} else if (direction == RotationDirection.COUNTER_CLOCKWISE) {
				gcodeDirection = "G3";
			}

			String path = " X" + geometry.getPoints().get(i).getX() + " Y" + geometry.getPoints().get(i).getY() + " Z"
					+ (project.getPass() * project.getPassIncrement()) + " F" + feedRate + " R" + radius + "\n";

			ans += gcodeDirection + path;

		}

		// retrait ici en cas de fraiseuse (avant S0)
		if (project.getBitHead() == BitHead.ROTOR) {
			ans += "G0 Z" + project.getSafeLevel() + "\n";
		}
		ans += "S0\n";
		
		//System.out.println("ans =" + ans);
		//System.out.println("----- end arc gcode ------");

		return ans;

	}

	public double getPower() {
		return power;
	}

	public void setPower(double power) {
		this.power = power;
	}

	public Geometry getGeometry() {
		return geometry;
	}

	public void setGeometry(Geometry geometry) {
		this.geometry = geometry;
	}

	public RotationDirection getDirection() {
		return direction;
	}

	public void setDirection(RotationDirection direction) {
		this.direction = direction;
	}

	public double getRadius() {
		return radius;
	}

	public void setRadius(double radius) {
		this.radius = radius;
	}

	public Double getFeedRate() {
		return feedRate;
	}

	public void setPower(Double power) {
		this.power = power;
	}

	public void setFeedRate(Double feedRate) {
		this.feedRate = feedRate;
	}

	public ArcGcodePath(Geometry geometry, RotationDirection direction, double radius) {

		this.setGeometry(geometry);
		this.setDirection(direction);
		this.setRadius(radius);

	}

}
