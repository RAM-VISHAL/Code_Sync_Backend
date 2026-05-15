package com.commentservice.resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.commentservice.entity.Comment;
import com.commentservice.service.CommentService;
import java.util.List;

@RestController
@RequestMapping("/api/v1/comments")
public class CommentResource {

	@Autowired
	private CommentService commentService;

	@PostMapping("/add")
	public ResponseEntity<Comment> addComment(@RequestBody Comment comment) {
		return ResponseEntity.ok(commentService.addComment(comment));
	}

	@GetMapping("/file/{fileId}")
	public ResponseEntity<List<Comment>> getFileReviewThreads(@PathVariable Integer fileId) {
		// Fetches all inline comments to be rendered in the Monaco Editor gutters
		return ResponseEntity.ok(commentService.getCommentsByFile(fileId));
	}

	@PutMapping("/{id}")
	public ResponseEntity<Comment> editComment(@PathVariable Long id, @RequestBody String content) {
		return ResponseEntity.ok(commentService.updateComment(id, content));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> removeComment(@PathVariable Long id) {
		commentService.deleteComment(id);
		return ResponseEntity.noContent().build();
	}
}