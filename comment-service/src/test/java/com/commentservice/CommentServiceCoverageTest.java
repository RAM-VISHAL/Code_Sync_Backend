package com.commentservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import com.commentservice.entity.Comment;
import com.commentservice.exception.GlobalExceptionHandler;
import com.commentservice.resource.CommentResource;
import com.commentservice.service.CommentService;

class CommentServiceCoverageTest {

	@Test
	void commentResourceDelegatesToService() {
		CommentService commentService = org.mockito.Mockito.mock(CommentService.class);
		CommentResource resource = new CommentResource();
		Comment comment = Comment.builder().id(1L).fileId(10).content("hi").build();

		ReflectionTestUtils.setField(resource, "commentService", commentService);
		when(commentService.addComment(comment)).thenReturn(comment);
		when(commentService.getCommentsByFile(10)).thenReturn(List.of(comment));
		when(commentService.updateComment(1L, "edited")).thenReturn(comment);

		assertEquals(comment, resource.addComment(comment).getBody());
		assertEquals(List.of(comment), resource.getFileReviewThreads(10).getBody());
		assertEquals(comment, resource.editComment(1L, "edited").getBody());
		assertEquals(HttpStatus.NO_CONTENT, resource.removeComment(1L).getStatusCode());

		verify(commentService).deleteComment(1L);
	}

	@Test
	void globalExceptionHandlerBuildsExpectedPayloads() {
		GlobalExceptionHandler handler = new GlobalExceptionHandler();

		Map<String, String> runtimeBody = handler.handleRuntimeException(new RuntimeException("bad request")).getBody();
		Map<String, String> generalBody = handler.handleGeneralException(new Exception("boom")).getBody();

		assertEquals("bad request", runtimeBody.get("message"));
		assertEquals("error", runtimeBody.get("status"));
		assertEquals("Could not process code review. Please try again.", generalBody.get("message"));
		assertEquals("error", generalBody.get("status"));
	}

	@Test
	void applicationMainDelegatesToSpringApplication() {
		try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
			CommentServiceApplication.main(new String[] { "test" });
			springApplication.verify(() -> SpringApplication.run(CommentServiceApplication.class, new String[] { "test" }));
		}
	}
}
