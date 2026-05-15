package com.fileservice.service;

import com.fileservice.entity.CodeFile;
import com.fileservice.entity.Folder;
import com.fileservice.dto.FileNode;
import java.util.List;

public interface FileService {
	Folder createFolder(Folder folder, int userId);

	void deleteFolder(Long folderId);

	CodeFile createFile(CodeFile file, int userId);

	CodeFile updateFileContent(Long fileId, String content, int userId);

	CodeFile renameFile(Long fileId, String newName);

	Folder renameFolder(Long folderId, String newName);

	void deleteFile(Long fileId);

	List<FileNode> getProjectTree(int projectId);

	List<CodeFile> searchInProject(int projectId, String query);
	
	void cloneProjectFiles(int sourceProjectId, int targetProjectId);

}
