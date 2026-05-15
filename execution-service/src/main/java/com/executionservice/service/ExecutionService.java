package com.executionservice.service;

import java.util.List;
import java.util.Optional;

import com.executionservice.entity.ExecutionJob;

public interface ExecutionService {
	ExecutionJob submitExecution(ExecutionJob job);

	Optional<ExecutionJob> getJobById(String jobId);

	List<ExecutionJob> getExecutionsByUser(int userId);

	List<ExecutionJob> getExecutionsByProject(int projectId);

	void cancelExecution(String jobId);
}