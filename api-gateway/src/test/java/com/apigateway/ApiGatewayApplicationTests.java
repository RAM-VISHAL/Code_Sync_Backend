package com.apigateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.util.Date;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
		"eureka.client.enabled=false",
		"spring.cloud.discovery.enabled=false",
		"spring.cloud.gateway.server.webflux.discovery.locator.enabled=false",
		"jwt.secret=CodeSyncSuperSecretKeyForJWTAuthentication2026!"
})
class ApiGatewayApplicationTests {

	private static final String JWT_SECRET = "CodeSyncSuperSecretKeyForJWTAuthentication2026!";

	@LocalServerPort
	int port;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void contextLoads() {
	}

	@Test
	void publicEndpointDoesNotRequireToken() {
		webTestClient.get()
				.uri("http://localhost:" + port + "/api/v1/auth/login")
				.exchange()
				.expectStatus().isEqualTo(503);
	}

	@Test
	void protectedEndpointRejectsMissingToken() {
		webTestClient.get()
				.uri("http://localhost:" + port + "/api/v1/comments/file/1")
				.exchange()
				.expectStatus().isUnauthorized()
				.expectHeader().contentType(MediaType.APPLICATION_JSON)
				.expectBody()
				.jsonPath("$.message").isEqualTo("Missing or invalid Authorization header");
	}

	@Test
	void adminEndpointRejectsNonAdminToken() {
		webTestClient.get()
				.uri("http://localhost:" + port + "/api/v1/admin/users")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + createToken("USER"))
				.exchange()
				.expectStatus().isForbidden()
				.expectBody()
				.jsonPath("$.message").isEqualTo("Admin access required");
	}

	@Test
	void protectedEndpointAcceptsValidToken() {
		webTestClient.get()
				.uri("http://localhost:" + port + "/api/v1/comments/file/1")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + createToken("USER"))
				.exchange()
				.expectStatus().isEqualTo(503);
	}

	private String createToken(String role) {
		return Jwts.builder()
				.setSubject("test-user")
				.claim("role", role)
				.claim("userId", 42)
				.setIssuedAt(new Date())
				.setExpiration(new Date(System.currentTimeMillis() + 60_000))
				.signWith(Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
				.compact();
	}

}
