package com.menkaix.web;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.menkaix.project.GcodeProject;
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

	/** Wholesale replace of the project — backs the "import a project JSON" feature. */
	@PutMapping
	public GcodeProjectDefinition replaceProject(@RequestBody GcodeProjectDefinition newDefinition) {
		return projectService.replaceDefinition(newDefinition);
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

	/** Resolved project JSON for the "download the generated JSON" feature — a pure export, no disk write. */
	@GetMapping("/generated")
	public GcodeProject getGeneratedProject() {
		return projectService.buildResolvedProject();
	}

	/** G-code text for the "generate and download the .nc" feature — a pure export, no disk write. */
	@GetMapping("/gcode")
	public Map<String, Object> getGcode() {
		GcodeProject project = projectService.buildResolvedProject();
		Map<String, Object> ans = new LinkedHashMap<>();
		ans.put("projectName", project.getProjectName());
		ans.put("gcode", project.getGcodeText());
		return ans;
	}

}
