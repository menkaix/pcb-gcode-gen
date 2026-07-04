package com.menkaix.web;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.menkaix.project.GcodeProjectDefinition;

@RestController
@RequestMapping("/api/project")
public class ProjectController {

	private final ProjectService projectService;

	public ProjectController(ProjectService projectService) {
		this.projectService = projectService;
	}

	@GetMapping
	public GcodeProjectDefinition getProject() {
		return projectService.getDefinition();
	}

	@PutMapping("/meta")
	public GcodeProjectDefinition updateMeta(@RequestBody GcodeProjectDefinition meta) {
		return projectService.updateMeta(meta);
	}

	@PostMapping("/save")
	public Map<String, Object> save() {
		return projectService.save();
	}

	@PostMapping("/generate")
	public Map<String, Object> generate() {
		return projectService.generateAndWrite();
	}

}
