package com.notificationservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * CodeSync Notification Engine (Port 8087) Dispatches real-time WebSocket
 * events and asynchronous HTML emails.
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableAsync // Required to ensure Email sending doesn't block the UI thread
public class NotificationServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(NotificationServiceApplication.class, args);
	}
}