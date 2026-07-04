package com.menkaix.writegcode;

import com.menkaix.geometry.components.SimplePoint;
import com.menkaix.project.GcodeProject;
import com.menkaix.project.RotationDirection;
import com.menkaix.project.behaviours.GcodeBehaviour;
import com.menkaix.project.values.BitHead;

/**
 * Full circle in one center-offset (I/J) arc, instead of two radius-format
 * (R) semicircles. A single R-format arc can't describe a full 360° turn, so
 * splitting a circle into two R-format halves is a common workaround — but it
 * always makes the start/end points of each half exactly diametrically
 * opposite, i.e. a chord equal to the diameter. That's the degenerate
 * boundary case for R-format arcs where a controller's center/direction
 * resolution can become numerically unstable. I/J format states the center
 * directly and has no such ambiguity.
 */
public class CircleGcodePath implements GcodeBehaviour {

	private SimplePoint center;
	private double radius;
	private RotationDirection direction;

	private Double feedRate;
	private Double power;

	public CircleGcodePath(SimplePoint center, double radius, RotationDirection direction) {
		this.center = center;
		this.radius = radius;
		this.direction = direction;
	}

	@Override
	public void update() {
	}

	@Override
	public String getGcode(GcodeProject project) {

		if (feedRate == null) {
			feedRate = project.getFeedRate();
		}

		if (power == null) {
			power = project.getPower();
		}

		// A router's spindle is already spinning continuously at the configured
		// power from the initial M3 (see GcodeProject/GcodeFileWriter) — only a
		// laser needs its power switched on/off around each individual cut.
		boolean toggleSpindle = project.getBitHead() != BitHead.ROUTER;

		double startX = center.getX() - radius;
		double startY = center.getY();
		double z = project.getPass() * project.getPassIncrement();
		String gcodeDirection = direction == RotationDirection.COUNTER_CLOCKWISE ? "G3" : "G2";

		String ans = "\n";

		if (project.getBitHead() == BitHead.ROUTER) {
			ans += "G0 Z" + project.getSafeLevel() + "\n";
		}

		ans += "G0 X" + startX + " Y" + startY + "\n";

		if (toggleSpindle) {
			ans += "S" + power + "\n";
		}

		ans += "G1 X" + startX + " Y" + startY + " Z" + z + " F" + feedRate + "\n";
		ans += gcodeDirection + " X" + startX + " Y" + startY + " Z" + z + " F" + feedRate + " I" + radius + " J0\n";

		if (project.getBitHead() == BitHead.ROUTER) {
			ans += "G0 Z" + project.getSafeLevel() + "\n";
		}
		if (toggleSpindle) {
			ans += "S0\n";
		}

		return ans;
	}

	public SimplePoint getCenter() {
		return center;
	}

	public void setCenter(SimplePoint center) {
		this.center = center;
	}

	public double getRadius() {
		return radius;
	}

	public void setRadius(double radius) {
		this.radius = radius;
	}

	public RotationDirection getDirection() {
		return direction;
	}

	public void setDirection(RotationDirection direction) {
		this.direction = direction;
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
