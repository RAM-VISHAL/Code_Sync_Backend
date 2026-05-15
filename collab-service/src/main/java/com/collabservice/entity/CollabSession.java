package com.collabservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * CollabSession Entity - Represents a live "Room" for co-editing. Each session
 * is bound to a specific file within a project.
 */
@Entity
@Table(name = "collab_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CollabSession {
	@Id
	@Column(length = 36) // Standard UUID length for portability
	private String sessionId;

	private int projectId;
	private int fileId; // The specific file currently being co-edited
	private int ownerId;

	@Column(length = 20)
	private String status = "ACTIVE"; // ACTIVE, ENDED

	private String language;
	private boolean isPasswordProtected;
	private String sessionPassword;
	private int maxParticipants = 5; // Default limit per project tier

	private LocalDateTime createdAt = LocalDateTime.now();
	private LocalDateTime endedAt;
}