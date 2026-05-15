package com.collabservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import com.collabservice.entity.CollabSession;
import com.collabservice.entity.Participant;
import com.collabservice.exception.GlobalExceptionHandler;
import com.collabservice.resource.CollabResource;
import com.collabservice.resource.CollabWebSocketResource;
import com.collabservice.service.CollabService;

class CollabServiceCoverageTest {

	@Test
	void collabResourceDelegatesToService() {
		CollabService collabService = org.mockito.Mockito.mock(CollabService.class);
		CollabResource resource = new CollabResource();
		CollabSession session = new CollabSession();
		session.setSessionId("room-1");
		Participant participant = new Participant();
		participant.setUserId(9);

		ReflectionTestUtils.setField(resource, "collabService", collabService);
		when(collabService.createSession(session)).thenReturn(session);
		when(collabService.getSessionById("room-1")).thenReturn(Optional.of(session));
		when(collabService.joinSession("room-1", 9, "EDITOR", "alex")).thenReturn(participant);
		when(collabService.getParticipants("room-1")).thenReturn(List.of(participant));

		assertEquals(session, resource.createSession(session).getBody());
		assertEquals(session, resource.getSession("room-1").getBody());
		assertEquals(participant, resource.joinSession("room-1", 9, "EDITOR", "alex").getBody());
		assertEquals(List.of(participant), resource.getActiveParticipants("room-1").getBody());
		assertEquals(org.springframework.http.HttpStatus.NO_CONTENT,
				resource.updateCursor("room-1", Map.of("userId", 9, "line", 12, "col", 3)).getStatusCode());
		assertEquals(org.springframework.http.HttpStatus.NO_CONTENT, resource.leave("room-1", 9).getStatusCode());

		verify(collabService).updateCursor("room-1", 9, 12, 3);
		verify(collabService).leaveSession("room-1", 9);
	}

	@Test
	void collabResourceThrowsWhenSessionMissing() {
		CollabService collabService = org.mockito.Mockito.mock(CollabService.class);
		CollabResource resource = new CollabResource();
		ReflectionTestUtils.setField(resource, "collabService", collabService);
		when(collabService.getSessionById("missing")).thenReturn(Optional.empty());

		assertThrows(RuntimeException.class, () -> resource.getSession("missing"));
	}

	@Test
	void websocketResourceRelaysPayloadWithSessionId() {
		SimpMessagingTemplate template = org.mockito.Mockito.mock(SimpMessagingTemplate.class);
		CollabWebSocketResource resource = new CollabWebSocketResource(template);
		Map<String, Object> payload = new HashMap<>();
		payload.put("cursor", 10);

		resource.relaySessionMessage("abc", payload);
		resource.relaySessionMessage("empty", null);

		verify(template).convertAndSend(org.mockito.Mockito.eq("/topic/session/abc"),
				org.mockito.ArgumentMatchers.<Object>argThat(payloadObject -> {
					Map<?, ?> map = (Map<?, ?>) payloadObject;
					return "abc".equals(map.get("sessionId")) && Integer.valueOf(10).equals(map.get("cursor"));
				}));
		verify(template).convertAndSend(org.mockito.Mockito.eq("/topic/session/empty"),
				org.mockito.ArgumentMatchers.<Object>argThat(payloadObject -> {
					Map<?, ?> map = (Map<?, ?>) payloadObject;
					return "empty".equals(map.get("sessionId")) && map.size() == 1;
				}));
	}

	@Test
	void globalExceptionHandlerBuildsExpectedPayloads() {
		GlobalExceptionHandler handler = new GlobalExceptionHandler();
		assertEquals("bad", handler.handleRuntimeException(new RuntimeException("bad")).getBody().get("message"));
		assertEquals("Real-time sync error. Please refresh your editor.",
				handler.handleGeneralException(new Exception("boom")).getBody().get("message"));
	}

	@Test
	void applicationMainDelegatesToSpringApplication() {
		try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
			CollabServiceApplication.main(new String[] { "test" });
			springApplication.verify(() -> SpringApplication.run(CollabServiceApplication.class, new String[] { "test" }));
		}
	}
}
