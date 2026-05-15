package com.commentservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Comment Entity - Represents an inline code review annotation. Supports
 * line-specific feedback and threaded replies.
 */
@Entity
@Table(name = "user_comments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comment {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private Integer fileId;
	private Integer userId;
	private String username;

	@Column(columnDefinition = "TEXT", nullable = false)
	private String content;

	// Critical for Requirement R2.10: Attach feedback to specific code lines
	private Integer lineNumber;

	// Supports nested discussions (null if it's a top-level comment)
	private Long parentCommentId;

	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;

	@PrePersist
	protected void onCreate() {
		this.createdAt = LocalDateTime.now();
		this.updatedAt = LocalDateTime.now();
	}

	@PreUpdate
	protected void onUpdate() {
		this.updatedAt = LocalDateTime.now();
	}
}