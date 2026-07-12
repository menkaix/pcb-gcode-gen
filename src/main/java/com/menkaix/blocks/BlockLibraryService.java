package com.menkaix.blocks;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import jakarta.annotation.PostConstruct;

/**
 * Loads block libraries from git repositories: clones/pulls each configured
 * URL into a local cache directory, scans it for block JSON files, and keeps
 * the discovered blocks in memory for {@link BlockResolver} and the web UI
 * to consume.
 *
 * <p>
 * This is a local, single-user tool (same trust model as {@code
 * ProjectService}): a bad URL only affects this machine's own cache, so
 * validation here is limited to what keeps a malicious/typo'd URL from being
 * interpreted as a {@code git} command-line flag or hanging the request
 * forever, not a full security sandbox.
 */
@Service
public class BlockLibraryService {

	private static final Logger LOGGER = LoggerFactory.getLogger(BlockLibraryService.class);

	private static volatile BlockLibraryService INSTANCE;

	private static final Path CACHE_ROOT = Path.of("blocks-cache");
	private static final long GIT_TIMEOUT_SECONDS = 30;

	private final Gson gson = new Gson();
	private final Object lock = new Object();
	private final Map<String, BlockCacheEntry> blocksById = new LinkedHashMap<>();

	@PostConstruct
	public void init() {
		INSTANCE = this;
	}

	public static BlockLibraryService getInstance() {
		return INSTANCE;
	}

	private static class BlockCacheEntry {
		String id;
		String blockName;
		String repoUrl;
		int componentCount;
		BlockDefinition definition;
	}

	/**
	 * Clones (or pulls, if already cloned) every configured repository and
	 * rescans it for block JSON files, replacing the in-memory cache
	 * wholesale. One repository failing (bad URL, network error, timeout)
	 * does not stop the others.
	 */
	public List<Map<String, Object>> reload(List<String> repositoryUrls) {
		synchronized (lock) {
			List<Map<String, Object>> statuses = new ArrayList<>();
			Map<String, BlockCacheEntry> newCache = new LinkedHashMap<>();

			if (repositoryUrls != null) {
				for (String url : repositoryUrls) {
					Map<String, Object> status = new LinkedHashMap<>();
					status.put("url", url);
					try {
						validateUrl(url);
						Path dir = cloneOrPull(url);
						int found = scanRepo(url, dir, newCache);
						status.put("ok", true);
						status.put("blocksFound", found);
					} catch (Exception e) {
						LOGGER.warn("Failed to load block repository '{}'", url, e);
						status.put("ok", false);
						status.put("error", e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
					}
					statuses.add(status);
				}
			}

			blocksById.clear();
			blocksById.putAll(newCache);
			return statuses;
		}
	}

	public List<Map<String, Object>> listBlocks() {
		synchronized (lock) {
			List<Map<String, Object>> ans = new ArrayList<>();
			for (BlockCacheEntry entry : blocksById.values()) {
				Map<String, Object> m = new LinkedHashMap<>();
				m.put("id", entry.id);
				m.put("blockName", entry.blockName);
				m.put("repoUrl", entry.repoUrl);
				m.put("componentCount", entry.componentCount);
				ans.add(m);
			}
			return ans;
		}
	}

	public BlockDefinition getBlockDefinition(String id) {
		synchronized (lock) {
			BlockCacheEntry entry = blocksById.get(id);
			return entry == null ? null : entry.definition;
		}
	}

	private void validateUrl(String url) {
		if (url == null || url.isBlank()) {
			throw new IllegalArgumentException("empty block repository URL");
		}
		if (url.startsWith("-")) {
			// Never let a URL be mistaken for a git command-line flag.
			throw new IllegalArgumentException("invalid block repository URL: " + url);
		}
		boolean allowed = url.startsWith("http://") || url.startsWith("https://") || url.startsWith("ssh://")
				|| url.startsWith("file://") || url.startsWith("git@") || url.startsWith("/") || url.startsWith("./")
				|| url.startsWith("../");
		if (!allowed) {
			throw new IllegalArgumentException("unsupported block repository URL scheme: " + url);
		}
	}

	private Path cloneOrPull(String url) throws IOException, InterruptedException {
		Files.createDirectories(CACHE_ROOT);
		Path dir = CACHE_ROOT.resolve(slugFor(url));
		if (Files.isDirectory(dir.resolve(".git"))) {
			runGit(List.of("git", "-C", dir.toString(), "pull", "--ff-only"));
		} else {
			runGit(List.of("git", "clone", "--depth", "1", url, dir.toString()));
		}
		return dir;
	}

	private void runGit(List<String> command) throws IOException, InterruptedException {
		ProcessBuilder pb = new ProcessBuilder(command);
		pb.redirectErrorStream(true);
		// Never let a private repo's missing credentials block on an interactive
		// prompt: fail fast instead, same intent as the timeout below.
		pb.environment().put("GIT_TERMINAL_PROMPT", "0");

		Process process = pb.start();
		String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
		boolean finished = process.waitFor(GIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
		if (!finished) {
			process.destroyForcibly();
			throw new IOException("git operation timed out after " + GIT_TIMEOUT_SECONDS + "s: " + command);
		}
		if (process.exitValue() != 0) {
			throw new IOException("git command failed (" + process.exitValue() + "): " + output.trim());
		}
	}

	private int scanRepo(String repoUrl, Path dir, Map<String, BlockCacheEntry> into) throws IOException {
		String repoSlugName = repoNameFor(repoUrl);
		int count = 0;
		String gitDirMarker = File.separator + ".git" + File.separator;

		try (Stream<Path> stream = Files.walk(dir)) {
			List<Path> jsonFiles = stream.filter(p -> !p.toString().contains(gitDirMarker))
					.filter(p -> p.toString().endsWith(".json")).collect(Collectors.toList());

			for (Path file : jsonFiles) {
				try {
					String content = Files.readString(file);
					BlockDefinition definition = gson.fromJson(content, BlockDefinition.class);
					if (definition == null || definition.getElements() == null || definition.getElements().isEmpty()) {
						continue;
					}

					String baseName = file.getFileName().toString().replaceAll("(?i)\\.json$", "");
					// Includes the repo-relative subfolder path (e.g. "resistors/axial-5.08mm"),
					// not just the bare filename, so a library organized into folders (see
					// BlockController/the UI's "Bibliothèques de blocs" panel) both stays
					// collision-free across folders sharing a filename and lets the block
					// picker reflect that organization.
					String relativePath = dir.relativize(file).toString().replaceAll("(?i)\\.json$", "")
							.replace(File.separatorChar, '/');
					String id = repoSlugName + "/" + relativePath;

					BlockCacheEntry entry = new BlockCacheEntry();
					entry.id = id;
					entry.blockName = definition.getBlockName() != null ? definition.getBlockName() : baseName;
					entry.repoUrl = repoUrl;
					entry.componentCount = definition.getElements().size();
					entry.definition = definition;

					if (into.containsKey(id)) {
						LOGGER.warn("Duplicate block id '{}' (from repository '{}'), overwriting", id, repoUrl);
					}
					into.put(id, entry);
					count++;
				} catch (JsonSyntaxException e) {
					LOGGER.debug("Skipping non-block JSON file '{}': {}", file, e.getMessage());
				}
			}
		}
		return count;
	}

	private String slugFor(String url) {
		String hash = Integer.toHexString(url.hashCode());
		return hash + "-" + repoNameFor(url);
	}

	private String repoNameFor(String url) {
		String base = url.replaceAll("(?i)\\.git/?$", "");
		int lastSlash = Math.max(base.lastIndexOf('/'), base.lastIndexOf('\\'));
		String name = lastSlash >= 0 ? base.substring(lastSlash + 1) : base;
		name = name.replaceAll("[^a-zA-Z0-9._-]", "_");
		return name.isBlank() ? "repo" : name;
	}

}
