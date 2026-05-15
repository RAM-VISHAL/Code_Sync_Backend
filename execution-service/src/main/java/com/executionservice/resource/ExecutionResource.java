package com.executionservice.resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.executionservice.entity.ExecutionJob;
import com.executionservice.service.ExecutionService;
import java.util.List;

@RestController
@RequestMapping("/api/v1/executions")
public class ExecutionResource {

	@Autowired
	private ExecutionService execService;

	@PostMapping("/submit")
	public ResponseEntity<ExecutionJob> submitJob(@RequestBody ExecutionJob job) {
		// Enqueues the job into RabbitMQ and returns the Job ID for polling
		return ResponseEntity.ok(execService.submitExecution(job));
	}

	@GetMapping("/{jobId}")
	public ResponseEntity<ExecutionJob> getJobStatus(@PathVariable String jobId) {
		return execService.getJobById(jobId).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
	}

	@GetMapping("/user/{userId}")
	public ResponseEntity<List<ExecutionJob>> getUserHistory(@PathVariable int userId) {
		return ResponseEntity.ok(execService.getExecutionsByUser(userId));
	}
}