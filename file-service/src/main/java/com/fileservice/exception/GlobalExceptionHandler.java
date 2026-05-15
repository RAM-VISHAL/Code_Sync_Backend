package com.fileservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.HashMap;
import java.util.Map;

/**
 * Global Exception Handler for File-Service. Ensures file system errors are
 * displayed clearly on the Monaco Editor UI.
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
		ex.printStackTrace(); // Log the actual error to the console for debugging
		Map<String, String> response = new HashMap<>();
		response.put("message", "A file system error occurred: " + ex.getMessage());
		response.put("status", "error");
		return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
	}
}