package com.commentservice.serviceTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import com.commentservice.entity.Comment;
import com.commentservice.repository.CommentRepository;
import com.commentservice.serviceImpl.CommentServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Unit Tests for Comment-Service. Focuses on inline code review validation and
 * review thread retrieval.
 */
@ExtendWith(MockitoExtension.class)
public class CommentServiceImplTest {

	@Mock
	private CommentRepository repository;

	@InjectMocks
	private CommentServiceImpl commentService;

	private Comment testComment;

	@BeforeEach
	void setUp() {
		// Standardized test comment for a specific line in the editor
		testComment = Comment.builder().id(1L).fileId(101).userId(1).username("Abhay")
				.content("Please refactor this method for better readability.").lineNumber(15) // Requirement R2.10
				.build();
	}

	@Test
	void testAddComment_Success() {
		when(repository.save(any(Comment.class))).thenReturn(testComment);

		Comment savedComment = commentService.addComment(testComment);

		assertNotNull(savedComment);
		assertEquals("Abhay", savedComment.getUsername());
		verify(repository, times(1)).save(any(Comment.class));
	}

	@Test
	void testAddComment_MissingFileId_ShouldThrowException() {
		// Arrange: Create a comment without a file link
		Comment invalidComment = new Comment();
		invalidComment.setContent("Orphan comment");

		// Act & Assert: Should trigger the humanized error we added to ServiceImpl
		RuntimeException ex = assertThrows(RuntimeException.class, () -> {
			commentService.addComment(invalidComment);
		});

		assertTrue(ex.getMessage().contains("File ID is required"));
	}

	@Test
	void testGetCommentsByFile_Success() {
		List<Comment> mockComments = Arrays.asList(testComment);
		when(repository.findByFileIdOrderByCreatedAtAsc(101)).thenReturn(mockComments);

		List<Comment> result = commentService.getCommentsByFile(101);

		assertEquals(1, result.size());
		assertEquals(15, result.get(0).getLineNumber(), "Line number must be preserved in the review thread");
		verify(repository).findByFileIdOrderByCreatedAtAsc(101);
	}

	@Test
	void testUpdateComment_Success() {
		String newContent = "Actually, this looks fine now.";
		when(repository.findById(1L)).thenReturn(Optional.of(testComment));
		when(repository.save(any(Comment.class))).thenAnswer(i -> i.getArguments()[0]);

		Comment updatedComment = commentService.updateComment(1L, newContent);

		assertEquals(newContent, updatedComment.getContent());
		verify(repository).save(any(Comment.class));
	}

	@Test
	void testDeleteComment_Success() {
		// Arrange: Ensure the comment exists before trying to delete
		when(repository.existsById(1L)).thenReturn(true);

		// Act
		commentService.deleteComment(1L);

		// Assert
		verify(repository, times(1)).deleteById(1L);
	}

	@Test
	void testDeleteComment_NotFound_ShouldThrowException() {
		// Arrange
		when(repository.existsById(99L)).thenReturn(false);

		// Act & Assert
		RuntimeException ex = assertThrows(RuntimeException.class, () -> {
			commentService.deleteComment(99L);
		});

		assertTrue(ex.getMessage().contains("Delete Failed"));
	}
}