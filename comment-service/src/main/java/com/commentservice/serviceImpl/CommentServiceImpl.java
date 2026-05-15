package com.commentservice.serviceImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.commentservice.entity.Comment;
import com.commentservice.repository.CommentRepository;
import com.commentservice.service.CommentService;
import java.util.List;

@Service
public class CommentServiceImpl implements CommentService {

	@Autowired
	private CommentRepository repository;

	@Override
	@Transactional
	public Comment addComment(Comment comment) {
		// Business Rule: Ensure inline comments are bound to a valid file
		if (comment.getFileId() == null)
			throw new RuntimeException("Validation Error: File ID is required for reviews.");
		return repository.save(comment);
	}

	@Override
	public List<Comment> getCommentsByFile(Integer fileId) {
		return repository.findByFileIdOrderByCreatedAtAsc(fileId);
	}

	@Override
	@Transactional
	public void deleteComment(Long commentId) {
		if (!repository.existsById(commentId))
			throw new RuntimeException("Delete Failed: Comment not found.");
		repository.deleteById(commentId);
	}

	@Override
	@Transactional
	public Comment updateComment(Long id, String content) {
		Comment existing = repository.findById(id)
				.orElseThrow(() -> new RuntimeException("Update Failed: Review comment not found."));

		existing.setContent(content);
		return repository.save(existing);
	}
}