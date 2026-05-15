package com.commentservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.commentservice.entity.Comment;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
	// Fetch comments for a specific file ordered by time
	List<Comment> findByFileIdOrderByCreatedAtAsc(Integer fileId);
}