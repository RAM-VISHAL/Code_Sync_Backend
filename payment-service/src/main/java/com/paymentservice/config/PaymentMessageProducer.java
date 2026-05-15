package com.paymentservice.config;

import java.util.Map;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PaymentMessageProducer {

	@Autowired
	private RabbitTemplate rabbitTemplate;

	public void sendPaymentStatusNotification(String userId, String email, String orderId, Double amount, String status) {
		String normalizedStatus = status != null ? status.toUpperCase() : "PENDING";
		String messageText = "SUCCESS".equals(normalizedStatus)
				? "Payment of Rs " + amount + " for order " + orderId + " was successful!"
				: "Payment of Rs " + amount + " for order " + orderId + " is currently pending.";

		Map<String, Object> message = Map.of("userId", userId, "email", email, "orderId", orderId, "amount", amount,
				"status", normalizedStatus, "type", "PAYMENT_" + normalizedStatus, "message", messageText);

		rabbitTemplate.convertAndSend("payment-notification-queue", message);
	}
}
