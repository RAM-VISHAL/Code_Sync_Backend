package com.fileservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "folders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Folder {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long folderId;

	private String name;
	private int projectId;

	private Long parentFolderId;

	// Added to resolve "method setDeleted(boolean) is undefined" error
	private boolean isDeleted = false;

	private LocalDateTime createdAt = LocalDateTime.now();
}