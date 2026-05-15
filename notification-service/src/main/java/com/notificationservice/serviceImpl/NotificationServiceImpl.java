package com.notificationservice.serviceImpl;

import com.notificationservice.entity.Notification;
import com.notificationservice.repository.NotificationRepository;
import com.notificationservice.service.EmailService;
import com.notificationservice.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {

	@Autowired
	private NotificationRepository repository;

	@Autowired
	private SimpMessagingTemplate messagingTemplate;

	@Autowired
	private EmailService emailService;

	@Override
	@Transactional
	public Notification sendProjectNotification(Notification notification, List<String> memberEmails) {
		// 1. Save for persistent history (User's notification center)
		Notification saved = repository.save(notification);

		// 2. Real-time push via a user-id based topic channel
		messagingTemplate.convertAndSend("/topic/notifications/" + notification.getRecipientId(), saved);

		// 3. Asynchronous Email Dispatch
		if (memberEmails != null && !memberEmails.isEmpty()) {
			String msg = notification.getMessage().toLowerCase();
			boolean isApproved = msg.contains("approved") || msg.contains("granted");

			String requesterName = (notification.getRecipientName() != null) ? notification.getRecipientName() : 
                                   (notification.getSenderName() != null ? notification.getSenderName() : "Developer");
			String projectName = (notification.getProjectName() != null) ? notification.getProjectName() : "CodeSync Project";

			emailService.sendHtmlEmail(memberEmails, requesterName, projectName, isApproved);
		}

		return saved;
	}

	@Override
	public List<Notification> getUserNotifications(Integer userId) {
		return repository.findByRecipientIdOrderByCreatedAtDesc(userId);
	}

	@Override
	@Transactional
	public void markAsRead(Long notificationId) {
		repository.findById(notificationId).ifPresent(n -> {
			n.setRead(true);
			repository.save(n);
		});
	}

	@Override
	public void clearAllNotifications(Integer userId) {
		List<Notification> all = repository.findByRecipientId(userId);
		repository.deleteAll(all);
	}

	// Standard CRUD implementations cleaned for performance
	@Override
	public void markAllAsRead(Integer userId) {
		repository.findByRecipientId(userId).forEach(n -> {
			n.setRead(true);
			repository.save(n);
		});
	}

	@Override
	public void deleteNotification(Long id) {
		repository.deleteById(id);
	}
}
