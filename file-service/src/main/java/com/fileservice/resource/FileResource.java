package com.fileservice.resource;

import com.fileservice.dto.FileNode;
import com.fileservice.entity.CodeFile;
import com.fileservice.entity.Folder;
import com.fileservice.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/files")
public class FileResource {

	@Autowired
	private FileService fileService;

	@GetMapping("/tree/{projectId}")
	public ResponseEntity<List<FileNode>> getProjectTree(@PathVariable int projectId) {
		// Loads the sidebar explorer data
		return ResponseEntity.ok(fileService.getProjectTree(projectId));
	}

	@PostMapping("/file")
	public ResponseEntity<CodeFile> createFile(@RequestBody CodeFile file) {
		return ResponseEntity.ok(fileService.createFile(file, requireCurrentUserId()));
	}

	@PutMapping("/file/{fileId}/content")
	public ResponseEntity<CodeFile> updateFileContent(@PathVariable Long fileId, @RequestBody String content,
			@RequestParam int userId) {
		// Logic to strip extra quotes if sent by frontend JSON.stringify
		String cleanedContent = (content != null && content.startsWith("\"") && content.endsWith("\""))
				? content.substring(1, content.length() - 1)
				: content;

		return ResponseEntity.ok(fileService.updateFileContent(fileId, cleanedContent, requireCurrentUserId()));
	}

	@PostMapping("/folder")
	public ResponseEntity<Folder> createFolder(@RequestBody Folder folder) {
		return ResponseEntity.ok(fileService.createFolder(folder, requireCurrentUserId()));
	}

	@PostMapping("/clone")
	public ResponseEntity<Void> cloneProjectFiles(@RequestParam int sourceId, @RequestParam int targetId) {
		// Triggered by Project-Service during a 'Fork' operation
		fileService.cloneProjectFiles(sourceId, targetId);
		return ResponseEntity.ok().build();
	}

	@DeleteMapping("/file/{fileId}")
	public ResponseEntity<Void> deleteFile(@PathVariable Long fileId) {
		requireCurrentUserId();
		fileService.deleteFile(fileId);
		return ResponseEntity.noContent().build();
	}

	@DeleteMapping("/folder/{folderId}")
	public ResponseEntity<Void> deleteFolder(@PathVariable Long folderId) {
		requireCurrentUserId();
		fileService.deleteFolder(folderId);
		return ResponseEntity.noContent().build();
	}

	@PatchMapping("/file/{fileId}/rename")
	public ResponseEntity<CodeFile> renameFile(@PathVariable Long fileId, @RequestParam("newName") String newName) {
		requireCurrentUserId();
		return ResponseEntity.ok(fileService.renameFile(fileId, newName));
	}

	@PatchMapping("/folder/{folderId}/rename")
	public ResponseEntity<Folder> renameFolder(@PathVariable Long folderId, @RequestParam("newName") String newName) {
		requireCurrentUserId();
		return ResponseEntity.ok(fileService.renameFolder(folderId, newName));
	}

	@GetMapping("/search/{projectId}")
	public ResponseEntity<List<CodeFile>> searchInProject(@PathVariable int projectId,
			@RequestParam("q") String query) {
		return ResponseEntity.ok(fileService.searchInProject(projectId, query));
	}

	private int requireCurrentUserId() {
		try {
			String forwardedUserId = currentRequestHeader("X-Authenticated-UserId");
			if (forwardedUserId != null && !forwardedUserId.isBlank()) {
				return Integer.parseInt(forwardedUserId);
			}
		} catch (Exception ignored) {
			// fall through
		}
		throw new RuntimeException("Authentication required.");
	}

	private String currentRequestHeader(String name) {
		try {
			return ((org.springframework.web.context.request.ServletRequestAttributes) org.springframework.web.context.request.RequestContextHolder
					.currentRequestAttributes()).getRequest().getHeader(name);
		} catch (Exception ignored) {
			return null;
		}
	}
}
