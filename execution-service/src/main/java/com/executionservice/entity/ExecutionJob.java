package com.executionservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * ExecutionJob Entity - Tracks a sandboxed code run. Captures output (stdout),
 * errors (stderr), and resource usage.
 */
@Entity
@Table(name = "execution_jobs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExecutionJob {
	@Id
	private String jobId; // UUID generated in ServiceImpl
	private int projectId;
	private int fileId;
	private int userId;
	private String language;

	@Column(columnDefinition = "LONGTEXT")
	private String sourceCode;

	@Column(columnDefinition = "TEXT")
	private String stdin;

	private String status; // QUEUED, RUNNING, COMPLETED, FAILED, TIMED_OUT

	@Column(columnDefinition = "LONGTEXT")
	private String stdout;

	@Column(columnDefinition = "LONGTEXT")
	private String stderr;

	private int exitCode;
	private long executionTimeMs;
	private long memoryUsedKb;

	private LocalDateTime createdAt = LocalDateTime.now();
	private LocalDateTime completedAt;
}