package com.menkaix.web;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.google.gson.JsonSyntaxException;
import com.menkaix.pcbgcode.utilities.DuplicateLayerNameException;
import com.menkaix.pcbgcode.utilities.MissingPropertyException;
import com.menkaix.pcbgcode.utilities.UnknownElementException;

@RestControllerAdvice
public class ApiExceptionHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);

	@ExceptionHandler({ MissingPropertyException.class, UnknownElementException.class, JsonSyntaxException.class,
			IllegalArgumentException.class })
	public ResponseEntity<Map<String, Object>> handleBadRequest(Exception e) {
		return build(HttpStatus.BAD_REQUEST, e);
	}

	@ExceptionHandler(DuplicateLayerNameException.class)
	public ResponseEntity<Map<String, Object>> handleConflict(Exception e) {
		return build(HttpStatus.CONFLICT, e);
	}

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<Map<String, Object>> handleNotFound(Exception e) {
		return build(HttpStatus.NOT_FOUND, e);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<Map<String, Object>> handleOther(Exception e) {
		LOGGER.error("Unhandled exception in web API", e);
		return build(HttpStatus.INTERNAL_SERVER_ERROR, e);
	}

	private ResponseEntity<Map<String, Object>> build(HttpStatus status, Exception e) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("error", status.getReasonPhrase());
		body.put("message", e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
		return ResponseEntity.status(status).body(body);
	}

}
