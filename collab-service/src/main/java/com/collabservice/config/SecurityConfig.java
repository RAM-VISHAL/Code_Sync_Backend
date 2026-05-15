package com.collabservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
				// Disable CSRF for API-driven microservice interactions
				.csrf(csrf -> csrf.disable())

				// Ensure no HTTP session is created (Stateless JWT mode)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

				.authorizeHttpRequests(auth -> auth
						// Permitting access to documentation and health check endpoints
						.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/actuator/**").permitAll()
						// Session endpoints are routed via Gateway
						.requestMatchers("/api/v1/sessions/**").permitAll()
						// Permit WebSocket handshakes
						.requestMatchers("/ws-collab/**").permitAll().anyRequest().authenticated())
				// Disable default Login/Logout redirects to keep it a pure API
				.formLogin(form -> form.disable()).httpBasic(basic -> basic.disable());

		return http.build();
	}
}