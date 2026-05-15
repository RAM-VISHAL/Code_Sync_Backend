package com.notificationservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;
import org.springframework.test.util.ReflectionTestUtils;

import com.notificationservice.entity.Notification;
import com.notificationservice.exception.GlobalExceptionHandler;
import com.notificationservice.resource.NotificationResource;
import com.notificationservice.service.NotificationService;

class NotificationServiceCoverageTest {

	@Test
	void notificationResourceDelegatesToService() {
		NotificationService notificationService = org.mockito.Mockito.mock(NotificationService.class);
		NotificationResource resource = new NotificationResource();
		Notification notification = Notification.builder().id(1L).message("Hello").build();

		ReflectionTestUtils.setField(resource, "notificationService", notificationService);
		when(notificationService.sendProjectNotification(notification, List.of("a@example.com"))).thenReturn(notification);
		when(notificationService.getUserNotifications(7)).thenReturn(List.of(notification));

		assertEquals(notification, resource.dispatchNotification(notification, List.of("a@example.com")).getBody());
		assertEquals(List.of(notification), resource.getNotifications(7).getBody());
		assertEquals(204, resource.markAsRead(1L).getStatusCode().value());
		assertEquals(204, resource.markAllAsRead(7).getStatusCode().value());
		assertEquals(204, resource.deleteNotification(1L).getStatusCode().value());
		assertEquals(204, resource.clearHistory(7).getStatusCode().value());

		verify(notificationService).markAsRead(1L);
		verify(notificationService).markAllAsRead(7);
		verify(notificationService).deleteNotification(1L);
		verify(notificationService).clearAllNotifications(7);
	}

	@Test
	void globalExceptionHandlerBuildsExpectedPayloads() {
		GlobalExceptionHandler handler = new GlobalExceptionHandler();
		assertEquals("boom", handler.handleRuntimeException(new RuntimeException("boom")).getBody().get("message"));
		assertEquals("Real-time notifications are temporarily unavailable. Retrying...",
				handler.handleGeneralException(new Exception("x")).getBody().get("message"));
	}

	@Test
	void applicationMainDelegatesToSpringApplication() {
		try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
			NotificationServiceApplication.main(new String[] { "test" });
			springApplication.verify(() -> SpringApplication.run(NotificationServiceApplication.class, new String[] { "test" }));
		}
	}
}
