package com.menkaix.elements;

import com.menkaix.geometry.basic.PointCouple;
import com.menkaix.geometry.components.SimplePoint;
import com.menkaix.pcbgcode.utilities.MissingPropertyException;
import com.menkaix.project.RotationDirection;
import com.menkaix.project.behaviours.Geometry;
import com.menkaix.writegcode.ArcGcodePath;

import org.slf4j.Logger; // Added
import org.slf4j.LoggerFactory; // Added

public class ArcPath extends Element {

	private static final Logger LOGGER = LoggerFactory.getLogger(ArcPath.class); // Added

	/**
	 *
	 */
	private static final long serialVersionUID = -6371348113906900012L;

	private transient Geometry geometry;

	// private SimplePoint from ;
	// private SimplePoint to ;
	// private double radius ;

	private void init() throws MissingPropertyException {

		SimplePoint from = pointFromMap(getProperty("from"));
		SimplePoint to = pointFromMap(getProperty("to"));

		geometry = new PointCouple(from, to);

		// setRadius((Double)getProperty("radius"));
		// set((RotationDirection)getProperty("direction"));

		getBehaviours().clear();
		getBehaviours().add(geometry);
		getBehaviours()
				.add(new ArcGcodePath(geometry, RotationDirection.valueOf(this.getProperty("direction").toString()),
						Double.parseDouble(this.getProperty("radius").toString())));

	}

	// private SimplePoint from ;
	// private SimplePoint to ;
	// private double radius ;

	// private SimplePoint from ;
	// private SimplePoint to ;
	// private double radius ;

	@Override
	public void reloadBehaviour() throws MissingPropertyException {

		checkMandatoryProperties("from", "to", "radius", "direction");

		init();

	}

	public SimplePoint getFrom() {
		return (SimplePoint) getProperty("from");
	}

	public void setFrom(SimplePoint from) {
		setProperty("from", from);
	}

	public SimplePoint getTo() {
		return (SimplePoint) getProperty("to");
	}

	public void setTo(SimplePoint to) {
		setProperty("to", to);
	}

	public double getRadius() {
		return (Double) getProperty("radius");
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

	// private SimplePoint from ;
	// private SimplePoint to ;
	// private double radius ;

	// private SimplePoint from ;
	// private SimplePoint to ;
	// private double radius ;

	public ArcPath() {
		super();
	}

	public ArcPath(SimplePoint from, SimplePoint to, double radius, RotationDirection direction) {

		this();

		setProperty("from", from);
		setProperty("to", to);
		setProperty("radius", radius);
		setProperty("direction", direction);

		try {
			init();
		} catch (MissingPropertyException e) {
			// This error during construction might leave the object in an inconsistent
			// state.
			LOGGER.error(
					"Failed to initialize ArcPath due to missing properties. From: {}, To: {}, Radius: {}, Direction: {}",
					from, to, radius, direction, e);
			// Consider re-throwing as a runtime exception or handling more gracefully.
		}

		// geometry = new PointCouple(from, to);
		// getBehaviours().add(new ArcGcodePath(geometry, direction, radius));

	}

}
