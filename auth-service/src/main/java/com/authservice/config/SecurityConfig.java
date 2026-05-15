package com.authservice.config;

import com.authservice.serviceImpl.CustomOAuth2UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;


/**
 * Security Configuration - Centralizes Auth Rules Standardizes Password
 * Encoding and OAuth2 Integration
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

	private final CustomOAuth2UserService customOAuth2UserService;
	private final String frontendUrl;
	private final JwtUtils jwtService;
	private final com.authservice.repository.UserRepository userRepository;

	public SecurityConfig(CustomOAuth2UserService customOAuth2UserService, @Value("${FRONTEND_URL}") String frontendUrl,
			JwtUtils jwtService, com.authservice.repository.UserRepository userRepository) {
		this.customOAuth2UserService = customOAuth2UserService;
		this.frontendUrl = frontendUrl;
		this.jwtService = jwtService;
		this.userRepository = userRepository;
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		// Industry Standard: BCrypt for one-way password hashing
		return new BCryptPasswordEncoder();
	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http.csrf(csrf -> csrf.disable())
				.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
				.oauth2Login(oauth2 -> oauth2
						.authorizationEndpoint(auth -> auth.baseUri("/api/v1/auth/oauth2/authorization"))
						.redirectionEndpoint(red -> red.baseUri("/api/v1/auth/login/oauth2/code/*"))
						.userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
						.successHandler((request, response, authentication) -> {
							String username = authentication.getName();
							com.authservice.entity.User user = userRepository.findByUsername(username).orElseThrow();
							// Generate Token for the OAuth User
							String token = jwtService.generateToken(username, user.getUserId(), user.getRole().toString());
							// Redirect to frontend with token in URL
							response.sendRedirect(frontendUrl + "/oauth-success?token=" + token);
						}));

		return http.build();
	}
}
