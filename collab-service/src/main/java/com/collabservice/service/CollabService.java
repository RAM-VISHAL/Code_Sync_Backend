package com.collabservice.service;

import java.util.List;
import java.util.Optional;

import com.collabservice.entity.CollabSession;
import com.collabservice.entity.Participant;

public interface CollabService {
	CollabSession createSession(CollabSession session); 

	Optional<CollabSession> getSessionById(String sessionId); 

	List<CollabSession> getSessionsByProject(int projectId); 

	Participant joinSession(String sessionId, int userId, String role, String username); 

	void leaveSession(String sessionId, int userId); 

	void endSession(String sessionId); 

	void updateCursor(String sessionId, int userId, int line, int col); 

	List<Participant> getParticipants(String sessionId);
}
