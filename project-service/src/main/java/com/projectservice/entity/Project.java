package com.projectservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Project Entity - Metadata container for code projects. Tracks language,
 * visibility, and social metrics (stars/forks).
 */
@Entity
@Table(name = "projects")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Project {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int projectId;

	private int ownerId; // Link to Auth-Service User ID
	private String ownerUsername; // New field for frontend displaying owner profile
	private String name;
	private String description;
	private String language; // Java, Python, etc.
	private String visibility; // PUBLIC or PRIVATE
	private int templateId;

	private boolean isArchived = false;
	private LocalDateTime createdAt = LocalDateTime.now();
	private LocalDateTime updatedAt = LocalDateTime.now();

	@Transient
	@JsonProperty("isStarredByMe")
	private boolean isStarredByMe; // Calculated at runtime based on current user session

	private int starCount = 0;
	private int forkCount = 0;

	@PreUpdate
	protected void onUpdate() {
		this.updatedAt = LocalDateTime.now();
	}
}