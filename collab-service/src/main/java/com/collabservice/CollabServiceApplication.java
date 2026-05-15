package com.collabservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * CodeSync Collaboration-Service (Port 8084) Manages live co-editing sessions,
 * cursor sync, and participant presence.
 */
@SpringBootApplication
@EnableDiscoveryClient
public class CollabServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(CollabServiceApplication.class, args);
	}
}