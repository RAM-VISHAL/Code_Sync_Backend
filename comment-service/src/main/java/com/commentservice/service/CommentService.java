package com.commentservice.service;

import java.util.List;

import com.commentservice.entity.Comment;

public interface CommentService {
	Comment addComment(Comment comment);

	List<Comment> getCommentsByFile(Integer fileId);

	void deleteComment(Long commentId);

	Comment updateComment(Long id, String content);
}