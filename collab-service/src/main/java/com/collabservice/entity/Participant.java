package com.collabservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Participant Entity - Tracks a user inside a live session. Stores cursor
 * position to synchronize views across all collaborators.
 */
@Entity
@Table(name = "participants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Participant {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long participantId;

	@Column(length = 36)
	private String sessionId;

	private int userId;
	private String username;

	@Column(length = 20)
	private String role; // HOST, EDITOR, VIEWER

	@Column(length = 7)
	private String color; // Unique hex color for user's cursor

	private int cursorLine = 0;
	private int cursorCol = 0;

	private LocalDateTime joinedAt = LocalDateTime.now();
}