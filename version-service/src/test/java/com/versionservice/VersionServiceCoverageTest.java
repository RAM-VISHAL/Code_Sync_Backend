package com.versionservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;
import org.springframework.test.util.ReflectionTestUtils;

import com.versionservice.entity.Snapshot;
import com.versionservice.exception.GlobalExceptionHandler;
import com.versionservice.resource.VersionResource;
import com.versionservice.service.VersionService;

class VersionServiceCoverageTest {

	@Test
	void versionResourceDelegatesToService() {
		VersionService versionService = org.mockito.Mockito.mock(VersionService.class);
		VersionResource resource = new VersionResource();
		Snapshot snapshot = Snapshot.builder().id(1L).fileId(12).content("v1").build();
		Map<String, Object> diff = Map.of("changed", true);

		ReflectionTestUtils.setField(resource, "versionService", versionService);
		when(versionService.createSnapshot(snapshot)).thenReturn(snapshot);
		when(versionService.getFileHistory(12)).thenReturn(List.of(snapshot));
		when(versionService.compareSnapshots(1L, 2L)).thenReturn(diff);
		when(versionService.restoreVersion(1L, 7)).thenReturn(snapshot);

		assertEquals(snapshot, resource.takeSnapshot(snapshot).getBody());
		assertEquals(List.of(snapshot), resource.getFileHistory(12).getBody());
		assertEquals(diff, resource.getLineDiff(1L, 2L).getBody());
		assertEquals(snapshot, resource.revertToFileVersion(1L, 7).getBody());
	}

	@Test
	void globalExceptionHandlerBuildsExpectedPayloads() {
		GlobalExceptionHandler handler = new GlobalExceptionHandler();

		assertEquals("failure", handler.handleRuntimeException(new RuntimeException("failure")).getBody().get("message"));
		assertEquals("Version history could not be retrieved. Please try again.",
				handler.handleGeneralException(new Exception("boom")).getBody().get("message"));
	}

	@Test
	void applicationMainDelegatesToSpringApplication() {
		try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
			VersionServiceApplication.main(new String[] { "test" });
			springApplication.verify(() -> SpringApplication.run(VersionServiceApplication.class, new String[] { "test" }));
		}
	}
}
