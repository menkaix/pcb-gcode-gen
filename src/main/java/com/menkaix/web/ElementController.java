package com.menkaix.web;

import java.util.ArrayList;
import java.util.LinkedHashMap;
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

import com.menkaix.elements.Element;
import com.menkaix.pcbgcode.utilities.MissingPropertyException;
import com.menkaix.pcbgcode.utilities.UnknownElementException;

@RestController
@RequestMapping("/api/layers/{layerIndex}/elements")
public class ElementController {

	private final ProjectService projectService;

	public ElementController(ProjectService projectService) {
		this.projectService = projectService;
	}

	@GetMapping
	public List<Map<String, Object>> list(@PathVariable int layerIndex) {
		List<Element> elements = projectService.listElements(layerIndex);
		List<Map<String, Object>> ans = new ArrayList<>();
		for (int i = 0; i < elements.size(); i++) {
			ans.add(entry(i, elements.get(i)));
		}
		return ans;
	}

	@GetMapping("/{elementIndex}")
	public Element get(@PathVariable int layerIndex, @PathVariable int elementIndex) {
		return projectService.getElementAt(layerIndex, elementIndex);
	}

	@PostMapping
	public ResponseEntity<Map<String, Object>> create(@PathVariable int layerIndex, @RequestBody Element element)
			throws MissingPropertyException, UnknownElementException {
		int index = projectService.addElement(layerIndex, element);
		return ResponseEntity.status(HttpStatus.CREATED).body(entry(index, element));
	}

	@PutMapping("/{elementIndex}")
	public Map<String, Object> update(@PathVariable int layerIndex, @PathVariable int elementIndex,
			@RequestBody Element element) throws MissingPropertyException, UnknownElementException {
		projectService.updateElement(layerIndex, elementIndex, element);
		return entry(elementIndex, element);
	}

	@DeleteMapping("/{elementIndex}")
	public ResponseEntity<Void> delete(@PathVariable int layerIndex, @PathVariable int elementIndex) {
		projectService.removeElement(layerIndex, elementIndex);
		return ResponseEntity.noContent().build();
	}

	private Map<String, Object> entry(int index, Element element) {
		Map<String, Object> ans = new LinkedHashMap<>();
		ans.put("index", index);
		ans.put("element", element);
		return ans;
	}

}
