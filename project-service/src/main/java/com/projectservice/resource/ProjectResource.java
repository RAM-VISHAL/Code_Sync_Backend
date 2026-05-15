package com.projectservice.resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.projectservice.dto.ProjectPermissionResponse;
import com.projectservice.entity.Project;
import com.projectservice.entity.ProjectMember;
import com.projectservice.service.ProjectService;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectResource {

	@Autowired
	private ProjectService projectService;

	@PostMapping
	public ResponseEntity<Project> createProject(@RequestBody Project project) {
		// Requirement: Every project must have an owner and a language
		Integer currentUserId = requireCurrentUserId();
		project.setOwnerId(currentUserId);
		return ResponseEntity.ok(projectService.createProject(project));
	}

	@GetMapping("/owner/{ownerId}")
	public ResponseEntity<List<Project>> getByOwner(@PathVariable int ownerId) {
		if (!Integer.valueOf(ownerId).equals(requireCurrentUserId())) {
			throw new RuntimeException("You can only view your own private project list.");
		}
		return ResponseEntity.ok(projectService.getProjectsByOwner(ownerId));
	}

	@GetMapping("/public")
	public ResponseEntity<List<Project>> getPublic(@RequestParam(required = false) Integer currentUserId) {
		// Fetches community projects with 'isStarredByMe' logic applied
		Integer resolvedUserId = currentUserId != null ? currentUserId : currentUserIdOrNull();
		return ResponseEntity.ok(projectService.getPublicProjects(resolvedUserId != null ? resolvedUserId : 0));
	}

	@PutMapping("/{id}/star")
	public ResponseEntity<Void> toggleStar(@PathVariable int id, @RequestParam int userId) {
		projectService.starProject(id, userId);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/search")
	public ResponseEntity<List<Project>> search(@RequestParam String name, @RequestParam int userId) {
		return ResponseEntity.ok(projectService.searchProjects(name, userId));
	}

	@GetMapping("/{id}")
	public ResponseEntity<Project> getById(@PathVariable int id) {
		// Vital for the EditorPage to load project metadata (Language, Name)
		if (!projectService.canViewProject(id, currentUserIdOrNull())) {
			throw new RuntimeException("You do not have permission to view this project.");
		}
		return ResponseEntity.ok(projectService.getProjectById(id));
	}

	@GetMapping("/{id}/permissions/me")
	public ResponseEntity<ProjectPermissionResponse> getMyPermissions(@PathVariable int id) {
		return ResponseEntity.ok(projectService.getProjectPermissions(id, currentUserIdOrNull()));
	}

	@PostMapping("/{id}/fork")
	public ResponseEntity<Project> fork(@PathVariable int id, @RequestParam int userId, @RequestParam String username) {
		Integer currentUserId = requireCurrentUserId();
		return ResponseEntity.ok(projectService.forkProject(id, currentUserId, username));
	}

	@PutMapping("/{id}")
	public ResponseEntity<Project> updateProject(@PathVariable int id, @RequestBody Project project) {
		assertOwner(id);
		return ResponseEntity.ok(projectService.updateProject(id, project));
	}

	// --- COLLABORATION ENDPOINTS ---

	@PostMapping("/{projectId}/members/request")
	public ResponseEntity<Void> requestAccess(@PathVariable int projectId, @RequestParam int userId,
			@RequestParam String username) {
		projectService.requestCollaboration(projectId, requireCurrentUserId(), username);
		return ResponseEntity.ok().build();
	}

	@PostMapping("/{projectId}/members/approve")
	public ResponseEntity<Void> approveAccess(@PathVariable int projectId, @RequestParam int userId) {
		assertOwner(projectId);
		projectService.approveCollaborator(projectId, userId);
		return ResponseEntity.ok().build();
	}

	@GetMapping("/{projectId}/access")
	public ResponseEntity<Boolean> checkEditAccess(@PathVariable int projectId, @RequestParam int userId) {
		// Gatekeeper check: Does this user have 'EDITOR' role or is 'OWNER'?
		return ResponseEntity.ok(projectService.hasEditAccess(projectId, requireCurrentUserId()));
	}

	@GetMapping("/{projectId}/requests")
	public ResponseEntity<List<ProjectMember>> getPendingRequests(@PathVariable int projectId) {
		assertOwner(projectId);
		return ResponseEntity.ok(projectService.getPendingRequests(projectId));
	}

	@GetMapping("/{projectId}/members")
	public ResponseEntity<List<ProjectMember>> getMembers(@PathVariable int projectId) {
		if (!projectService.canViewProject(projectId, requireCurrentUserId())) {
			throw new RuntimeException("You do not have permission to view project members.");
		}
		return ResponseEntity.ok(projectService.getProjectMembers(projectId));
	}

	@DeleteMapping("/{projectId}/members/{userId}")
	public ResponseEntity<Void> removeMember(@PathVariable int projectId, @PathVariable int userId) {
		assertOwner(projectId);
		projectService.removeProjectMember(projectId, userId);
		return ResponseEntity.noContent().build();
	}

	@PutMapping("/{projectId}/members/{userId}/edit-permission")
	public ResponseEntity<Void> updateEditPermission(@PathVariable int projectId, @PathVariable int userId,
			@RequestBody Map<String, Boolean> payload) {
		assertOwner(projectId);
		boolean canEdit = Boolean.TRUE.equals(payload.get("canEdit"));
		projectService.updateMemberEditPermission(projectId, userId, canEdit);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/{projectId}/members/grant-by-email")
	public ResponseEntity<Void> grantEditByEmail(@PathVariable int projectId, @RequestBody Map<String, String> payload) {
		assertOwner(projectId);
		projectService.grantEditAccessByEmail(projectId, payload.get("email"));
		return ResponseEntity.noContent().build();
	}

	@PutMapping("/{id}/archive")
	public ResponseEntity<Void> archive(@PathVariable int id) {
		assertOwner(id);
		projectService.archiveProject(id);
		return ResponseEntity.noContent().build();
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable int id) {
		assertOwner(id);
		projectService.deleteProject(id);
		return ResponseEntity.noContent().build();
	}

	private void assertOwner(int projectId) {
		ProjectPermissionResponse permission = projectService.getProjectPermissions(projectId, requireCurrentUserId());
		if (!permission.isOwner()) {
			throw new RuntimeException("Only the project owner can perform this action.");
		}
	}

	private Integer requireCurrentUserId() {
		Integer userId = currentUserIdOrNull();
		if (userId == null) {
			throw new RuntimeException("Authentication required.");
		}
		return userId;
	}

	private Integer currentUserIdOrNull() {
		String forwardedUserId = currentRequestHeader("X-Authenticated-UserId");
		if (forwardedUserId == null || forwardedUserId.isBlank()) {
			return null;
		}
		try {
			return Integer.valueOf(forwardedUserId);
		} catch (Exception ignored) {
			return null;
		}
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
