package com.menkaix.pcbgcode.main;

import java.util.Arrays;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import com.menkaix.web.ProjectService;

@SpringBootApplication
@ComponentScan(basePackages = "com.menkaix")
public class Main {

	private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

	private static final String DEFAULT_INPUT_FILE = "input-sample-router.json";
	private static final String HEADLESS_FLAG = "--headless";

	public static void main(String[] args) {
		boolean headless = Arrays.asList(args).contains(HEADLESS_FLAG);

		SpringApplicationBuilder builder = new SpringApplicationBuilder(Main.class);
		if (headless) {
			// Preserves the original CLI behavior: process once and exit, no embedded
			// web server.
			builder.web(WebApplicationType.NONE);
		}
		builder.run(args);
	}

	@Bean
	public CommandLineRunner run(ProjectService projectService) {
		return args -> {
			boolean headless = Arrays.asList(args).contains(HEADLESS_FLAG);
			String inputFile = firstNonFlagArg(args).orElse(DEFAULT_INPUT_FILE);

			projectService.loadFromFile(inputFile);

			if (headless) {
				LOGGER.info("Running in headless mode for file: {}", inputFile);
				projectService.generateAndWrite();
				LOGGER.info("Processing finished.");
			} else {
				LOGGER.info("Web GUI ready: open http://127.0.0.1:8080 in your browser (loaded project: {})",
						inputFile);
			}
		};
	}

	private Optional<String> firstNonFlagArg(String[] args) {
		for (String arg : args) {
			if (!arg.startsWith("--")) {
				return Optional.of(arg);
			}
		}
		return Optional.empty();
	}

}
