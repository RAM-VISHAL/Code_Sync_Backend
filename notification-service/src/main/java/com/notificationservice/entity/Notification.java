package com.notificationservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Notification Entity - Represents a system or user-generated alert. Supports
 * both persistent history and real-time dispatch.
 */
@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private Integer recipientId; // Target User ID
	private Integer senderId; // Initiating User ID
	private String senderName;
	private String senderEmail;

	private Long relatedId; // Link to Project ID or File ID
	private String message;
	private String type; // COLLAB_REQUEST, ACCESS_GRANTED, COMMENT_ADDED

	@Builder.Default
	private boolean isRead = false;
	private LocalDateTime createdAt;

	@Transient
	private String projectName;
	
	@Transient
	private String recipientName;

	@PrePersist
	protected void onCreate() {
		this.createdAt = LocalDateTime.now();
	}
}
