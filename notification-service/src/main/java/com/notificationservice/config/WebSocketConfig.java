package com.notificationservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @org.springframework.beans.factory.annotation.Value("${FRONTEND_URL}")
    private String frontendUrl;

	@Override
	public void configureMessageBroker(MessageBrokerRegistry config) {
		// Enable a simple memory-based message broker
		config.enableSimpleBroker("/topic", "/queue");
		config.setApplicationDestinationPrefixes("/app");
		// Required for targeted notifications (convertAndSendToUser)
		config.setUserDestinationPrefix("/user");
	}

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		// Port 8087 WebSocket endpoint for the React frontend
		registry.addEndpoint("/ws-notifications").setAllowedOrigins(frontendUrl).withSockJS();
	}
}