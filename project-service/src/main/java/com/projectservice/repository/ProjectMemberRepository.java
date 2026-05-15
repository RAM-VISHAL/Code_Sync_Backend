package com.projectservice.repository;

import com.projectservice.entity.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {
	List<ProjectMember> findByProjectId(int projectId);

	Optional<ProjectMember> findByProjectIdAndUserId(int projectId, int userId);

	boolean existsByProjectIdAndUserIdAndRole(int projectId, int userId, String role);

	boolean existsByProjectIdAndUserIdAndStatus(int projectId, int userId, String status);

	boolean existsByProjectIdAndUserIdAndStatusAndCanEditTrue(int projectId, int userId, String status);
}
