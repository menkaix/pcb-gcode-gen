package com.menkaix.writegcode;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger; // Added
import org.slf4j.LoggerFactory; // Added

public class GcodeFileWriter {

	private static final Logger LOGGER = LoggerFactory.getLogger(GcodeFileWriter.class); // Added

	private String filePath;
	private List<String> gcodes;

	/**
	 * @param startPower spindle/laser power to command in the initial M3. Router
	 *                   jobs pass the project's configured power here and never
	 *                   touch S again until the final M5, since a physical
	 *                   spindle should spin continuously rather than stop/start
	 *                   between every cut; laser jobs pass 0 and modulate S
	 *                   per-shape instead (see ArcGcodePath/LineGcodePath/
	 *                   ClosedLineGcodePath/CircleGcodePath).
	 */
	public void initializeGcode(double startPower) {
		gcodes.add("G21"); // explicit metric mode: all coordinates and feed rates are in millimeters
		gcodes.add("G90"); // absolute positioning: X/Y/Z are absolute coordinates, not deltas
		gcodes.add("G17"); // XY plane: required for G2/G3 arcs to be interpreted correctly
		gcodes.add("M3 S" + startPower); // set rotation clockwise, at the given starting power
		gcodes.add("G0 X0 Y0"); //

	}

	public void finalizeGcode() {

		gcodes.add("S0");
		gcodes.add("M5 S0");
		gcodes.add("M30"); // end of program (and rewind), so senders/controllers see an explicit finish

	}

	public void write() {
		File file = new File(getFilePath());

		try {

			BufferedWriter bfWriter = new BufferedWriter(new FileWriter(file));

			for (String gcode : gcodes) {
				bfWriter.write(gcode + "\n");
			}

			bfWriter.close();
			LOGGER.debug("Successfully wrote G-code to file: {}", filePath);

		} catch (IOException e) {
			LOGGER.error("Failed to write G-code to file: {}", filePath, e);
		}
	}

	// ============ GETTERS AND SETTERS ============

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public List<String> getGcodes() {
		return gcodes;
	}

	public void setGcodes(List<String> gcodes) {
		this.gcodes = gcodes;
	}

	// ============= CONSTRUCTORS ==============

	public GcodeFileWriter(String path) {
		setFilePath(path);
		setGcodes(new ArrayList<String>());
	}

}
