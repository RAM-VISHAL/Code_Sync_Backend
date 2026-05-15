package com.executionservice.serviceTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;

import com.executionservice.config.RabbitMQConfig;
import com.executionservice.entity.ExecutionJob;
import com.executionservice.repository.ExecutionRepository;
import com.executionservice.serviceImpl.ExecutionServiceImpl;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.PingCmd;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.Optional;
import java.util.UUID;

/**
 * Unit Tests for Execution-Service Business Logic. Verifies the Job Submission
 * workflow and RabbitMQ dispatching.
 */
@ExtendWith(MockitoExtension.class)
public class ExecutionServiceImplTest {

	@Mock
	private ExecutionRepository repository;

	@Mock
	private RabbitTemplate rabbitTemplate;

	@Mock
	private DockerClient dockerClient;

	@Mock
	private PingCmd pingCmd;

	@InjectMocks
	private ExecutionServiceImpl executionService;

	private ExecutionJob testJob;

	@BeforeEach
	void setUp() {
		lenient().when(dockerClient.pingCmd()).thenReturn(pingCmd);
		lenient().doNothing().when(pingCmd).exec();

		// Initialize a standard Python execution job for testing
		testJob = new ExecutionJob();
		testJob.setSourceCode("print('Hello World')");
		testJob.setLanguage("python");
		testJob.setUserId(101);
		testJob.setProjectId(500);
	}

	@Test
	void testSubmitExecution_ShouldEnqueueJob() {
		// Arrange: Capture the job passed to save to verify its modifications
		when(repository.save(any(ExecutionJob.class))).thenAnswer(invocation -> invocation.getArgument(0));

		// Act: Submit the job for execution
		ExecutionJob result = executionService.submitExecution(testJob);

		// Assert: Verify internal state updates
		assertNotNull(result.getJobId(), "A unique Job ID must be generated upon submission");
		assertEquals("QUEUED", result.getStatus(), "Initial status must be QUEUED before the worker picks it up");

		// Verify database persistence
		verify(repository, times(1)).save(any(ExecutionJob.class));

		// Verify RabbitMQ Dispatch: The heart of the async execution engine
		verify(rabbitTemplate, times(1)).convertAndSend(eq(RabbitMQConfig.EXECUTION_EXCHANGE), eq("execution.run"),
				eq(result.getJobId()));
	}

	@Test
	void testGetJobById_Found() {
		// Arrange
		String id = UUID.randomUUID().toString();
		testJob.setJobId(id);
		when(repository.findById(id)).thenReturn(Optional.of(testJob));

		// Act
		Optional<ExecutionJob> foundJob = executionService.getJobById(id);

		// Assert
		assertTrue(foundJob.isPresent());
		assertEquals(id, foundJob.get().getJobId(), "Should retrieve the correct job metadata from DB");
	}

	@Test
	void testCancelExecution_ShouldUpdateStatus() {
		// Arrange
		String id = "job-123";
		testJob.setJobId(id);
		testJob.setStatus("QUEUED");

		when(repository.findById(id)).thenReturn(Optional.of(testJob));
		when(repository.save(any(ExecutionJob.class))).thenReturn(testJob);

		// Act: Cancel a job that hasn't started yet
		executionService.cancelExecution(id);

		// Assert
		assertEquals("CANCELLED", testJob.getStatus(), "Status must reflect cancellation in the DB");
		verify(repository).save(argThat(job -> "CANCELLED".equals(job.getStatus())));
	}
}
