package com.versionservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * CodeSync Version-Service (Port 8086) Handles snapshots, line-by-line diffs,
 * and version restoration.
 */
@SpringBootApplication
@EnableDiscoveryClient
public class VersionServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(VersionServiceApplication.class, args);
	}
}