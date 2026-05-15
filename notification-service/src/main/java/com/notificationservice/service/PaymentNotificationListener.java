package com.notificationservice.service;

import com.notificationservice.entity.Notification;
import com.notificationservice.repository.NotificationRepository;
import java.util.Map;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class PaymentNotificationListener {

	@Autowired
	private JavaMailSender mailSender;

	@Autowired
	private NotificationRepository notificationRepository;

	@Autowired
	private SimpMessagingTemplate messagingTemplate;

	@RabbitListener(queues = "payment-notification-queue")
	public void handlePaymentNotification(Map<String, Object> message) {
		String userEmail = (String) message.get("email");
		String status = String.valueOf(message.getOrDefault("status", message.getOrDefault("type", "PAYMENT_SUCCESS")));
		String amount = String.valueOf(message.getOrDefault("amount", "0"));
		String orderId = String.valueOf(message.getOrDefault("orderId", ""));
		Integer recipientId = parseRecipientId(message.get("userId"));
		String body = String.valueOf(message.getOrDefault("message",
				"SUCCESS".equalsIgnoreCase(status)
						? "Payment of Rs " + amount + " for order " + orderId + " was successful."
						: "Payment of Rs " + amount + " for order " + orderId + " is currently pending."));

		if (recipientId != null) {
			Notification saved = notificationRepository.save(Notification.builder().recipientId(recipientId).message(body)
					.type("SUCCESS".equalsIgnoreCase(status) ? "PAYMENT_SUCCESS" : "PAYMENT_PENDING").build());
			messagingTemplate.convertAndSend("/topic/notifications/" + recipientId, saved);
		}

		if (userEmail != null && !userEmail.isBlank()) {
			SimpleMailMessage mailMessage = new SimpleMailMessage();
			mailMessage.setTo(userEmail);
			mailMessage.setSubject("CodeSync Payment Status: " + status);
			mailMessage.setText("Hello,\n\n" + body + "\n\nThank you for using CodeSync!");

			try {
				mailSender.send(mailMessage);
				System.out.println("Notification sent to " + userEmail);
			} catch (Exception e) {
				System.err.println("Failed to send notification: " + e.getMessage());
			}
		}
	}

	private Integer parseRecipientId(Object value) {
		if (value instanceof Number number) {
			return number.intValue();
		}
		if (value instanceof String text) {
			try {
				return Integer.parseInt(text);
			} catch (NumberFormatException ignored) {
				return null;
			}
		}
		return null;
	}
}
