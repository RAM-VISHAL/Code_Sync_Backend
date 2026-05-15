package com.fileservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.fileservice.dto.FileNode;
import com.fileservice.entity.CodeFile;
import com.fileservice.entity.Folder;
import com.fileservice.exception.GlobalExceptionHandler;
import com.fileservice.resource.FileResource;
import com.fileservice.service.FileService;

class FileServiceCoverageTest {

	@AfterEach
	void clearRequestContext() {
		RequestContextHolder.resetRequestAttributes();
	}

	@Test
	void fileResourceDelegatesToServiceAndUsesForwardedUser() {
		FileService fileService = org.mockito.Mockito.mock(FileService.class);
		FileResource resource = new FileResource();
		CodeFile file = new CodeFile();
		file.setFileId(1L);
		Folder folder = new Folder();
		folder.setFolderId(2L);

		ReflectionTestUtils.setField(resource, "fileService", fileService);
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("X-Authenticated-UserId", "11");
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

		when(fileService.getProjectTree(3)).thenReturn(List.of(FileNode.builder().id("folder-1").name("src").type("FOLDER").build()));
		when(fileService.createFile(file, 11)).thenReturn(file);
		when(fileService.updateFileContent(1L, "hello", 11)).thenReturn(file);
		when(fileService.createFolder(folder, 11)).thenReturn(folder);
		when(fileService.renameFile(1L, "new.java")).thenReturn(file);
		when(fileService.renameFolder(2L, "new-folder")).thenReturn(folder);
		when(fileService.searchInProject(3, "hello")).thenReturn(List.of(file));

		assertEquals(1, resource.getProjectTree(3).getBody().size());
		assertEquals(file, resource.createFile(file).getBody());
		assertEquals(file, resource.updateFileContent(1L, "\"hello\"", 55).getBody());
		assertEquals(folder, resource.createFolder(folder).getBody());
		assertEquals(200, resource.cloneProjectFiles(1, 2).getStatusCode().value());
		assertEquals(204, resource.deleteFile(1L).getStatusCode().value());
		assertEquals(204, resource.deleteFolder(2L).getStatusCode().value());
		assertEquals(file, resource.renameFile(1L, "new.java").getBody());
		assertEquals(folder, resource.renameFolder(2L, "new-folder").getBody());
		assertEquals(List.of(file), resource.searchInProject(3, "hello").getBody());

		verify(fileService).cloneProjectFiles(1, 2);
	}

	@Test
	void fileResourceRequiresAuthenticationWhenHeaderMissing() {
		FileResource resource = new FileResource();
		ReflectionTestUtils.setField(resource, "fileService", org.mockito.Mockito.mock(FileService.class));
		RequestContextHolder.resetRequestAttributes();
		assertThrows(RuntimeException.class, () -> resource.createFile(new CodeFile()));
	}

	@Test
	void globalExceptionHandlerBuildsExpectedPayloads() {
		GlobalExceptionHandler handler = new GlobalExceptionHandler();
		assertEquals("boom", handler.handleRuntimeException(new RuntimeException("boom")).getBody().get("message"));
		assertEquals("A file system error occurred: x",
				handler.handleGeneralException(new Exception("x")).getBody().get("message"));
	}

	@Test
	void applicationMainDelegatesToSpringApplication() {
		try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
			FileServiceApplication.main(new String[] { "test" });
			springApplication.verify(() -> SpringApplication.run(FileServiceApplication.class, new String[] { "test" }));
		}
	}
}
