package com.menkaix.writegcode;

import java.util.ArrayList;
import java.util.List;

import com.menkaix.geometry.components.SimplePoint;
import com.menkaix.project.GcodeProject;
import com.menkaix.project.Layer;
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

		if (geometry == null) {
			return "(null geometry in polygon)";
		}

		if (geometry.getPoints().size() <= 0) {
			return "(empty geometry in polygon)";
		}

		String ans = "\n";

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

		// retrait ici en cas de fraiseuse (avant S0)
		if (project.getBitHead() == BitHead.ROUTER) {
			ans += "G0 Z" + project.getSafeLevel() + "\n";
		}

		ans += "G0 X" + geometry.getPoints().get(0).getX() + " Y" + geometry.getPoints().get(0).getY() + "\n";

		if (toggleSpindle) {
			ans += "S" + power + "\n";
		}

		double z = project.getPass() * project.getPassIncrement();

		Layer layer = project.getCurrentLayer();
		boolean tabsEnabled = layer != null && layer.isTabsEnabled() && layer.getTabCount() > 0
				&& layer.getTabWidth() > 0;

		if (!tabsEnabled) {

			for (int i = 0; i < geometry.getPoints().size(); i++) {

				ans += "G1 X" + geometry.getPoints().get(i).getX() + " Y" + geometry.getPoints().get(i).getY() + " Z"
						+ z + " F" + feedRate + "\n";

			}

			ans += "G1 X" + geometry.getPoints().get(0).getX() + " Y" + geometry.getPoints().get(0).getY() + " Z" + z
					+ " F" + feedRate + "\n";

		} else {

			// Closed vertex loop (last point duplicates the first, for the closing edge)
			// with the cumulative arc-length at each vertex, so tab boundaries can be
			// placed at arbitrary distances along the perimeter while still emitting
			// every original vertex (corners) that falls inside a cutting segment.
			List<SimplePoint> loopPoints = new ArrayList<>(geometry.getPoints());
			loopPoints.add(loopPoints.get(0));

			double[] cumDist = new double[loopPoints.size()];
			for (int i = 1; i < loopPoints.size(); i++) {
				cumDist[i] = cumDist[i - 1] + SimplePoint.distance(loopPoints.get(i - 1), loopPoints.get(i));
			}
			double perimeter = cumDist[cumDist.length - 1];

			int tabCount = layer.getTabCount();
			double slot = perimeter / tabCount;
			double cutLength = slot - layer.getTabWidth();
			if (cutLength <= 0) {
				cutLength = slot * 0.5;
			}

			for (int k = 0; k < tabCount; k++) {

				double cutStart = k * slot;
				double cutEnd = cutStart + cutLength;

				for (int i = 0; i < loopPoints.size(); i++) {
					if (cumDist[i] > cutStart && cumDist[i] < cutEnd) {
						SimplePoint p = loopPoints.get(i);
						ans += "G1 X" + p.getX() + " Y" + p.getY() + " Z" + z + " F" + feedRate + "\n";
					}
				}

				SimplePoint segEnd = pointAtDistance(loopPoints, cumDist, cutEnd);
				ans += "G1 X" + segEnd.getX() + " Y" + segEnd.getY() + " Z" + z + " F" + feedRate + "\n";

				if (k < tabCount - 1) {
					SimplePoint nextStart = pointAtDistance(loopPoints, cumDist, (k + 1) * slot);

					// Skip across the tab without cutting: retract (router) and/or cut the
					// power (laser) while traversing, then resume at the next segment.
					if (project.getBitHead() == BitHead.ROUTER) {
						ans += "G0 Z" + project.getSafeLevel() + "\n";
					}
					if (toggleSpindle) {
						ans += "S0\n";
					}
					ans += "G0 X" + nextStart.getX() + " Y" + nextStart.getY() + "\n";
					if (toggleSpindle) {
						ans += "S" + power + "\n";
					}
					ans += "G1 X" + nextStart.getX() + " Y" + nextStart.getY() + " Z" + z + " F" + feedRate + "\n";
				}
			}

		}

		// retrait ici en cas de fraiseuse (avant S0)
		if (project.getBitHead() == BitHead.ROUTER) {
			ans += "G0 Z" + project.getSafeLevel() + "\n";
		}
		if (toggleSpindle) {
			ans += "S0\n";
		}
		return ans;
	}

	/**
	 * Point at the given arc-length distance along a closed vertex loop, linearly
	 * interpolating within whichever edge that distance falls on.
	 */
	private SimplePoint pointAtDistance(List<SimplePoint> loopPoints, double[] cumDist, double targetDist) {
		int last = loopPoints.size() - 1;
		for (int i = 0; i < last; i++) {
			if (targetDist <= cumDist[i + 1]) {
				double segLen = cumDist[i + 1] - cumDist[i];
				double t = segLen <= 0 ? 0 : (targetDist - cumDist[i]) / segLen;
				SimplePoint a = loopPoints.get(i);
				SimplePoint b = loopPoints.get(i + 1);
				return new SimplePoint(a.getX() + t * (b.getX() - a.getX()), a.getY() + t * (b.getY() - a.getY()));
			}
		}
		return loopPoints.get(last);
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
