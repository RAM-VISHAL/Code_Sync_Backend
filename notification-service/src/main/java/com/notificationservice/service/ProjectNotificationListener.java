package com.notificationservice.service;

import com.notificationservice.entity.Notification;
import java.util.List;
import java.util.Map;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProjectNotificationListener {

	@Autowired
	private NotificationService notificationService;

	@RabbitListener(queues = "project-notification-queue")
	public void handleProjectNotification(Map<String, Object> payload) {
		Integer recipientId = parseInteger(payload.get("recipientId"));
		if (recipientId == null) {
			return;
		}

		Notification notification = Notification.builder().recipientId(recipientId)
				.senderId(parseInteger(payload.get("senderId"))).senderName(asText(payload.get("senderName")))
				.relatedId(parseLong(payload.get("relatedId"))).message(asText(payload.get("message")))
				.type(asText(payload.get("type"))).recipientName(asText(payload.get("recipientName"))).projectName(asText(payload.get("projectName"))).build();

		String recipientEmail = asText(payload.get("recipientEmail"));
		List<String> emails = recipientEmail != null && !recipientEmail.isBlank() ? List.of(recipientEmail) : null;
		notificationService.sendProjectNotification(notification, emails);
	}

	private Integer parseInteger(Object value) {
		if (value instanceof Number number) {
			return number.intValue();
		}
		if (value instanceof String text && !text.isBlank()) {
			try {
				return Integer.parseInt(text);
			} catch (NumberFormatException ignored) {
				return null;
			}
		}
		return null;
	}

	private Long parseLong(Object value) {
		if (value instanceof Number number) {
			return number.longValue();
		}
		if (value instanceof String text && !text.isBlank()) {
			try {
				return Long.parseLong(text);
			} catch (NumberFormatException ignored) {
				return null;
			}
		}
		return null;
	}

	private String asText(Object value) {
		return value != null ? String.valueOf(value) : null;
	}
}
