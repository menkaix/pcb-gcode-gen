package com.menkaix.elements;

import java.util.List;

import com.menkaix.geometry.basic.PolyGone;
import com.menkaix.writegcode.ClosedLineGcodePath;

/**
 * A merged copper island produced by {@code TraceMerger} from one or more
 * overlapping/touching {@link TraceElement}s. Holds one closed ring per
 * boundary of the merged footprint (the outer contour, plus one ring per
 * interior hole, e.g. a copper loop enclosing an isolated non-copper area) —
 * the same "several independent closed contours per shape" pattern already
 * used by {@link TextElement} for glyph counters.
 *
 * <p>
 * Runtime-only: never registered in {@code ElementFactory} and never meant to
 * be (de)serialized from/to project JSON — it only ever exists transiently
 * inside a resolved {@code GcodeProject}, built by {@code TraceMerger} right
 * before G-code generation.
 */
public class TraceIslandElement extends Element {

	private static final long serialVersionUID = -4269587310630556674L;

	public TraceIslandElement(List<PolyGone> rings) {
		super();
		setElementName("Trace island");
		getBehaviours().clear();
		for (PolyGone ring : rings) {
			getBehaviours().add(ring);
			// Trace islands never support tabs: a bridge left uncut would leave the
			// isolation gap unrouted at that point, re-connecting copper that was
			// meant to stay separated.
			getBehaviours().add(new ClosedLineGcodePath(ring, false));
		}
	}

}
