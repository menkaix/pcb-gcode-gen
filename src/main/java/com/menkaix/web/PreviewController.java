package com.menkaix.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.menkaix.elements.Element;

@RestController
@RequestMapping("/api/preview")
public class PreviewController {

	private final ProjectService projectService;

	public PreviewController(ProjectService projectService) {
		this.projectService = projectService;
	}

	@GetMapping
	public List<PreviewShape> previewAll() {
		return projectService.previewAll();
	}

	/**
	 * Live preview for a form still being edited. Always returns 200: an invalid
	 * candidate is a normal, expected state while typing, not an API error.
	 */
	@PostMapping("/element")
	public PreviewShape previewCandidate(@RequestBody Element element) {
		return projectService.previewCandidate(element);
	}

}
