package com.menkaix.pcbgcode.main;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.menkaix.project.GcodeProject;
import com.menkaix.project.GcodeProjectDefinition;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@SpringBootApplication
public class Main {

	private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

	public static void main(String[] args) {
		SpringApplication.run(Main.class, args);
	}

	@Bean
	public CommandLineRunner run() {
		return args -> {
			jsonContent();
			LOGGER.info("Processing finished.");
		};
	}

	private void jsonContent() {
		LOGGER.info("Starting JSON content processing...");
		String s;
		try {
			s = Files.readString(Path.of("input-sample-router.json"));

			Gson gson = (new GsonBuilder()).create();

			GcodeProjectDefinition prjDef = gson.fromJson(s, GcodeProjectDefinition.class);

			GcodeProject prj = prjDef.generate();

			prj.saveJson("");
			LOGGER.info("JSON output saved.");
			prj.writeGcode();
			LOGGER.info("G-code output saved.");

		} catch (IOException e) {
			LOGGER.error("Failed to read or process input JSON file", e);
		} catch (Exception e) { // Catch other potential exceptions during generation/saving
			LOGGER.error("An unexpected error occurred during project processing", e);
		}
	}
}
