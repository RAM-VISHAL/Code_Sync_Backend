package com.executionservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * CodeSync Execution Engine (Port 8085) Orchestrates sandboxed containers to
 * run developer code safely.
 */
@SpringBootApplication
@EnableDiscoveryClient
public class ExecutionServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ExecutionServiceApplication.class, args);
	}
}