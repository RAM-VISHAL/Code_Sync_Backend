package com.apigateway.config;

import java.util.List;

import org.springframework.http.HttpMethod;
import org.springframework.http.server.PathContainer;
import org.springframework.stereotype.Component;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import org.springframework.web.server.ServerWebExchange;

@Component
public class RouteSecurityRules {

	private static final PathPatternParser PATH_PATTERN_PARSER = new PathPatternParser();

	private static final List<PathPattern> PUBLIC_MATCHERS = List.of(
			PATH_PATTERN_PARSER.parse("/api/v1/auth/register"),
			PATH_PATTERN_PARSER.parse("/api/v1/auth/login"),
			PATH_PATTERN_PARSER.parse("/api/v1/auth/send-registration-otp"),
			PATH_PATTERN_PARSER.parse("/api/v1/auth/send-otp"),
			PATH_PATTERN_PARSER.parse("/api/v1/auth/reset-password"),
			PATH_PATTERN_PARSER.parse("/api/v1/auth/oauth2/**"),
			PATH_PATTERN_PARSER.parse("/api/v1/auth/login/oauth2/**"),
			PATH_PATTERN_PARSER.parse("/swagger-ui.html"),
			PATH_PATTERN_PARSER.parse("/swagger-ui/**"),
			PATH_PATTERN_PARSER.parse("/v3/api-docs/**"),
			PATH_PATTERN_PARSER.parse("/actuator/**"),
			PATH_PATTERN_PARSER.parse("/ws-notifications/**"),
			PATH_PATTERN_PARSER.parse("/ws-collab/**"));

	private static final List<PathPattern> ADMIN_MATCHERS = List.of(
			PATH_PATTERN_PARSER.parse("/api/v1/admin/**"),
			PATH_PATTERN_PARSER.parse("/admin/**"));

	public boolean isPublic(ServerWebExchange exchange) {
		if (HttpMethod.OPTIONS.equals(exchange.getRequest().getMethod())) {
			return true;
		}

		PathContainer path = exchange.getRequest().getPath().pathWithinApplication();
		String pathValue = path.value();
		HttpMethod method = exchange.getRequest().getMethod();

		if (matchesAny(PUBLIC_MATCHERS, path)) {
			return true;
		}

		if (HttpMethod.GET.equals(method)
				&& ("/api/v1/projects/public".equals(pathValue) || pathValue.matches("^/api/v1/projects/[^/]+$"))) {
			return true;
		}

		if (HttpMethod.POST.equals(method) && "/api/v1/files/clone".equals(pathValue)) {
			return true;
		}

		if (pathValue.startsWith("/api/v1/payments/")) {
			return true;
		}

		return false;
	}

	public boolean isAdmin(ServerWebExchange exchange) {
		return matchesAny(ADMIN_MATCHERS, exchange.getRequest().getPath().pathWithinApplication());
	}

	private boolean matchesAny(List<PathPattern> matchers, PathContainer path) {
		for (PathPattern matcher : matchers) {
			if (matcher.matches(path)) {
				return true;
			}
		}
		return false;
	}
}
