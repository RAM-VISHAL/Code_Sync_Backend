package com.collabservice.resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.collabservice.entity.*;
import com.collabservice.service.CollabService;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/sessions")
public class CollabResource {

	@Autowired
	private CollabService collabService;

	@PostMapping("/create")
	public ResponseEntity<CollabSession> createSession(@RequestBody CollabSession session) {
		return ResponseEntity.ok(collabService.createSession(session));
	}

	@GetMapping("/{sessionId}")
	public ResponseEntity<CollabSession> getSession(@PathVariable String sessionId) {
		return collabService.getSessionById(sessionId).map(ResponseEntity::ok)
				.orElseThrow(() -> new RuntimeException("Session metadata could not be retrieved"));
	}

	@PostMapping("/{sessionId}/join")
	public ResponseEntity<Participant> joinSession(@PathVariable String sessionId, @RequestParam int userId,
			@RequestParam String role, @RequestParam(required = false) String username) {
		return ResponseEntity.ok(collabService.joinSession(sessionId, userId, role, username));
	}

	@PutMapping("/{sessionId}/cursor")
	public ResponseEntity<Void> updateCursor(@PathVariable String sessionId, @RequestBody Map<String, Object> coords) {
		int userId = Integer.parseInt(coords.get("userId").toString());
		int line = Integer.parseInt(coords.get("line").toString());
		int col = Integer.parseInt(coords.get("col").toString());

		collabService.updateCursor(sessionId, userId, line, col);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/{sessionId}/participants")
	public ResponseEntity<List<Participant>> getActiveParticipants(@PathVariable String sessionId) {
		return ResponseEntity.ok(collabService.getParticipants(sessionId));
	}

	@DeleteMapping("/{sessionId}/leave")
	public ResponseEntity<Void> leave(@PathVariable String sessionId, @RequestParam int userId) {
		collabService.leaveSession(sessionId, userId);
		return ResponseEntity.noContent().build();
	}
}
