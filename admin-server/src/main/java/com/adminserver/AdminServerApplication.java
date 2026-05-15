package com.adminserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import de.codecentric.boot.admin.server.config.EnableAdminServer;

/**
 * Admin Server - The centralized monitoring dashboard for CodeSync. Uses
 * Service Discovery (Eureka) to automatically find and monitor all 11
 * microservices.
 */
@SpringBootApplication
@EnableAdminServer // Powers the UI and Monitoring Engine
@EnableDiscoveryClient // Allows the Admin server to talk to Eureka
public class AdminServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(AdminServerApplication.class, args);
	}
}