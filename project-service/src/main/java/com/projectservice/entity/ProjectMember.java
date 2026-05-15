package com.projectservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "project_members")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProjectMember {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private int projectId;
	private int userId;
	private String username;

	@Column(length = 20)
	private String role; // PENDING (Requesting access), EDITOR (Full Access)

	@Column(length = 20)
	private String status = "PENDING";

	private boolean canEdit = false;

	private Integer grantedBy;

	private LocalDateTime joinedAt = LocalDateTime.now();
	private LocalDateTime updatedAt = LocalDateTime.now();

	@PrePersist
	@PreUpdate
	private void syncLegacyRole() {
		boolean hasLegacyOnlyRole = status == null || status.isBlank();
		if (hasLegacyOnlyRole) {
			status = "EDITOR".equalsIgnoreCase(role) ? "APPROVED" : "PENDING";
			canEdit = "EDITOR".equalsIgnoreCase(role);
		}
		role = "APPROVED".equalsIgnoreCase(status) && canEdit ? "EDITOR" : status;
		updatedAt = LocalDateTime.now();
	}
}
