package com.notificationservice.serviceTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.notificationservice.entity.Notification;
import com.notificationservice.repository.NotificationRepository;
import com.notificationservice.service.PaymentNotificationListener;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@ExtendWith(MockitoExtension.class)
class PaymentNotificationListenerTest {

	@Mock
	private JavaMailSender mailSender;

	@Mock
	private NotificationRepository notificationRepository;

	@Mock
	private SimpMessagingTemplate messagingTemplate;

	@InjectMocks
	private PaymentNotificationListener listener;

	@Test
	void handlePaymentNotification_successSavesPushesAndEmails() {
		when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

		listener.handlePaymentNotification(Map.of("userId", "12", "email", "payee@example.com", "status", "SUCCESS",
				"amount", 499.0, "orderId", "order_123"));

		ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
		verify(notificationRepository).save(notificationCaptor.capture());
		Notification saved = notificationCaptor.getValue();
		assertEquals(12, saved.getRecipientId());
		assertEquals("PAYMENT_SUCCESS", saved.getType());
		verify(messagingTemplate).convertAndSend("/topic/notifications/12", saved);
		verify(mailSender).send(any(org.springframework.mail.SimpleMailMessage.class));
	}

	@Test
	void handlePaymentNotification_withoutRecipientAndEmailSkipsSideEffects() {
		listener.handlePaymentNotification(Map.of("status", "PENDING", "amount", 99.0, "orderId", "order_456"));

		verify(notificationRepository, never()).save(any(Notification.class));
		verify(messagingTemplate, never()).convertAndSend(any(String.class), any(Object.class));
		verify(mailSender, never()).send(any(org.springframework.mail.SimpleMailMessage.class));
	}
}
