package com.fileservice.repository;

import com.fileservice.entity.CodeFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CodeFileRepository extends JpaRepository<CodeFile, Long> {
	// Standard CRUD
	List<CodeFile> findByProjectId(int projectId);

	// For Tree Navigation: Finding files at root or within folders that aren't
	// deleted 
	List<CodeFile> findByProjectIdAndFolderIdIsNullAndIsDeletedFalse(int projectId);

	List<CodeFile> findByFolderIdAndIsDeletedFalse(Long folderId);

	// For finding all files in a project for initialization checks
	List<CodeFile> findByProjectIdAndIsDeletedFalse(int projectId);

	// Requirement: Find text patterns across all non-deleted project files
	List<CodeFile> findByProjectIdAndContentContainingIgnoreCaseAndIsDeletedFalse(int projectId, String query);
}