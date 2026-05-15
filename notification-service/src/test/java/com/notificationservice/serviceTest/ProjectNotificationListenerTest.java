package com.notificationservice.serviceTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.notificationservice.entity.Notification;
import com.notificationservice.service.NotificationService;
import com.notificationservice.service.ProjectNotificationListener;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjectNotificationListenerTest {

	@Mock
	private NotificationService notificationService;

	@InjectMocks
	private ProjectNotificationListener listener;

	@Test
	void handleProjectNotification_forwardsNotificationAndEmail() {
		Map<String, Object> payload = Map.of("recipientId", 11, "senderId", 7, "senderName", "owner",
				"recipientEmail", "user@example.com", "relatedId", 55L, "type", "PROJECT_FORKED", "message",
				"forked your project", "projectName", "Demo", "recipientName", "collab");

		listener.handleProjectNotification(payload);

		ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<String>> emailsCaptor = ArgumentCaptor.forClass(List.class);

		verify(notificationService).sendProjectNotification(notificationCaptor.capture(), emailsCaptor.capture());
		Notification notification = notificationCaptor.getValue();
		assertEquals(11, notification.getRecipientId());
		assertEquals(7, notification.getSenderId());
		assertEquals("PROJECT_FORKED", notification.getType());
		assertEquals(55L, notification.getRelatedId());
		assertEquals(List.of("user@example.com"), emailsCaptor.getValue());
	}

	@Test
	void handleProjectNotification_invalidRecipientSkipsDispatch() {
		listener.handleProjectNotification(Map.of("recipientId", "bad-id", "message", "ignored"));

		verify(notificationService, never()).sendProjectNotification(any(), any());
	}
}
