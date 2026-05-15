package com.paymentservice.exception;

import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(com.razorpay.RazorpayException.class)
	public ResponseEntity<?> handleRazorpayException(com.razorpay.RazorpayException e) {
		return ResponseEntity.status(400)
				.body(Map.of("status", "gateway_error", "message", "Payment Provider Error: " + e.getMessage()));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<?> handleGeneralException(Exception e) {
		return ResponseEntity.status(500)
				.body(Map.of("status", "error", "message", "A payment processing error occurred."));
	}
}