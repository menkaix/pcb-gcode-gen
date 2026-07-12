package com.menkaix.elements;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.PathIterator;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.menkaix.geometry.basic.PolyGone;
import com.menkaix.geometry.components.SimplePoint;
import com.menkaix.pcbgcode.utilities.MissingPropertyException;
import com.menkaix.writegcode.ClosedLineGcodePath;

/**
 * A line of text rendered with a vector (outline) font and converted into
 * closed G-code paths, one per glyph contour (so letters with holes, like
 * "O" or "A", keep their counters). {@code fontFamily} is either the name of
 * a font installed on the system running the generator, or a filesystem path
 * to a .ttf/.otf font file to embed.
 */
public class TextElement extends Element {

	private static final long serialVersionUID = -2740510402863887201L;

	/** Curve flattening tolerance, in the same unit as the rest of the project (mm). */
	private static final double FLATNESS_MM = 0.15;

	private static final ConcurrentHashMap<String, Font> FONT_FILE_CACHE = new ConcurrentHashMap<>();

	private static final Set<String> SYSTEM_FONT_FAMILIES = new HashSet<>();
	static {
		SYSTEM_FONT_FAMILIES
				.addAll(Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
		SYSTEM_FONT_FAMILIES.addAll(
				Arrays.asList(Font.SANS_SERIF, Font.SERIF, Font.MONOSPACED, Font.DIALOG, Font.DIALOG_INPUT));
	}

	private transient List<List<SimplePoint>> contours = new ArrayList<>();

	@Override
	public void reloadBehaviour() throws MissingPropertyException {
		checkMandatoryProperties("text", "position", "fontSize", "fontFamily");

		String text = getProperty("text").toString();
		SimplePoint position = pointFromMap(getProperty("position"));
		double fontSize = Double.parseDouble(getProperty("fontSize").toString());
		String fontFamily = getProperty("fontFamily").toString();
		boolean bold = isTruthy(getProperty("bold"));
		boolean italic = isTruthy(getProperty("italic"));

		Font font = resolveFont(fontFamily, bold, italic, fontSize);
		contours = glyphContours(font, text, position);

		double rotationDegrees = getRotationDegrees();
		if (rotationDegrees != 0.0) {
			for (List<SimplePoint> contour : contours) {
				for (int i = 0; i < contour.size(); i++) {
					contour.set(i, SimplePoint.rotate(contour.get(i), position, rotationDegrees));
				}
			}
		}

		getBehaviours().clear();
		for (List<SimplePoint> contour : contours) {
			if (contour.size() < 3) {
				continue;
			}
			PolyGone geometry = new PolyGone();
			for (SimplePoint p : contour) {
				geometry.addPoint(p.getX(), p.getY());
			}
			getBehaviours().add(geometry);
			getBehaviours().add(new ClosedLineGcodePath(geometry));
		}
	}

	private boolean isTruthy(Object property) {
		return property != null && Boolean.parseBoolean(property.toString());
	}

	private Font resolveFont(String fontFamily, boolean bold, boolean italic, double fontSize) {
		int style = Font.PLAIN | (bold ? Font.BOLD : 0) | (italic ? Font.ITALIC : 0);
		String lower = fontFamily.toLowerCase(Locale.ROOT);

		Font base;
		if (lower.endsWith(".ttf") || lower.endsWith(".otf")) {
			base = FONT_FILE_CACHE.computeIfAbsent(fontFamily, TextElement::loadFontFile);
		} else {
			if (!SYSTEM_FONT_FAMILIES.contains(fontFamily)) {
				throw new IllegalArgumentException(
						"Police introuvable : '" + fontFamily + "' (ni fichier .ttf/.otf, ni police système installée)");
			}
			base = new Font(fontFamily, Font.PLAIN, 1);
		}

		return base.deriveFont(style, (float) fontSize);
	}

	private static Font loadFontFile(String path) {
		try {
			return Font.createFont(Font.TRUETYPE_FONT, new File(path));
		} catch (FontFormatException | IOException e) {
			throw new IllegalArgumentException(
					"Impossible de charger la police vectorielle '" + path + "' : " + e.getMessage(), e);
		}
	}

	private static List<List<SimplePoint>> glyphContours(Font font, String text, SimplePoint position) {
		FontRenderContext frc = new FontRenderContext(null, true, true);
		GlyphVector glyphVector = font.createGlyphVector(frc, text);
		Shape outline = glyphVector.getOutline(0f, 0f);
		PathIterator pathIterator = outline.getPathIterator(null, FLATNESS_MM);

		List<List<SimplePoint>> result = new ArrayList<>();
		List<SimplePoint> current = null;
		double[] coords = new double[6];

		while (!pathIterator.isDone()) {
			int type = pathIterator.currentSegment(coords);
			switch (type) {
				case PathIterator.SEG_MOVETO:
					current = new ArrayList<>();
					result.add(current);
					current.add(toWorldPoint(coords[0], coords[1], position));
					break;
				case PathIterator.SEG_LINETO:
					current.add(toWorldPoint(coords[0], coords[1], position));
					break;
				case PathIterator.SEG_CLOSE:
					// The consuming PolyGone/ClosedLineGcodePath already closes back to its
					// first point, so no extra point is needed here.
					break;
				default:
					// getPathIterator(null, flatness) only ever emits MOVETO/LINETO/CLOSE.
					break;
			}
			pathIterator.next();
		}

		return result;
	}

	/**
	 * AWT glyph outlines use a screen-style Y-down space (ascenders at negative
	 * Y). The rest of this project's geometry is Y-up, so the Y axis is flipped
	 * here, once, at the source.
	 */
	private static SimplePoint toWorldPoint(double glyphX, double glyphY, SimplePoint position) {
		// position.getZ() is null whenever the "position" property's JSON omits
		// "z" (valid and common - see Element#pointFromMap, which treats z as
		// optional everywhere else), so it can't be unboxed directly here.
		double z = position.getZ() != null ? position.getZ() : 0.0;
		return new SimplePoint(position.getX() + glyphX, position.getY() - glyphY, z);
	}

	public List<List<SimplePoint>> getContours() {
		return contours;
	}

	public TextElement() {
		super();
	}

}
