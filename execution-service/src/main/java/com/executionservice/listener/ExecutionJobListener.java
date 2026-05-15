package com.executionservice.listener;

import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.TimeUnit;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.executionservice.config.RabbitMQConfig;
import com.executionservice.entity.ExecutionJob;
import com.executionservice.repository.ExecutionRepository;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HostConfig;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ExecutionJobListener {

	private static final Pattern PUBLIC_CLASS_PATTERN = Pattern.compile("\\bpublic\\s+class\\s+([A-Za-z_$][A-Za-z\\d_$]*)\\b");

	@Autowired
	private DockerClient dockerClient;

	@Autowired
	private ExecutionRepository repository;

	@RabbitListener(queues = RabbitMQConfig.EXECUTION_QUEUE)
	public void processJob(String jobId) {
		ExecutionJob job = repository.findById(jobId).orElseThrow(() -> new RuntimeException("Job not found in DB"));
		job.setStatus("RUNNING");
		repository.save(job);

		String containerId = null;
		long startTime = System.currentTimeMillis();

		try {
			String image = getDockerImage(job.getLanguage());

			// Security Sandbox Config: No Network, Limited CPU/Memory
			HostConfig hostConfig = HostConfig.newHostConfig().withMemory(256 * 1024 * 1024L) // 256MB RAM Limit
					.withCpuQuota(100000L) // 1 CPU Core Limit
					.withNetworkMode("none"); // Absolute Isolation

			CreateContainerResponse container = dockerClient.createContainerCmd(image).withHostConfig(hostConfig)
					.withCmd(getExecutionCommand(job.getLanguage(), job.getSourceCode())).exec();

			containerId = container.getId();
			dockerClient.startContainerCmd(containerId).exec();

			// Timeout Enforcement: 10 seconds max execution time
			WaitContainerResultCallback waitCallback = new WaitContainerResultCallback();
			dockerClient.waitContainerCmd(containerId).exec(waitCallback);
			boolean finished = waitCallback.awaitCompletion(10, TimeUnit.SECONDS);

			if (!finished) {
				job.setStatus("TIMED_OUT");
				job.setStderr("Execution Error: Code exceeded the 10-second safety limit.");
				dockerClient.stopContainerCmd(containerId).exec();
			} else {
				captureLogs(containerId, job);
				job.setStatus("COMPLETED");
			}

		} catch (Exception e) {
			log.error("Execution failure for job {}: {}", jobId, e.getMessage());
			job.setStatus("FAILED");
			job.setStderr("Sandbox Error: " + e.getMessage());
		} finally {
			cleanup(containerId, job, startTime);
		}
	}

	private void captureLogs(String containerId, ExecutionJob job) throws Exception {
		StringBuilder stdout = new StringBuilder();
		StringBuilder stderr = new StringBuilder();

		dockerClient.logContainerCmd(containerId).withStdOut(true).withStdErr(true).withFollowStream(true)
				.exec(new ResultCallback.Adapter<Frame>() {
					@Override
					public void onNext(Frame frame) {
						if ("STDOUT".equals(frame.getStreamType().name()))
							stdout.append(new String(frame.getPayload()));
						else if ("STDERR".equals(frame.getStreamType().name()))
							stderr.append(new String(frame.getPayload()));
					}
				}).awaitCompletion();

		job.setStdout(stdout.toString());
		job.setStderr(stderr.toString());
	}

	private void cleanup(String containerId, ExecutionJob job, long startTime) {
		if (containerId != null) {
			try {
				dockerClient.removeContainerCmd(containerId).withForce(true).exec();
			} catch (Exception e) {
				log.warn("Resource Leak Warning: Could not remove container {}", containerId);
			}
		}
		job.setExecutionTimeMs(System.currentTimeMillis() - startTime);
		job.setCompletedAt(LocalDateTime.now());
		repository.save(job);
	}

	private String getDockerImage(String language) {
		return switch (language.toLowerCase()) {
		case "java" -> "amazoncorretto:17-alpine";
		case "javascript", "nodejs" -> "node:alpine";
		case "python" -> "python:3.9-slim";
		case "c", "cpp" -> "gcc:latest";
		default -> "alpine:latest";
		};
	}

	private String[] getExecutionCommand(String language, String code) {
		// Escape single quotes to prevent command injection in the shell
		String escapedCode = code.replace("'", "'\\''");
		return switch (language.toLowerCase()) {
		case "javascript", "nodejs" -> new String[] { "node", "-e", code };
		case "java" -> {
			String className = extractJavaClassName(code);
			yield new String[] { "/bin/sh", "-c",
					"echo '" + escapedCode + "' > " + className + ".java && javac " + className + ".java && java " + className };
		}
		case "python" -> new String[] { "python", "-c", code };
		case "c" -> new String[] { "/bin/sh", "-c", "echo '" + escapedCode + "' > main.c && gcc main.c && ./a.out" };
		case "cpp" -> new String[] { "/bin/sh", "-c", "echo '" + escapedCode + "' > main.cpp && g++ main.cpp && ./a.out" };
		default -> new String[] { "echo", "Language not supported" };
		};
	}

	private String extractJavaClassName(String code) {
		Matcher matcher = PUBLIC_CLASS_PATTERN.matcher(code);
		if (matcher.find()) {
			return matcher.group(1);
		}
		return "Main";
	}
}
