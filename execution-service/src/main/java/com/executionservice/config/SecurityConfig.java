package com.executionservice.config;

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
            // 1. Disable CSRF to allow the POST /submit request from React
            .csrf(csrf -> csrf.disable())
            
            // 2. Set as Stateless (standard for Microservices)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // 3. Configure permissions
            .authorizeHttpRequests(auth -> auth
                // Allow all execution endpoints (submit, status, cancel)
                .requestMatchers("/submit/**", "/user/**").permitAll()
                .requestMatchers("/{jobId}/**").permitAll()
                // Allow internal health and swagger checks
                .requestMatchers("/actuator/**", "/v3/api-docs/**", "/swagger-ui/**").permitAll()
                // Permitting all for development to bypass 403 errors
                .anyRequest().permitAll() 
            )
            
            // 4. Disable login redirects that cause CORS preflight failures
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable());

        return http.build();
    }
}