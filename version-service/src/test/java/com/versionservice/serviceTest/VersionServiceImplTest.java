package com.versionservice.serviceTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import com.versionservice.entity.Snapshot;
import com.versionservice.repository.SnapshotRepository;
import com.versionservice.serviceImpl.VersionServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Unit Tests for Version-Service. Ensures cryptographic integrity, accurate
 * diff calculation, and history preservation.
 */
@ExtendWith(MockitoExtension.class)
public class VersionServiceImplTest {

	@Mock
	private SnapshotRepository repository;

	@InjectMocks
	private VersionServiceImpl versionService;

	private Snapshot baseSnapshot;
	private Snapshot headSnapshot;

	@BeforeEach
	void setUp() {
		// Sample data representing the evolution of a Java class
		baseSnapshot = Snapshot.builder().id(1L).fileId(10).content("public class Hello {\n    // Old Code\n}").build();

		headSnapshot = Snapshot.builder().id(2L).fileId(10).content("public class Hello {\n    // New Code\n}").build();
	}

	@Test
	void testCreateSnapshot_IntegrityAndHashing() {
		// Arrange: Mock the save and the "latest version" check
		when(repository.save(any(Snapshot.class))).thenAnswer(i -> i.getArguments()[0]);
		when(repository.findFirstByFileIdOrderByCreatedAtDesc(10)).thenReturn(Optional.empty());

		// Act
		Snapshot result = versionService.createSnapshot(baseSnapshot);

		// Assert
		assertNotNull(result.getHash(), "Snapshot must have a generated hash");
		assertEquals(64, result.getHash().length(), "SHA-256 hash must be exactly 64 characters (hex)");
		verify(repository, times(1)).save(any(Snapshot.class));
	}

	@Test
	@SuppressWarnings("unchecked")
	void testCompareSnapshots_MyersDiffLogic() {
		// Arrange
		when(repository.findById(1L)).thenReturn(Optional.of(baseSnapshot));
		when(repository.findById(2L)).thenReturn(Optional.of(headSnapshot));

		// Act: Generate line-by-line comparison
		Map<String, Object> diffResult = versionService.compareSnapshots(1L, 2L);

		// Assert
		assertTrue(diffResult.containsKey("diffs"), "Response must contain the calculated diffs");
		List<String> changes = (List<String>) diffResult.get("diffs");

		// Ensure the algorithm detected the difference between "Old Code" and "New
		// Code"
		assertFalse(changes.isEmpty(), "Diff results should not be empty for modified files");
		assertTrue(changes.stream().anyMatch(s -> s.contains("New Code")), "Diff should highlight the added line");
	}

	@Test
	void testRestoreVersion_NonDestructiveFlow() {
		// Arrange: Mock retrieval of the old version we want to revert to
		when(repository.findById(1L)).thenReturn(Optional.of(baseSnapshot));
		when(repository.save(any(Snapshot.class))).thenAnswer(i -> i.getArguments()[0]);

		// Act: Restore file to state of Snapshot #1
		Snapshot restored = versionService.restoreVersion(1L, 500);

		// Assert: Requirement check - Restoration must create a NEW entry, not delete
		// history
		assertEquals(baseSnapshot.getContent(), restored.getContent(),
				"Restored content must match the target version");
		assertTrue(restored.getCommitMessage().contains("Restored"),
				"Restoration must be labeled in the history trail");
		assertEquals(500, restored.getUserId(), "The restorer's ID must be attributed to the new snapshot");

		// Verify it was treated as a NEW snapshot creation (Standard industry
		// immutability)
		verify(repository, times(1)).save(any(Snapshot.class));
	}
}