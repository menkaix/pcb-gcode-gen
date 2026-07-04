package com.menkaix.web;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.menkaix.pcbgcode.utilities.DuplicateLayerNameException;
import com.menkaix.project.Layer;

@RestController
@RequestMapping("/api/layers")
public class LayerController {

	private final ProjectService projectService;

	public LayerController(ProjectService projectService) {
		this.projectService = projectService;
	}

	@GetMapping
	public List<Map<String, Object>> list() {
		return projectService.listLayers();
	}

	@PostMapping
	public ResponseEntity<Map<String, Object>> create(@RequestBody LayerRequest request)
			throws DuplicateLayerNameException {
		Map<String, Object> created = projectService.addLayer(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}

	@GetMapping("/{layerIndex}")
	public Layer get(@PathVariable int layerIndex) {
		return projectService.getLayerAt(layerIndex);
	}

	@PutMapping("/{layerIndex}")
	public Map<String, Object> update(@PathVariable int layerIndex, @RequestBody LayerRequest request)
			throws DuplicateLayerNameException {
		return projectService.updateLayer(layerIndex, request);
	}

	@DeleteMapping("/{layerIndex}")
	public ResponseEntity<Void> delete(@PathVariable int layerIndex) {
		projectService.removeLayerAt(layerIndex);
		return ResponseEntity.noContent().build();
	}

}
