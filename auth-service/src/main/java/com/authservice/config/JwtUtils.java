package com.authservice.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

/**
 * Utility for JWT Generation and Validation
 */
@Component
public class JwtUtils {

	@Value("${jwt.secret}") // Matched to application.yml
	private String jwtSecret;

	@Value("${jwt.expiry:86400000}")
	private long tokenExpiry;

	private Key getSigningKey() {
		// Ensure secret is securely encoded
		return Keys.hmacShaKeyFor(jwtSecret.getBytes());
	}

	public String generateToken(String username, Integer userId, String role) {
		return Jwts.builder().setSubject(username).claim("role", role).claim("userId", userId).setIssuedAt(new Date())
				.setExpiration(new Date(System.currentTimeMillis() + tokenExpiry))
				.signWith(getSigningKey(), SignatureAlgorithm.HS256).compact();
	}

	public String generateToken(String username, String role) {
		return generateToken(username, null, role);
	}

	public boolean validateToken(String token) {
		try {
			Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token);
			return true;
		} catch (Exception e) {
			return false; // Token is expired, malformed, or signature is invalid
		}
	}

	public String getUsernameFromToken(String token) {
		return Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token).getBody().getSubject();
	}
}
