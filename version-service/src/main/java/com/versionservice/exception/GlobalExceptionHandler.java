package com.versionservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.HashMap;
import java.util.Map;

/**
 * Global Exception Handler for Version-Service. Translates snapshot and diffing
 * errors into readable UI alerts.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(RuntimeException.class)
	public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException ex) {
		Map<String, String> response = new HashMap<>();
		response.put("message", ex.getMessage());
		response.put("status", "error");
		return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<Map<String, String>> handleGeneralException(Exception ex) {
		Map<String, String> response = new HashMap<>();
		response.put("message", "Version history could not be retrieved. Please try again.");
		response.put("status", "error");
		return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
	}
}