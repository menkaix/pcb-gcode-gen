package com.menkaix.web;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.menkaix.blocks.BlockLibraryService;

/**
 * Manages the project's block library repositories (git URLs, persisted on
 * {@code GcodeProjectDefinition}) and the blocks discovered in them.
 */
@RestController
@RequestMapping("/api/blocks")
public class BlockController {

	private final ProjectService projectService;
	private final BlockLibraryService blockLibraryService;

	public BlockController(ProjectService projectService, BlockLibraryService blockLibraryService) {
		this.projectService = projectService;
		this.blockLibraryService = blockLibraryService;
	}

	@GetMapping("/repositories")
	public Map<String, Object> getRepositories() {
		Map<String, Object> ans = new LinkedHashMap<>();
		ans.put("urls", projectService.getBlockRepositories());
		return ans;
	}

	@PutMapping("/repositories")
	public Map<String, Object> setRepositories(@RequestBody RepositoriesRequest request) {
		Map<String, Object> ans = new LinkedHashMap<>();
		ans.put("urls", projectService.setBlockRepositories(request == null ? null : request.urls));
		return ans;
	}

	/** Clones/pulls every configured repository and rescans it for block JSON files. */
	@PostMapping("/reload")
	public Map<String, Object> reload() {
		List<Map<String, Object>> statuses = blockLibraryService.reload(projectService.getBlockRepositories());
		Map<String, Object> ans = new LinkedHashMap<>();
		ans.put("repositories", statuses);
		ans.put("blocks", blockLibraryService.listBlocks());
		return ans;
	}

	/** Currently cached blocks, without triggering a network reload. */
	@GetMapping
	public List<Map<String, Object>> list() {
		return blockLibraryService.listBlocks();
	}

	public static class RepositoriesRequest {
		public List<String> urls;
	}

}
