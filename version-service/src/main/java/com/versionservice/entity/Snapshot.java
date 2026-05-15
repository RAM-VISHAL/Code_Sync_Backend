package com.versionservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Snapshot Entity - A point-in-time capture of a file's state. Uses SHA-256
 * hashing for integrity and a parent pointer for history tracking.
 */
@Entity
@Table(name = "snapshots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Snapshot {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private Integer fileId;
	private Integer userId;
	private String username;

	@Column(columnDefinition = "LONGTEXT")
	private String content;

	private String commitMessage;

	// Cryptographic fingerprint for Requirement 2.7 (Data Integrity)
	private String hash;

	// Pointer to the previous version to form the history chain
	private Long parentSnapshotId;

	private LocalDateTime createdAt = LocalDateTime.now();
}