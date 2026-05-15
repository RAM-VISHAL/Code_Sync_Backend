package com.apigateway.config;

import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;

import java.util.Map;

/**
 * Custom Error Attributes to ensure the Frontend receives the real error
 * message from microservices instead of a generic "Internal Server Error".
 */
@Component
public class GlobalErrorAttributes extends DefaultErrorAttributes {

	@Override
	public Map<String, Object> getErrorAttributes(ServerRequest request, ErrorAttributeOptions options) {
		Map<String, Object> map = super.getErrorAttributes(request, options);

		// Extract the original exception to get the real message
		Throwable error = getError(request);
		if (error != null) {
			map.put("message", error.getMessage());
		}
		return map;
	}
}