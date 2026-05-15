package com.notificationservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

	@Bean(name = "emailTaskExecutor")
	public Executor emailTaskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(5); // Base thread count
		executor.setMaxPoolSize(20); // Max threads during spikes
		executor.setQueueCapacity(500); // Queue up to 500 emails before rejecting
		executor.setThreadNamePrefix("EmailSender-");
		executor.initialize();
		return executor;
	}
}
