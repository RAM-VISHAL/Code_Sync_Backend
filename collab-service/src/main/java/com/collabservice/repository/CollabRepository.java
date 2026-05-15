package com.collabservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.collabservice.entity.CollabSession;

import java.util.List;

@Repository
public interface CollabRepository extends JpaRepository<CollabSession, String> {
	List<CollabSession> findByProjectId(int projectId);

	List<CollabSession> findByFileIdAndStatusOrderByCreatedAtDesc(int fileId, String status);

	List<CollabSession> findByOwnerId(int ownerId);
}
