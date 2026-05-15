package com.apigateway.config;

import java.util.List;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import io.jsonwebtoken.Claims;
import reactor.core.publisher.Mono;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GatewayJwtAuthenticationFilter implements WebFilter {

	private static final String BEARER_PREFIX = "Bearer ";

	private final JwtTokenService jwtTokenService;
	private final RouteSecurityRules routeSecurityRules;

	public GatewayJwtAuthenticationFilter(JwtTokenService jwtTokenService, RouteSecurityRules routeSecurityRules) {
		this.jwtTokenService = jwtTokenService;
		this.routeSecurityRules = routeSecurityRules;
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
		if (routeSecurityRules.isPublic(exchange)) {
			return chain.filter(exchange);
		}

		String authorizationHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
		if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
			return writeError(exchange, HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header");
		}

		String token = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
		Claims claims;
		try {
			claims = jwtTokenService.parseToken(token);
		} catch (JwtTokenService.InvalidJwtException ex) {
			return writeError(exchange, HttpStatus.UNAUTHORIZED, ex.getMessage());
		}

		String role = claims.get("role", String.class);
		if (routeSecurityRules.isAdmin(exchange) && !isAdmin(role)) {
			return writeError(exchange, HttpStatus.FORBIDDEN, "Admin access required");
		}

		ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
				.headers(headers -> {
					headers.set("X-Authenticated-User", claims.getSubject());
					setIfPresent(headers, "X-Authenticated-UserId", claims.get("userId"));
					setIfPresent(headers, "X-Authenticated-Role", role);
					headers.put("X-Authenticated-Via", List.of("api-gateway"));
				})
				.build();

		return chain.filter(exchange.mutate().request(mutatedRequest).build());
	}

	private boolean isAdmin(String role) {
		return role != null && ("ADMIN".equalsIgnoreCase(role) || "ROLE_ADMIN".equalsIgnoreCase(role));
	}

	private void setIfPresent(HttpHeaders headers, String headerName, Object value) {
		if (value != null) {
			headers.set(headerName, String.valueOf(value));
		}
	}

	private Mono<Void> writeError(ServerWebExchange exchange, HttpStatus status, String message) {
		exchange.getResponse().setStatusCode(status);
		exchange.getResponse().getHeaders().set(HttpHeaders.CONTENT_TYPE, "application/json");
		byte[] body = ("{\"message\":\"" + message + "\"}").getBytes();
		return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(body)));
	}
}
