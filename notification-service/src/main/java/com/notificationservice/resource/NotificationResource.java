package com.notificationservice.resource;

import com.notificationservice.entity.Notification;
import com.notificationservice.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationResource {

	@Autowired
	private NotificationService notificationService;

	@PostMapping("/send")
	public ResponseEntity<Notification> dispatchNotification(@RequestBody Notification notification,
			@RequestParam(required = false) List<String> emails) {
		return ResponseEntity.ok(notificationService.sendProjectNotification(notification, emails));
	}

	@GetMapping("/user/{userId}")
	public ResponseEntity<List<Notification>> getNotifications(@PathVariable Integer userId) {
		return ResponseEntity.ok(notificationService.getUserNotifications(userId));
	}

	@PatchMapping("/read/{id}")
	public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
		notificationService.markAsRead(id);
		return ResponseEntity.noContent().build();
	}

	@PatchMapping("/read-all/{userId}")
	public ResponseEntity<Void> markAllAsRead(@PathVariable Integer userId) {
		notificationService.markAllAsRead(userId);
		return ResponseEntity.noContent().build();
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteNotification(@PathVariable Long id) {
		notificationService.deleteNotification(id);
		return ResponseEntity.noContent().build();
	}

	@DeleteMapping("/user/{userId}")
	public ResponseEntity<Void> clearHistory(@PathVariable Integer userId) {
		notificationService.clearAllNotifications(userId);
		return ResponseEntity.noContent().build();
	}
}