package com.collabservice.serviceImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.collabservice.entity.*;
import com.collabservice.repository.*;
import com.collabservice.service.CollabService;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class CollabServiceImpl implements CollabService {

	@Autowired
	private CollabRepository collabRepo;

	@Autowired
	private ParticipantRepository participantRepo;

	// Vibrant colors for remote cursors in the editor
	private final String[] COLORS = { "#F97316", "#06B6D4", "#8B5CF6", "#EC4899", "#10B981", "#EAB308" };

	@Override
	@Transactional
	public CollabSession createSession(CollabSession session) {
		if (session.getFileId() > 0) {
			List<CollabSession> activeSessions = collabRepo.findByFileIdAndStatusOrderByCreatedAtDesc(session.getFileId(),
					"ACTIVE");
			if (!activeSessions.isEmpty()) {
				CollabSession newestSession = activeSessions.get(0);
				if (activeSessions.size() > 1) {
					LocalDateTime now = LocalDateTime.now();
					for (int index = 1; index < activeSessions.size(); index++) {
						CollabSession duplicateSession = activeSessions.get(index);
						duplicateSession.setStatus("ENDED");
						duplicateSession.setEndedAt(now);
						collabRepo.save(duplicateSession);
					}
				}
				return newestSession;
			}
		}

		session.setSessionId(UUID.randomUUID().toString());
		session.setStatus("ACTIVE");
		session.setCreatedAt(LocalDateTime.now());
		return collabRepo.save(session);
	}

	@Override
	@Transactional
	public Participant joinSession(String sessionId, int userId, String role, String username) {
		// Logic: Ensure session exists before joining
		collabRepo.findById(sessionId)
				.orElseThrow(() -> new RuntimeException("Join Failed: Session not found or expired"));

		Optional<Participant> existingParticipant = participantRepo.findBySessionIdAndUserId(sessionId, userId);
		if (existingParticipant.isPresent()) {
			Participant participant = existingParticipant.get();
			participant.setRole(role);
			if (username != null && !username.isBlank()) {
				participant.setUsername(username.trim());
			}
			return participantRepo.save(participant);
		}

		// Assign a rotating color based on current participant count
		long count = participantRepo.countBySessionId(sessionId);
		String color = COLORS[(int) (count % COLORS.length)];

		Participant participant = new Participant();
		participant.setSessionId(sessionId);
		participant.setUserId(userId);
		participant.setUsername(username != null && !username.isBlank() ? username.trim() : "User " + userId);
		participant.setRole(role);
		participant.setColor(color);
		participant.setJoinedAt(LocalDateTime.now());

		return participantRepo.save(participant);
	}

	@Override
	@Transactional
	public void leaveSession(String sessionId, int userId) {
		participantRepo.deleteBySessionIdAndUserId(sessionId, userId);
	}

	@Override
	@Transactional
	public void updateCursor(String sessionId, int userId, int line, int col) {
		// Updates the cursor coordinates for live synchronization
		participantRepo.findBySessionId(sessionId).stream().filter(p -> p.getUserId() == userId).findFirst()
				.ifPresent(p -> {
					p.setCursorLine(line);
					p.setCursorCol(col);
					participantRepo.save(p);
				});
	}

	@Override
	@Transactional
	public void endSession(String sessionId) {
		collabRepo.findById(sessionId).ifPresent(s -> {
			s.setStatus("ENDED");
			s.setEndedAt(LocalDateTime.now());
			collabRepo.save(s);
		});
	}

	@Override
	public Optional<CollabSession> getSessionById(String id) {
		return collabRepo.findById(id);
	}

	@Override
	public List<CollabSession> getSessionsByProject(int id) {
		return collabRepo.findByProjectId(id);
	}

	@Override
	public List<Participant> getParticipants(String id) {
		return participantRepo.findBySessionId(id);
	}
}
