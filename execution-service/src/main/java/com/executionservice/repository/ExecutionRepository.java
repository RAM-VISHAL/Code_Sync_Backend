package com.executionservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.executionservice.entity.ExecutionJob;

import java.util.List;

public interface ExecutionRepository extends JpaRepository<ExecutionJob, String> {
	List<ExecutionJob> findByUserId(int userId);

	List<ExecutionJob> findByProjectId(int projectId);

	List<ExecutionJob> findByStatus(String status);
}