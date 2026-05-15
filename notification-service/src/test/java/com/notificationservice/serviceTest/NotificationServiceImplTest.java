package com.notificationservice.serviceTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import com.notificationservice.entity.Notification;
import com.notificationservice.repository.NotificationRepository;
import com.notificationservice.service.EmailService;
import com.notificationservice.serviceImpl.NotificationServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Arrays;
import java.util.List;

/**
 * Unit Tests for Notification-Service. Focuses on the orchestration of
 * persistent history, real-time WebSockets, and Async Emails.
 */
@ExtendWith(MockitoExtension.class)
public class NotificationServiceImplTest {

	@Mock
	private NotificationRepository repository;

	@Mock
	private SimpMessagingTemplate messagingTemplate;

	@Mock
	private EmailService emailService;

	@InjectMocks
	private NotificationServiceImpl notificationService;

	private Notification testNotification;

	@BeforeEach
	void setUp() {
		// Sample notification: A collaborator has been approved for a project
		testNotification = Notification.builder().recipientId(101).senderName("Abhay")
				.message("Hey, your request to join CodeSync has been Approved.").type("COLLAB_REQUEST").build();
	}

	@Test
	void testSendProjectNotification_FullOmnichannelFlow() {
		// Arrange: Mock the persistence layer
		List<String> emails = Arrays.asList("member1@gmail.com", "member2@gmail.com");
		when(repository.save(any(Notification.class))).thenReturn(testNotification);

		// Act: Trigger the notification flow
		Notification result = notificationService.sendProjectNotification(testNotification, emails);

		// Assert: Verify data integrity
		assertNotNull(result);
		assertEquals("COLLAB_REQUEST", result.getType());

		// 1. Verify Database Save (For the user's notification center history)
		verify(repository, times(1)).save(testNotification);

		// 2. Verify WebSocket Push (For real-time UI "ping")
		verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/notifications/101"), eq(testNotification));

		// 3. Verify Email Dispatch (For offline alerts)
		// Requirement: Use the professional HTML template logic
		verify(emailService, times(1)).sendHtmlEmail(eq(emails), anyString(), // Extracted Username
				anyString(), // Extracted Project Name
				eq(true) // isApproved should be true based on the message string
		);
	}

	@Test
	void testSendProjectNotification_SkipEmailWhenNull() {
		// Arrange
		when(repository.save(any(Notification.class))).thenReturn(testNotification);

		// Act: Pass null for emails list
		notificationService.sendProjectNotification(testNotification, null);

		// Assert: DB and WebSockets should still fire, but Email should be skipped
		verify(repository).save(any());
		verify(messagingTemplate).convertAndSend(anyString(), any(Object.class));
		verify(emailService, never()).sendHtmlEmail(any(), anyString(), anyString(), anyBoolean());
	}

	@Test
	void testMarkAsRead_ShouldUpdateStatus() {
		// Arrange
		when(repository.findById(1L)).thenReturn(java.util.Optional.of(testNotification));

		// Act
		notificationService.markAsRead(1L);

		// Assert
		assertTrue(testNotification.isRead(), "Notification must be marked as read in the DB");
		verify(repository).save(testNotification);
	}
}
