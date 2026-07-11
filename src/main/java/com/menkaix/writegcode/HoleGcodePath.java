package com.menkaix.writegcode;

import com.menkaix.geometry.components.SimplePoint;
import com.menkaix.project.GcodeProject;
import com.menkaix.project.Layer;
import com.menkaix.project.behaviours.GcodeBehaviour;
import com.menkaix.project.values.BitHead;

/**
 * A single drill cycle: rapid to (X, Y), plunge to the owning layer's
 * {@code holeDepth}, retract. Unlike every other {@link GcodeBehaviour}, the
 * Z it cuts to is not derived from {@code project.getPass() *
 * project.getPassIncrement()} — a hole's depth is a fixed per-layer setting,
 * not something that grows across multiple passes.
 */
public class HoleGcodePath implements GcodeBehaviour {

	private SimplePoint position;
	private Double feedRate;
	private Double power;

	public HoleGcodePath(SimplePoint position) {
		this.position = position;
	}

	@Override
	public void update() {
	}

	@Override
	public String getGcode(GcodeProject project) {

		if (position == null) {
			return "(null position in hole)";
		}

		if (feedRate == null) {
			feedRate = project.getFeedRate();
		}

		if (power == null) {
			power = project.getPower();
		}

		Layer layer = project.getCurrentLayer();
		double depth = layer != null ? layer.getHoleDepth() : project.getPass() * project.getPassIncrement();

		// A router's spindle is already spinning continuously at the configured
		// power from the initial M3 (see GcodeProject/GcodeFileWriter) — only a
		// laser needs its power switched on/off around each individual cut.
		boolean toggleSpindle = project.getBitHead() != BitHead.ROUTER;

		String ans = "\n";

		if (project.getBitHead() == BitHead.ROUTER) {
			ans += "G0 Z" + project.getSafeLevel() + "\n";
		}

		ans += "G0 X" + position.getX() + " Y" + position.getY() + "\n";

		if (toggleSpindle) {
			ans += "S" + power + "\n";
		}

		ans += "G1 X" + position.getX() + " Y" + position.getY() + " Z" + depth + " F" + feedRate + "\n";

		if (project.getBitHead() == BitHead.ROUTER) {
			ans += "G0 Z" + project.getSafeLevel() + "\n";
		}
		if (toggleSpindle) {
			ans += "S0\n";
		}

		return ans;
	}

	public SimplePoint getPosition() {
		return position;
	}

	public void setPosition(SimplePoint position) {
		this.position = position;
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
