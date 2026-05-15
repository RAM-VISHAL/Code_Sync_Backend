package com.collabservice.resource;

import java.util.HashMap;
import java.util.Map;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class CollabWebSocketResource {

	private final SimpMessagingTemplate messagingTemplate;

	public CollabWebSocketResource(SimpMessagingTemplate messagingTemplate) {
		this.messagingTemplate = messagingTemplate;
	}

	@MessageMapping("/session/{sessionId}")
	public void relaySessionMessage(@DestinationVariable String sessionId, @Payload Map<String, Object> payload) {
		Map<String, Object> message = payload == null ? new HashMap<>() : new HashMap<>(payload);
		message.put("sessionId", sessionId);
		messagingTemplate.convertAndSend("/topic/session/" + sessionId, message);
	}
}
