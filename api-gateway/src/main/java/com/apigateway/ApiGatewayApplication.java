package com.apigateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * CodeSync API Gateway - Port 9010 Acts as the Security Guard and Traffic
 * Controller. All frontend requests land here before being routed to specific
 * microservices.
 */
@SpringBootApplication
@EnableDiscoveryClient // Registers Gateway as a Eureka client
public class ApiGatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiGatewayApplication.class, args);
	}
}
