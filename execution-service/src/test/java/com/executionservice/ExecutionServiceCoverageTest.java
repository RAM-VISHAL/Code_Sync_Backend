package com.executionservice;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;
import org.springframework.test.util.ReflectionTestUtils;

import com.executionservice.entity.ExecutionJob;
import com.executionservice.exception.GlobalExceptionHandler;
import com.executionservice.listener.ExecutionJobListener;
import com.executionservice.resource.ExecutionResource;
import com.executionservice.service.ExecutionService;

class ExecutionServiceCoverageTest {

	@Test
	void executionResourceDelegatesToService() {
		ExecutionService executionService = org.mockito.Mockito.mock(ExecutionService.class);
		ExecutionResource resource = new ExecutionResource();
		ExecutionJob job = new ExecutionJob();
		job.setJobId("job-1");

		ReflectionTestUtils.setField(resource, "execService", executionService);
		when(executionService.submitExecution(job)).thenReturn(job);
		when(executionService.getJobById("job-1")).thenReturn(Optional.of(job));
		when(executionService.getJobById("missing")).thenReturn(Optional.empty());
		when(executionService.getExecutionsByUser(5)).thenReturn(List.of(job));

		assertEquals(job, resource.submitJob(job).getBody());
		assertEquals(job, resource.getJobStatus("job-1").getBody());
		assertEquals(404, resource.getJobStatus("missing").getStatusCode().value());
		assertEquals(List.of(job), resource.getUserHistory(5).getBody());
	}

	@Test
	void executionListenerHelperMethodsCoverLanguageBranches() {
		ExecutionJobListener listener = new ExecutionJobListener();

		assertEquals("amazoncorretto:17-alpine", ReflectionTestUtils.invokeMethod(listener, "getDockerImage", "java"));
		assertEquals("node:alpine", ReflectionTestUtils.invokeMethod(listener, "getDockerImage", "javascript"));
		assertEquals("python:3.9-slim", ReflectionTestUtils.invokeMethod(listener, "getDockerImage", "python"));
		assertEquals("gcc:latest", ReflectionTestUtils.invokeMethod(listener, "getDockerImage", "cpp"));
		assertEquals("alpine:latest", ReflectionTestUtils.invokeMethod(listener, "getDockerImage", "go"));
		assertEquals("Main", ReflectionTestUtils.invokeMethod(listener, "extractJavaClassName", "class Hello {}"));
		assertEquals("Demo",
				ReflectionTestUtils.invokeMethod(listener, "extractJavaClassName", "public class Demo { public static void main(String[] a){} }"));
		assertArrayEquals(new String[] { "node", "-e", "console.log(1)" },
				(String[]) ReflectionTestUtils.invokeMethod(listener, "getExecutionCommand", "javascript", "console.log(1)"));
		assertTrue(((String[]) ReflectionTestUtils.invokeMethod(listener, "getExecutionCommand", "java",
				"public class Demo { public static void main(String[] args){} }"))[2].contains("Demo.java"));
		assertTrue(((String[]) ReflectionTestUtils.invokeMethod(listener, "getExecutionCommand", "c", "printf('x');"))[2]
				.contains("main.c"));
		assertTrue(((String[]) ReflectionTestUtils.invokeMethod(listener, "getExecutionCommand", "unknown", "noop"))[1]
				.contains("Language not supported"));
	}

	@Test
	void globalExceptionHandlerBuildsExpectedPayloads() {
		GlobalExceptionHandler handler = new GlobalExceptionHandler();
		assertEquals("boom", handler.handleRuntimeException(new RuntimeException("boom")).getBody().get("message"));
		assertEquals("x", handler.handleGeneralException(new Exception("x")).getBody().get("message"));
	}

	@Test
	void applicationMainDelegatesToSpringApplication() {
		try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
			ExecutionServiceApplication.main(new String[] { "test" });
			springApplication.verify(() -> SpringApplication.run(ExecutionServiceApplication.class, new String[] { "test" }));
		}
	}
}
