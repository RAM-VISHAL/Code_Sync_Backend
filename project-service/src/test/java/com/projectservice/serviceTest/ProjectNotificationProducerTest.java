package com.projectservice.serviceTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

import com.projectservice.config.ProjectNotificationProducer;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@ExtendWith(MockitoExtension.class)
class ProjectNotificationProducerTest {

	@Mock
	private RabbitTemplate rabbitTemplate;

	@InjectMocks
	private ProjectNotificationProducer producer;

	@Test
	void send_publishesExpectedProjectNotificationPayload() {
		producer.send(10, 1, "owner", "dev@example.com", 88L, "PROJECT_FORKED", "forked your project", "Demo",
				"recipient");

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
		verify(rabbitTemplate).convertAndSend(org.mockito.Mockito.eq("project-notification-queue"),
				payloadCaptor.capture());

		Map<String, Object> payload = payloadCaptor.getValue();
		assertEquals(10, payload.get("recipientId"));
		assertEquals(1, payload.get("senderId"));
		assertEquals("PROJECT_FORKED", payload.get("type"));
		assertEquals("Demo", payload.get("projectName"));
	}
}
