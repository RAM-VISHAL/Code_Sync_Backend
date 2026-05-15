package com.paymentservice.serviceTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

import com.paymentservice.config.PaymentMessageProducer;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@ExtendWith(MockitoExtension.class)
class PaymentMessageProducerTest {

	@Mock
	private RabbitTemplate rabbitTemplate;

	@InjectMocks
	private PaymentMessageProducer producer;

	@Test
	void sendPaymentStatusNotification_publishesNormalizedPayload() {
		producer.sendPaymentStatusNotification("10", "user@example.com", "order_123", 299.0, "success");

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
		verify(rabbitTemplate).convertAndSend(org.mockito.Mockito.eq("payment-notification-queue"), payloadCaptor.capture());

		Map<String, Object> payload = payloadCaptor.getValue();
		assertEquals("SUCCESS", payload.get("status"));
		assertEquals("PAYMENT_SUCCESS", payload.get("type"));
		assertEquals("10", payload.get("userId"));
	}

	@Test
	void sendPaymentStatusNotification_nullStatusDefaultsToPending() {
		producer.sendPaymentStatusNotification("10", "user@example.com", "order_123", 299.0, null);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
		verify(rabbitTemplate).convertAndSend(org.mockito.Mockito.eq("payment-notification-queue"), payloadCaptor.capture());
		assertEquals("PENDING", payloadCaptor.getValue().get("status"));
	}
}
