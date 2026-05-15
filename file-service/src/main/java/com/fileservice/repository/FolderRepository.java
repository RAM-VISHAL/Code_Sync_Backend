package com.fileservice.repository;

import com.fileservice.entity.Folder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FolderRepository extends JpaRepository<Folder, Long> {
	List<Folder> findByProjectId(int projectId);

	// Added to resolve repository method errors in Impl
	List<Folder> findByParentFolderIdAndIsDeletedFalse(Long parentFolderId);

	List<Folder> findByProjectIdAndParentFolderIdIsNullAndIsDeletedFalse(int projectId);

	List<Folder> findByParentFolderId(Long parentFolderId);
}