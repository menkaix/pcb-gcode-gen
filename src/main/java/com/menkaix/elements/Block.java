package com.menkaix.elements;

import java.util.ArrayList;
import java.util.List;

import com.menkaix.blocks.BlockResolver;
import com.menkaix.elements.factory.ElementFactory;
import com.menkaix.pcbgcode.utilities.MissingPropertyException;
import com.menkaix.pcbgcode.utilities.UnknownElementException;

/**
 * A placed instance of a library block: a fixed group of native shapes
 * (loaded from {@code blockId}, see
 * {@link com.menkaix.blocks.BlockLibraryService}) repositioned as a whole by
 * {@code position}/{@code rotation}. The components' own relative
 * properties never change - only where and how the whole group sits, via
 * {@link BlockResolver}.
 */
public class Block extends Element {

	private static final long serialVersionUID = 1L;

	/**
	 * Guards against a block that (directly or transitively) references
	 * itself, which would otherwise recurse until the stack overflows. A
	 * {@code ThreadLocal} is safe here because one block instance's whole
	 * resolution tree - including any nested blocks - runs synchronously on a
	 * single thread; it never gets handed off mid-resolution.
	 */
	private static final int MAX_RESOLUTION_DEPTH = 8;
	private static final ThreadLocal<Integer> RESOLUTION_DEPTH = ThreadLocal.withInitial(() -> 0);

	private transient List<Element> resolvedChildren = new ArrayList<>();

	public Block() {
		super();
	}

	@Override
	public void reloadBehaviour() throws MissingPropertyException, UnknownElementException {

		checkMandatoryProperties("blockId", "position");

		int depth = RESOLUTION_DEPTH.get();
		if (depth >= MAX_RESOLUTION_DEPTH) {
			throw new MissingPropertyException(
					"block nesting too deep (possible self-referencing block): " + getProperty("blockId"));
		}
		RESOLUTION_DEPTH.set(depth + 1);
		try {
			List<Element> rawChildren = BlockResolver.resolveTransformedChildren(this);

			resolvedChildren = new ArrayList<>();
			getBehaviours().clear();
			for (Element raw : rawChildren) {
				Element resolved = ElementFactory.create(raw);
				resolvedChildren.add(resolved);
				getBehaviours().addAll(resolved.getBehaviours());
			}
		} finally {
			RESOLUTION_DEPTH.set(depth);
		}
	}

	public List<Element> getResolvedChildren() {
		return resolvedChildren;
	}

}
