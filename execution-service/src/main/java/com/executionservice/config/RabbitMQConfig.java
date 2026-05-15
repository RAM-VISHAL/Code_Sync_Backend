package com.executionservice.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
	public static final String EXECUTION_QUEUE = "execution_queue";
	public static final String EXECUTION_EXCHANGE = "execution_exchange";
	public static final String ROUTING_KEY = "execution.run";

	@Bean
	public Queue queue() {
		return new Queue(EXECUTION_QUEUE, true);
	}

	@Bean
	public TopicExchange exchange() {
		return new TopicExchange(EXECUTION_EXCHANGE);
	}

	@Bean
	public Binding binding(Queue queue, TopicExchange exchange) {
		return BindingBuilder.bind(queue).to(exchange).with(ROUTING_KEY);
	}
}