package com.apigateway.config;

import java.security.Key;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtTokenService {

	private final Key signingKey;

	public JwtTokenService(@Value("${jwt.secret}") String jwtSecret) {
		this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes());
	}

	public Claims parseToken(String token) {
		try {
			Jws<Claims> parsedToken = Jwts.parserBuilder().setSigningKey(signingKey).build().parseClaimsJws(token);
			return parsedToken.getBody();
		} catch (JwtException | IllegalArgumentException ex) {
			throw new InvalidJwtException("Invalid or expired JWT token", ex);
		}
	}

	public static class InvalidJwtException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		public InvalidJwtException(String message, Throwable cause) {
			super(message, cause);
		}
	}
}
