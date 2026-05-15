package com.projectservice.service;

import java.util.List;
import com.projectservice.dto.ProjectPermissionResponse;
import com.projectservice.entity.Project;
import com.projectservice.entity.ProjectMember;

public interface ProjectService {
	Project createProject(Project project);

	List<Project> getProjectsByOwner(int ownerId);

	List<Project> getPublicProjects(int currentUserId);

	Project getProjectById(int projectId);

	Project forkProject(int sourceProjectId, int newOwnerId, String ownerUsername);

	void starProject(int projectId, int userId);

	void archiveProject(int projectId);

	List<Project> searchProjects(String name, int currentUserId);

	List<Project> getProjectsByLanguage(String language);

	Project updateProject(int projectId, Project project);

	void deleteProject(int projectId);

	void requestCollaboration(int projectId, int userId, String username);

	void approveCollaborator(int projectId, int userId);

	boolean hasEditAccess(int projectId, int userId);

	boolean canViewProject(int projectId, Integer userId);

	ProjectPermissionResponse getProjectPermissions(int projectId, Integer userId);

	List<ProjectMember> getPendingRequests(int projectId);

	List<ProjectMember> getProjectMembers(int projectId);

	void removeProjectMember(int projectId, int userId);

	void updateMemberEditPermission(int projectId, int userId, boolean canEdit);

	void grantEditAccessByEmail(int projectId, String email);
}
