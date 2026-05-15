package com.projectservice.repository;

import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.projectservice.entity.ProjectStar;

public interface ProjectStarRepository extends JpaRepository<ProjectStar, Long> {

	boolean existsByProjectIdAndUserId(int projectId, int userId);

	java.util.Optional<ProjectStar> findByProjectIdAndUserId(int projectId, int userId);

	/**
	 * Optimized check for bulk project lists. Returns the subset of Project IDs
	 * that the user has actually starred.
	 */
	@Query(value = "SELECT project_id FROM project_stars WHERE user_id = :userId AND project_id IN (:projectIds)", nativeQuery = true)
	Set<Integer> findStarredProjectIds(@Param("projectIds") List<Integer> projectIds, @Param("userId") int userId);
}