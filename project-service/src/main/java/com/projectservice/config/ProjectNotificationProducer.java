package com.projectservice.config;

import java.util.HashMap;
import java.util.Map;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ProjectNotificationProducer {

	private static final String PROJECT_NOTIFICATION_QUEUE = "project-notification-queue";

	@Autowired
	private RabbitTemplate rabbitTemplate;

	public void send(Integer recipientId, Integer senderId, String senderName, String recipientEmail, Long relatedId,
			String type, String message, String projectName, String recipientName) {
		Map<String, Object> payload = new HashMap<>();
		payload.put("recipientId", recipientId);
		payload.put("senderId", senderId);
		payload.put("senderName", senderName);
		payload.put("recipientEmail", recipientEmail);
		payload.put("relatedId", relatedId);
		payload.put("type", type);
		payload.put("message", message);
		payload.put("projectName", projectName);
		payload.put("recipientName", recipientName);
		rabbitTemplate.convertAndSend(PROJECT_NOTIFICATION_QUEUE, payload);
	}
}
