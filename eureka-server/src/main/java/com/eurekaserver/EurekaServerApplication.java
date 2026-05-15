package com.eurekaserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Service Discovery Server (The Phonebook of CodeSync) This application keeps
 * track of every microservice instance running in the ecosystem to allow
 * dynamic inter-service communication.
 */
@SpringBootApplication
@EnableEurekaServer // Key annotation to activate the Discovery Server logic
public class EurekaServerApplication {

	public static void main(String[] args) {
		// Entry point for the CodeSync Discovery Engine
		SpringApplication.run(EurekaServerApplication.class, args);
	}

}