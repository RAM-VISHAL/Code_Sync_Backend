package com.projectservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.projectservice.entity.Project;
import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Integer> {
	List<Project> findByOwnerId(int ownerId);

	List<Project> findByVisibility(String visibility);

	List<Project> findByLanguage(String language);

	List<Project> findByNameContainingIgnoreCase(String name);

	List<Project> findByIsArchived(boolean isArchived);

	Long countByOwnerId(int ownerId);
}