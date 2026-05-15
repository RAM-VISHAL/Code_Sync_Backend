package com.collabservice.serviceTest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.List;
import java.util.ArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.collabservice.entity.CollabSession;
import com.collabservice.entity.Participant;
import com.collabservice.repository.CollabRepository;
import com.collabservice.repository.ParticipantRepository;
import com.collabservice.serviceImpl.CollabServiceImpl;

/**
 * Unit Tests for Collab-Service. Focuses on session orchestration, cursor
 * synchronization, and participant presence.
 */
@ExtendWith(MockitoExtension.class)
public class CollabServiceTest {

	@Mock
	private CollabRepository collabRepo;

	@Mock
	private ParticipantRepository participantRepo;

	@InjectMocks
	private CollabServiceImpl collabService;

	private CollabSession sampleSession;
	private Participant sampleParticipant;

	@BeforeEach
	void setUp() {
		// Initialize a standard session for testing
		sampleSession = new CollabSession();
		sampleSession.setSessionId("session-uuid");
		sampleSession.setProjectId(101);
		sampleSession.setFileId(202);
		sampleSession.setStatus("ACTIVE");

		// Initialize a standard participant
		sampleParticipant = new Participant();
		sampleParticipant.setUserId(1);
		sampleParticipant.setSessionId("session-uuid");
	}

	@Test
	void testCreateSession_ShouldAssignUUIDAndStatus() {
		// Arrange
		when(collabRepo.save(any(CollabSession.class))).thenReturn(sampleSession);

		// Act
		CollabSession created = collabService.createSession(new CollabSession());

		// Assert
		assertNotNull(created.getSessionId(), "Service must generate a unique UUID for every session");
		assertEquals("ACTIVE", created.getStatus(), "Newly created sessions must default to ACTIVE status");
		verify(collabRepo, times(1)).save(any(CollabSession.class));
	}

	@Test
	void testJoinSession_ShouldAssignDistinctColor() {
		// Arrange
		String sessionId = "session-uuid";
		when(collabRepo.findById(sessionId)).thenReturn(Optional.of(sampleSession));
		// Mock count as 1, which should result in the second color from the palette
		// (#06B6D4)
		when(participantRepo.countBySessionId(sessionId)).thenReturn(1L);
		when(participantRepo.save(any(Participant.class))).thenAnswer(i -> i.getArguments()[0]);

		// Act
		Participant p = collabService.joinSession(sessionId, 5, "EDITOR", "tester");

		// Assert
		assertEquals("#06B6D4", p.getColor(), "Color must be assigned based on participant count rotation");
		assertEquals(5, p.getUserId());
		verify(participantRepo).save(any(Participant.class));
	}

	@Test
	void testJoinSession_NonExistentSession_ShouldThrowException() {
		// Arrange: Simulate a user trying to join an expired or invalid session ID
		when(collabRepo.findById("invalid-id")).thenReturn(Optional.empty());

		// Act & Assert: This ensures our GlobalExceptionHandler has a clear message to
		// catch
		RuntimeException ex = assertThrows(RuntimeException.class, () -> {
			collabService.joinSession("invalid-id", 1, "EDITOR", "tester");
		});

		assertTrue(ex.getMessage().contains("Session not found"), "Error message must be user-friendly for the UI");
	}

	@Test
	void testUpdateCursor_ShouldPersistCoordinates() {
		// Arrange
		String sessionId = "session-uuid";
		int userId = 1;
		List<Participant> participants = new ArrayList<>();
		participants.add(sampleParticipant);

		when(participantRepo.findBySessionId(sessionId)).thenReturn(participants);

		// Act: User moves cursor to Line 15, Column 40
		collabService.updateCursor(sessionId, userId, 15, 40);

		// Assert
		assertEquals(15, sampleParticipant.getCursorLine());
		assertEquals(40, sampleParticipant.getCursorCol());
		verify(participantRepo).save(sampleParticipant);
	}

	@Test
	void testEndSession_ShouldMarkStatusAndTimestamp() {
		// Arrange
		String sid = "session-uuid";
		when(collabRepo.findById(sid)).thenReturn(Optional.of(sampleSession));

		// Act
		collabService.endSession(sid);

		// Assert
		assertEquals("ENDED", sampleSession.getStatus(), "Session status must update to ENDED");
		assertNotNull(sampleSession.getEndedAt(), "endedAt timestamp must be populated on session closure");
		verify(collabRepo).save(sampleSession);
	}
}
