package com.menkaix.writegcode;

//import com.menkaix.geometry.components.SimplePoint;
import com.menkaix.project.GcodeProject;
import com.menkaix.project.behaviours.GcodeBehaviour;
import com.menkaix.project.behaviours.Geometry;
import com.menkaix.project.values.BitHead;

import org.slf4j.Logger; // Added
import org.slf4j.LoggerFactory; // Added

public class LineGcodePath implements GcodeBehaviour {

	private static final Logger LOGGER = LoggerFactory.getLogger(LineGcodePath.class); // Added logger

	private Geometry geometry;
	private Double feedRate;
	private Double power;

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

	public LineGcodePath(Geometry geometry) {
		setGeometry(geometry);
	}

	@Override
	public void update() {

	}

	@Override
	public String getGcode(GcodeProject project) {

		if (geometry == null) {
			return "(null geometry in line)";
		}

		if (geometry.getPoints().size() <= 0) {
			return "(empty geometry in line)";
		}

		try {

			String ans = "\n";

			if (feedRate == null) {
				feedRate = project.getFeedRate();
			}

			if (power == null) {
				power = project.getPower();
			}

			// retrait ici en cas de fraiseuse (avant S0)
			if (project.getBitHead() == BitHead.ROUTER) {
				ans += "G0 Z" + project.getSafeLevel() + "\n";
			}
			ans += "S0\n";

			ans += "G0 X" + geometry.getPoints().get(0).getX() + " Y" + geometry.getPoints().get(0).getY() + "\n";
			ans += "S" + power + "\n";

			for (int i = 0; i < geometry.getPoints().size(); i++) {
				// SimplePoint point = geometry.getPoints().get(i);

				ans += "G1 X" + geometry.getPoints().get(i).getX() + " Y" + geometry.getPoints().get(i).getY() + " Z"
						+ (project.getPass() * project.getPassIncrement()) + " F" + feedRate + "\n";

			}

			// retrait ici en cas de fraiseuse (avant S0)
			if (project.getBitHead() == BitHead.ROUTER) {
				ans += "G0 Z" + project.getSafeLevel() + "\n";
			}
			ans += "S0\n";
			return ans;
		} catch (NullPointerException e) {
			LOGGER.error("Null pointer exception during G-code generation for line path. Project: {}, Geometry: {}",
					project, geometry, e);
			return "(error: null pointer)";
		} catch (IndexOutOfBoundsException e) {
			LOGGER.error(
					"Index out of bounds exception during G-code generation for line path. Project: {}, Geometry points size: {}",
					project, (geometry != null ? geometry.getPoints().size() : "null"), e);
			return "(error: index out of bounds)";
		}
	}

	public Geometry getGeometry() {
		return geometry;
	}

	public void setGeometry(Geometry geometry) {
		this.geometry = geometry;
	}

}
