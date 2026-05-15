package com.fileservice.serviceImpl;

import com.fileservice.dto.ProjectPermissionResponse;
import com.fileservice.entity.CodeFile;
import com.fileservice.entity.Folder;
import com.fileservice.dto.FileNode;
import com.fileservice.repository.CodeFileRepository;
import com.fileservice.repository.FolderRepository;
import com.fileservice.service.FileService;
import jakarta.transaction.Transactional;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class FileServiceImpl implements FileService {

	@Autowired
	private FolderRepository folderRepository;

	@Autowired
	private CodeFileRepository fileRepository;

	@Autowired
	private RestTemplate restTemplate;

	@Override
	@Transactional
	public Folder createFolder(Folder folder, int userId) {
		assertCanEdit(folder.getProjectId());
		folder.setDeleted(false);
		return folderRepository.save(folder);
	}

	@Override
	@Transactional
	public CodeFile createFile(CodeFile file, int userId) {
		assertCanEdit(file.getProjectId());
		file.setDeleted(false);
		file.setCreatedById(userId);
		file.setLastEditedBy(userId);
		file.setCreatedAt(LocalDateTime.now());
		file.setUpdatedAt(LocalDateTime.now());
		return fileRepository.save(file);
	}

	@Override
	@Transactional
	public CodeFile updateFileContent(Long fileId, String content, int userId) {
		CodeFile file = fileRepository.findById(fileId)
				.orElseThrow(() -> new RuntimeException("Update Failed: File not found"));
		assertCanEdit(file.getProjectId());

		file.setContent(content);
		file.setLastEditedBy(userId);
		file.setUpdatedAt(LocalDateTime.now());
		file.setSize((long) (content != null ? content.length() : 0));
		return fileRepository.save(file);
	}

	@Override
	public List<FileNode> getProjectTree(int projectId) {
		assertCanView(projectId);
		// Auto-initialize if project is empty
		if (folderRepository.findByProjectId(projectId).isEmpty()
				&& fileRepository.findByProjectIdAndIsDeletedFalse(projectId).isEmpty()) {
			initializeNewProject(projectId);
		}

		List<FileNode> rootNodes = new ArrayList<>();

		// 1. Fetch Root Folders
		List<Folder> rootFolders = folderRepository.findByProjectIdAndParentFolderIdIsNullAndIsDeletedFalse(projectId);
		for (Folder folder : rootFolders) {
			rootNodes.add(buildFolderTree(folder));
		}

		// 2. Fetch Root Files
		List<CodeFile> rootFiles = fileRepository.findByProjectIdAndFolderIdIsNullAndIsDeletedFalse(projectId);
		for (CodeFile file : rootFiles) {
			rootNodes.add(convertToFileNode(file));
		}

		return rootNodes;
	}

	private void initializeNewProject(int projectId) {
		CodeFile readme = new CodeFile();
		readme.setName("README.md");
		readme.setExtension("md");
		readme.setContent("# Welcome to CodeSync\nCreated at: " + LocalDateTime.now());
		readme.setProjectId(projectId);
		readme.setPath("README.md");
		readme.setSize((long) readme.getContent().length());
		Integer currentUserId = currentUserId();
		int actorId = currentUserId != null ? currentUserId : 1;
		readme.setCreatedById(actorId);
		readme.setLastEditedBy(actorId);
		fileRepository.save(readme);
	}

	private FileNode buildFolderTree(Folder folder) {
		FileNode node = FileNode.builder().id("folder-" + folder.getFolderId()).name(folder.getName()).type("FOLDER")
				.children(new ArrayList<>()).build();

		// Recursive call for subfolders
		folderRepository.findByParentFolderIdAndIsDeletedFalse(folder.getFolderId())
				.forEach(sub -> node.getChildren().add(buildFolderTree(sub)));

		// Add files within this folder
		fileRepository.findByFolderIdAndIsDeletedFalse(folder.getFolderId())
				.forEach(f -> node.getChildren().add(convertToFileNode(f)));

		return node;
	}

	private FileNode convertToFileNode(CodeFile file) {
		return FileNode.builder().id("file-" + file.getFileId()).name(file.getName()).type("FILE")
				.content(file.getContent()).build();
	}

	@Override
	@Transactional
	public void deleteFolder(Long folderId) {
		Folder folder = folderRepository.findById(folderId)
				.orElseThrow(() -> new RuntimeException("Delete Failed: Folder not found"));
		assertCanEdit(folder.getProjectId());

		// Soft delete the folder
		folder.setDeleted(true);
		folderRepository.save(folder);

		// Soft delete all files inside
		List<CodeFile> files = fileRepository.findByFolderIdAndIsDeletedFalse(folderId);
		files.forEach(f -> f.setDeleted(true));
		fileRepository.saveAll(files);

		// Recursively handle subfolders
		folderRepository.findByParentFolderIdAndIsDeletedFalse(folderId)
				.forEach(sub -> deleteFolder(sub.getFolderId()));
	}

	@Override
	@Transactional
	public void cloneProjectFiles(int sourceProjectId, int targetProjectId) {
		// Clone Hierarchy starting from root folders
		folderRepository.findByProjectIdAndParentFolderIdIsNullAndIsDeletedFalse(sourceProjectId)
				.forEach(f -> cloneFolderRecursive(f, null, targetProjectId));

		// Clone Root Files
		fileRepository.findByProjectIdAndFolderIdIsNullAndIsDeletedFalse(sourceProjectId).forEach(file -> {
			CodeFile newFile = new CodeFile();
			newFile.setName(file.getName());
			newFile.setContent(file.getContent());
			newFile.setExtension(file.getExtension());
			newFile.setProjectId(targetProjectId);
			newFile.setCreatedById(file.getCreatedById());
			newFile.setLastEditedBy(file.getLastEditedBy() > 0 ? file.getLastEditedBy() : file.getCreatedById());
			newFile.setDeleted(false);
			newFile.setCreatedAt(LocalDateTime.now());
			newFile.setUpdatedAt(LocalDateTime.now());
			fileRepository.save(newFile);
		});
	}

	private void cloneFolderRecursive(Folder sourceFolder, Long targetParentId, int targetProjectId) {
		Folder newFolder = new Folder();
		newFolder.setName(sourceFolder.getName());
		newFolder.setProjectId(targetProjectId);
		newFolder.setParentFolderId(targetParentId);
		newFolder.setDeleted(false);
		newFolder = folderRepository.save(newFolder);

		final Long currentNewFolderId = newFolder.getFolderId();

		// Subfolders
		folderRepository.findByParentFolderIdAndIsDeletedFalse(sourceFolder.getFolderId())
				.forEach(sub -> cloneFolderRecursive(sub, currentNewFolderId, targetProjectId));

		// Files
		fileRepository.findByFolderIdAndIsDeletedFalse(sourceFolder.getFolderId()).forEach(f -> {
			CodeFile nf = new CodeFile();
			nf.setName(f.getName());
			nf.setContent(f.getContent());
			nf.setExtension(f.getExtension());
			nf.setProjectId(targetProjectId);
			nf.setFolderId(currentNewFolderId.intValue());
			nf.setDeleted(false);
			nf.setCreatedById(f.getCreatedById() > 0 ? f.getCreatedById() : 1);
			nf.setLastEditedBy(f.getLastEditedBy() > 0 ? f.getLastEditedBy() : nf.getCreatedById());
			nf.setCreatedAt(LocalDateTime.now());
			nf.setUpdatedAt(LocalDateTime.now());
			fileRepository.save(nf);
		});
	}

	@Override
	public void deleteFile(Long id) {
		fileRepository.findById(id).ifPresent(f -> {
			assertCanEdit(f.getProjectId());
			f.setDeleted(true);
			fileRepository.save(f);
		});
	}

	@Override
	public CodeFile renameFile(Long id, String name) {
		CodeFile f = fileRepository.findById(id).orElseThrow();
		assertCanEdit(f.getProjectId());
		f.setName(name);
		if (name != null && name.contains(".")) {
			f.setExtension(name.substring(name.lastIndexOf(".") + 1));
		} else {
			f.setExtension("");
		}
		return fileRepository.save(f);
	}

	@Override
	public Folder renameFolder(Long id, String name) {
		Folder f = folderRepository.findById(id).orElseThrow();
		assertCanEdit(f.getProjectId());
		f.setName(name);
		return folderRepository.save(f);
	}

	@Override
	public List<CodeFile> searchInProject(int pid, String q) {
		assertCanView(pid);
		return fileRepository.findByProjectIdAndContentContainingIgnoreCaseAndIsDeletedFalse(pid, q);
	}

	private void assertCanView(int projectId) {
		ProjectPermissionResponse permission = fetchPermissions(projectId);
		if (!permission.isCanView()) {
			throw new RuntimeException("You do not have permission to view this project.");
		}
	}

	private void assertCanEdit(int projectId) {
		ProjectPermissionResponse permission = fetchPermissions(projectId);
		if (!permission.isCanEdit()) {
			throw new RuntimeException("You do not have permission to edit this project.");
		}
	}

	private ProjectPermissionResponse fetchPermissions(int projectId) {
		HttpHeaders headers = new HttpHeaders();
		copyGatewayIdentityHeader(headers, "X-Authenticated-User");
		copyGatewayIdentityHeader(headers, "X-Authenticated-UserId");
		copyGatewayIdentityHeader(headers, "X-Authenticated-Role");
		copyGatewayIdentityHeader(headers, "X-Authenticated-Via");
		HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
		ResponseEntity<ProjectPermissionResponse> response = restTemplate.exchange(
				"http://project-service/api/v1/projects/" + projectId + "/permissions/me", HttpMethod.GET, requestEntity,
				ProjectPermissionResponse.class);
		ProjectPermissionResponse body = response.getBody();
		if (body == null) {
			throw new RuntimeException("Could not validate project permissions.");
		}
		return body;
	}

	private void copyGatewayIdentityHeader(HttpHeaders headers, String name) {
		String value = currentRequestHeader(name);
		if (value != null && !value.isBlank()) {
			headers.set(name, value);
		}
	}

	private String currentRequestHeader(String name) {
		RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
		if (!(attributes instanceof ServletRequestAttributes servletAttributes)) {
			return null;
		}
		HttpServletRequest request = servletAttributes.getRequest();
		return request.getHeader(name);
	}

	private Integer currentUserId() {
		String forwardedUserId = currentRequestHeader("X-Authenticated-UserId");
		if (forwardedUserId == null || forwardedUserId.isBlank()) {
			return null;
		}
		try {
			return Integer.valueOf(forwardedUserId);
		} catch (NumberFormatException ex) {
			return null;
		}
	}
}
