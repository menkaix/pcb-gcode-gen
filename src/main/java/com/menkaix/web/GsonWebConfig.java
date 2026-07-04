package com.menkaix.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

@Configuration
public class GsonWebConfig {

	@Bean
	public Gson gson() {
		// Pretty-printed so downloaded/exported project JSON is readable, matching
		// GcodeProject.saveJson()'s own formatting. Payloads stay small at this
		// app's scale (a handful of shapes), so the extra bytes don't matter.
		return new GsonBuilder().setPrettyPrinting().create();
	}

}
