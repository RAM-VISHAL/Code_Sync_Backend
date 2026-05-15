package com.fileservice.serviceImpl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.fileservice.dto.FileNode;
import com.fileservice.dto.ProjectPermissionResponse;
import com.fileservice.entity.CodeFile;
import com.fileservice.entity.Folder;
import com.fileservice.repository.CodeFileRepository;
import com.fileservice.repository.FolderRepository;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Unit Tests for File-Service Logic. Focuses on recursive tree building,
 * soft-deletion, and deep-cloning (forking).
 */
@ExtendWith(MockitoExtension.class)
class FileServiceImplTest {

	@Mock
	private FolderRepository folderRepository;

	@Mock
	private CodeFileRepository fileRepository;

	@Mock
	private RestTemplate restTemplate;

	@InjectMocks
	private FileServiceImpl fileService;

	private Folder mockFolder;
	private CodeFile mockFile;
	private final int projectId = 1;

	@BeforeEach
	void setUp() {
		// Prepare a mock folder for tree and clone tests
		mockFolder = new Folder();
		mockFolder.setFolderId(1L);
		mockFolder.setName("src");
		mockFolder.setProjectId(projectId);
		mockFolder.setDeleted(false);

		// Prepare a mock file for content update and search tests
		mockFile = new CodeFile();
		mockFile.setFileId(1L);
		mockFile.setName("App.java");
		mockFile.setExtension("java");
		mockFile.setContent("System.out.println();");
		mockFile.setProjectId(projectId);
		mockFile.setDeleted(false);

		ProjectPermissionResponse permission = new ProjectPermissionResponse();
		permission.setCanView(true);
		permission.setCanEdit(true);
		lenient().when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class),
				eq(ProjectPermissionResponse.class))).thenReturn(ResponseEntity.ok(permission));
	}

	@Test
	void testCreateFolder_InitializesCorrectly() {
		when(folderRepository.save(any(Folder.class))).thenReturn(mockFolder);
		Folder result = fileService.createFolder(new Folder(), 1);

		assertNotNull(result);
		assertFalse(result.isDeleted(), "New folders must not be marked as deleted");
		verify(folderRepository, times(1)).save(any());
	}

	@Test
	void testUpdateFileContent_CalculatesMetadata() {
		when(fileRepository.findById(1L)).thenReturn(Optional.of(mockFile));
		when(fileRepository.save(any(CodeFile.class))).thenReturn(mockFile);

		CodeFile result = fileService.updateFileContent(1L, "new content", 123);

		assertEquals("new content", result.getContent());
		assertEquals(123, result.getLastEditedBy(), "Attribution must track the editing user ID");
		assertEquals(11, result.getSize(), "Size must reflect content character count");
		verify(fileRepository).save(mockFile);
	}

	@Test
	void testGetProjectTree_InitializesREADME_OnEmptyProject() {
		// Simulate a brand new project with zero content
		when(folderRepository.findByProjectId(projectId)).thenReturn(Collections.emptyList());
		when(fileRepository.findByProjectIdAndIsDeletedFalse(projectId)).thenReturn(Collections.emptyList());

		// Standard tree fetching mocks
		when(folderRepository.findByProjectIdAndParentFolderIdIsNullAndIsDeletedFalse(projectId))
				.thenReturn(Collections.emptyList());
		when(fileRepository.findByProjectIdAndFolderIdIsNullAndIsDeletedFalse(projectId))
				.thenReturn(Collections.emptyList());

		fileService.getProjectTree(projectId);

		// Requirement: Every new project starts with a README.md for developer guidance
		verify(fileRepository, atLeastOnce()).save(argThat(file -> file.getName().equals("README.md")));
	}

	@Test
	void testDeleteFolder_SoftDeleteRecursive_Verification() {
		when(folderRepository.findById(1L)).thenReturn(Optional.of(mockFolder));
		when(fileRepository.findByFolderIdAndIsDeletedFalse(1L)).thenReturn(List.of(mockFile));
		when(folderRepository.findByParentFolderIdAndIsDeletedFalse(1L)).thenReturn(Collections.emptyList());

		fileService.deleteFolder(1L);

		// Verify soft-deletion pattern (Standard Industry Practice for VFS)
		assertTrue(mockFolder.isDeleted());
		assertTrue(mockFile.isDeleted());
		verify(folderRepository).save(mockFolder);
		verify(fileRepository).saveAll(anyList());
	}

	@Test
	void testCloneProjectFiles_AssignsNewTargetId() {
		int targetId = 2; // The forked project ID

		when(folderRepository.findByProjectIdAndParentFolderIdIsNullAndIsDeletedFalse(projectId))
				.thenReturn(List.of(mockFolder));
		when(fileRepository.findByProjectIdAndFolderIdIsNullAndIsDeletedFalse(projectId))
				.thenReturn(Collections.emptyList());
		when(folderRepository.findByParentFolderIdAndIsDeletedFalse(1L)).thenReturn(Collections.emptyList());
		when(fileRepository.findByFolderIdAndIsDeletedFalse(1L)).thenReturn(List.of(mockFile));

		// Handle ID generation during clone
		when(folderRepository.save(any(Folder.class))).thenAnswer(invocation -> {
			Folder folder = invocation.getArgument(0);
			if (folder.getFolderId() == null)
				folder.setFolderId(999L);
			return folder;
		});
		when(fileRepository.save(any(CodeFile.class))).thenAnswer(i -> i.getArgument(0));

		fileService.cloneProjectFiles(projectId, targetId);

		// Verify the clones are linked to the NEW project ID
		verify(folderRepository, atLeastOnce()).save(argThat(f -> f.getProjectId() == targetId));
		verify(fileRepository, atLeastOnce()).save(argThat(f -> f.getProjectId() == targetId));
	}

	@Test
	void testErrorHandling_FileNotFound_ThrowsHumanizedError() {
		when(fileRepository.findById(99L)).thenReturn(Optional.empty());

		// This will be caught by our GlobalExceptionHandler in the actual app
		assertThrows(RuntimeException.class, () -> {
			fileService.updateFileContent(99L, "text", 1);
		});
	}
}
