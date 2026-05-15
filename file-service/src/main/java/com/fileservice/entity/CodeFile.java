package com.fileservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "code_files")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CodeFile {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long fileId;

	private String name;
	private String extension;
	private String path;
	private Long size;
	@Column(columnDefinition = "LONGTEXT")
	private String content;

	private int projectId;
	private Integer folderId; // Changed to Integer for consistency

	private int createdById;
	private int lastEditedBy; // Critical for collaborative attribution

	private boolean isDeleted = false; // Soft-delete flag
	private LocalDateTime updatedAt = LocalDateTime.now();
	private LocalDateTime createdAt = LocalDateTime.now();
}