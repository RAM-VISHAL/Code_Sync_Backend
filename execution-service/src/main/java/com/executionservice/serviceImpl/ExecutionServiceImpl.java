package com.executionservice.serviceImpl;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.executionservice.config.RabbitMQConfig;
import com.executionservice.entity.ExecutionJob;
import com.executionservice.repository.ExecutionRepository;
import com.executionservice.service.ExecutionService;
import com.github.dockerjava.api.DockerClient;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class ExecutionServiceImpl implements ExecutionService {

	@Autowired
	private ExecutionRepository repository;

	@Autowired
	private RabbitTemplate rabbitTemplate;

	@Autowired
	private DockerClient dockerClient;

	@Override
	public ExecutionJob submitExecution(ExecutionJob job) {
		job.setJobId(UUID.randomUUID().toString());
		job.setStatus("QUEUED");
		ExecutionJob savedJob = repository.save(job);

		if (!isDockerAvailable()) {
			savedJob.setStatus("FAILED");
			savedJob.setStderr("Docker is not accessible for execution-service. Start Docker Desktop and ensure this user can access the Docker engine.");
			return repository.save(savedJob);
		}

		try {
			// Push to RabbitMQ for worker to process
			rabbitTemplate.convertAndSend(RabbitMQConfig.EXECUTION_EXCHANGE, RabbitMQConfig.ROUTING_KEY, savedJob.getJobId());
			return savedJob;
		} catch (Exception ex) {
			savedJob.setStatus("FAILED");
			savedJob.setStderr("Failed to queue execution job: " + ex.getMessage());
			return repository.save(savedJob);
		}
	}

	@Override
	public Optional<ExecutionJob> getJobById(String jobId) {
		return repository.findById(jobId);
	}

	@Override
	public List<ExecutionJob> getExecutionsByUser(int userId) {
		return repository.findByUserId(userId);
	}

	@Override
	public List<ExecutionJob> getExecutionsByProject(int projectId) {
		return repository.findByProjectId(projectId);
	}

	@Override
	public void cancelExecution(String jobId) {
		repository.findById(jobId).ifPresent(job -> {
			job.setStatus("CANCELLED");
			repository.save(job);
		});
	}

	private boolean isDockerAvailable() {
		try {
			CompletableFuture.runAsync(() -> dockerClient.pingCmd().exec()).orTimeout(2, TimeUnit.SECONDS).join();
			return true;
		} catch (Exception ignored) {
			return false;
		}
	}
}
