package com.menkaix.web;

import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lists the vector (outline) fonts installed on the machine running the
 * generator, so the web UI can suggest names for a TextElement's
 * {@code fontFamily} field. A .ttf/.otf file path is also accepted there and
 * isn't covered by this list.
 */
@RestController
@RequestMapping("/api/fonts")
public class FontController {

	@GetMapping
	public List<String> list() {
		String[] families = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
		Arrays.sort(families);
		return Arrays.asList(families);
	}

}
