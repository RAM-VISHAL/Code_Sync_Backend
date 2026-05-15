package com.notificationservice.service;

import com.notificationservice.entity.Notification;
import java.util.List;

public interface NotificationService {
	Notification sendProjectNotification(Notification notification, List<String> memberEmails);

	List<Notification> getUserNotifications(Integer userId);

	void markAsRead(Long notificationId);

	void markAllAsRead(Integer userId);

	void deleteNotification(Long id);

	void clearAllNotifications(Integer userId);
}