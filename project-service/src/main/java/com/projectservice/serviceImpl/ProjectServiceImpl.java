package com.projectservice.serviceImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.projectservice.config.ProjectNotificationProducer;
import com.projectservice.entity.*;
import com.projectservice.dto.NotificationRequest;
import com.projectservice.dto.UserSummaryResponse;
import com.projectservice.dto.ProjectPermissionResponse;
import com.projectservice.repository.*;
import com.projectservice.service.ProjectService;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.nio.charset.StandardCharsets;

@Service
public class ProjectServiceImpl implements ProjectService {

	private static final String FREE_CREDIT_EXHAUSTED_MESSAGE = "Your 5 free credits are finished. Please subscribe to continue using premium services.";

	@Autowired
	private ProjectRepository projectRepository;

	@Autowired
	private ProjectStarRepository starRepository;

	@Autowired
	private ProjectMemberRepository memberRepository;

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private ProjectNotificationProducer notificationProducer;

	/**
	 * Populates the 'isStarredByMe' transient flag for UI heart-icons.
	 * Optimization: Uses a native query to fetch only relevant stars in one hit.
	 */
	private void applyStarStatus(List<Project> projects, int userId) {
		if (projects == null || projects.isEmpty())
			return;

		List<Integer> projectIds = projects.stream().map(Project::getProjectId).collect(Collectors.toList());
		Set<Integer> starredProjectIds = starRepository.findStarredProjectIds(projectIds, userId);

		projects.forEach(p -> p.setStarredByMe(starredProjectIds.contains(p.getProjectId())));
	}

	@SuppressWarnings("unchecked")
	private boolean isSubscribed(int userId) {
		try {
			Map<String, Object> response = restTemplate.getForObject(
					"http://payment-service/api/v1/payments/status/" + userId, Map.class);
			return response != null && Boolean.TRUE.equals(response.get("isSubscribed"));
		} catch (Exception ex) {
			return false;
		}
	}

	@SuppressWarnings("unchecked")
	private int consumeFreeCredit(int userId) {
		try {
			ResponseEntity<Map> response = restTemplate.postForEntity(
					"http://auth-service/api/v1/auth/credits/" + userId + "/consume?amount=1", null, Map.class);
			Map<String, Object> body = response.getBody();
			Object remaining = body != null ? body.get("freeCreditsRemaining") : null;
			return remaining instanceof Number number ? number.intValue() : 0;
		} catch (Exception ex) {
			String message = ex.getMessage() != null ? ex.getMessage() : FREE_CREDIT_EXHAUSTED_MESSAGE;
			throw new RuntimeException(message.contains("free credits") ? message : FREE_CREDIT_EXHAUSTED_MESSAGE);
		}
	}

	private void enforceSubscriptionOrConsumeCredit(int userId) {
		if (!isSubscribed(userId)) {
			consumeFreeCredit(userId);
		}
	}

	@Override
	public Project createProject(Project project) {
		enforceSubscriptionOrConsumeCredit(project.getOwnerId());
		return projectRepository.save(project);
	}

	@Override
	public List<Project> getProjectsByOwner(int ownerId) {
		List<Project> projects = projectRepository.findByOwnerId(ownerId);
		applyStarStatus(projects, ownerId);
		return projects;
	}

	@Override
	public List<Project> getPublicProjects(int currentUserId) {
		List<Project> projects = projectRepository.findByVisibility("PUBLIC");
		applyStarStatus(projects, currentUserId);
		return projects;
	}

	@Override
	@Transactional
	public void starProject(int projectId, int userId) {
		Project p = getProjectById(projectId);
		Optional<ProjectStar> existingStar = starRepository.findByProjectIdAndUserId(projectId, userId);

		if (existingStar.isPresent()) {
			starRepository.delete(existingStar.get());
			p.setStarCount(Math.max(0, p.getStarCount() - 1));
		} else {
			ProjectStar newStar = new ProjectStar();
			newStar.setProjectId(projectId);
			newStar.setUserId(userId);
			starRepository.save(newStar);
			p.setStarCount(p.getStarCount() + 1);
		}
		projectRepository.save(p);
	}

	@Override
	public Project getProjectById(int projectId) {
		return projectRepository.findById(projectId)
				.orElseThrow(() -> new RuntimeException("Project ID " + projectId + " not found"));
	}

	@Override
	public boolean canViewProject(int projectId, Integer userId) {
		Project project = getProjectById(projectId);
		if ("PUBLIC".equalsIgnoreCase(project.getVisibility())) {
			return true;
		}
		if (userId == null) {
			return false;
		}
		if (project.getOwnerId() == userId) {
			return true;
		}
		return memberRepository.existsByProjectIdAndUserIdAndStatus(projectId, userId, "APPROVED");
	}

	@Override
	@Transactional
	public Project forkProject(int sourceId, int newOwnerId, String newOwnerUsername) {
		Project source = getProjectById(sourceId);
		if (!"PUBLIC".equals(source.getVisibility())) {
			throw new RuntimeException("Collaboration Error: Only public projects can be forked.");
		}
		enforceSubscriptionOrConsumeCredit(newOwnerId);

		Project forked = Project.builder().name(source.getName() + "-fork")
				.description("Forked from " + source.getName()).ownerId(newOwnerId).ownerUsername(newOwnerUsername)
				.language(source.getLanguage())
				.visibility("PRIVATE").createdAt(LocalDateTime.now()).isArchived(false).build();

		Project savedFork = projectRepository.save(forked);

		// Synchronous inter-service call to File-Service for deep cloning
		try {
			String fileServiceUrl = "http://file-service/api/v1/files/clone?sourceId=" + sourceId + "&targetId="
					+ savedFork.getProjectId();
			restTemplate.postForEntity(fileServiceUrl, null, Void.class);
		} catch (Exception e) {
			throw new RuntimeException("Filesystem cloning failed for forked project: " + e.getMessage());
		}

		source.setForkCount(source.getForkCount() + 1);
		projectRepository.save(source);

		if (source.getOwnerId() != newOwnerId) {
			sendProjectAccessNotification(source, source.getOwnerId(), source.getOwnerUsername(), null, newOwnerId,
					newOwnerUsername, "PROJECT_FORKED",
					(newOwnerUsername != null ? newOwnerUsername : "A user") + " forked your project \""
							+ source.getName() + "\".");
		}
		return savedFork;
	}

	@Override
	public void requestCollaboration(int projectId, int userId, String username) {
		Project project = getProjectById(projectId);
		if (project.getOwnerId() == userId)
			return;

		Optional<ProjectMember> existing = memberRepository.findByProjectIdAndUserId(projectId, userId);
		if (existing.isPresent()) {
			ProjectMember member = existing.get();
			member.setUsername(username);
			if (!"APPROVED".equalsIgnoreCase(resolveStatus(member))) {
				member.setStatus("PENDING");
				member.setCanEdit(false);
			}
			memberRepository.save(member);
			return;
		}

		enforceSubscriptionOrConsumeCredit(userId);
		ProjectMember request = new ProjectMember();
		request.setProjectId(projectId);
		request.setUserId(userId);
		request.setUsername(username);
		request.setStatus("PENDING");
		request.setCanEdit(false);
		request.setRole("PENDING");
		memberRepository.save(request);
	}

	@Override
	public boolean hasEditAccess(int projectId, int userId) {
		Project project = projectRepository.findById(projectId).orElse(null);
		if (project != null && project.getOwnerId() == userId)
			return true;
		return memberRepository.existsByProjectIdAndUserIdAndStatusAndCanEditTrue(projectId, userId, "APPROVED");
	}

	@Override
	public ProjectPermissionResponse getProjectPermissions(int projectId, Integer userId) {
		Project project = getProjectById(projectId);
		ProjectMember member = userId == null ? null : memberRepository.findByProjectIdAndUserId(projectId, userId).orElse(null);
		String status = resolveStatus(member);
		boolean isOwner = userId != null && project.getOwnerId() == userId;
		boolean isMember = member != null && "APPROVED".equalsIgnoreCase(status);
		boolean canEdit = isOwner || (member != null && "APPROVED".equalsIgnoreCase(status) && member.isCanEdit());
		boolean canView = "PUBLIC".equalsIgnoreCase(project.getVisibility()) || isOwner || isMember;
		String role = isOwner ? "OWNER" : canEdit ? "EDITOR" : isMember ? "MEMBER" : "PUBLIC_USER";

		return ProjectPermissionResponse.builder().projectId(projectId).userId(userId).canView(canView).canEdit(canEdit)
				.owner(isOwner).member(isMember).visibility(project.getVisibility()).membershipStatus(status)
				.projectRole(role).build();
	}

	// Remaining methods (update, delete, archive, getMembers) remain logically same
	// but cleaned
	@Override
	public void archiveProject(int id) {
		Project p = getProjectById(id);
		p.setArchived(true);
		projectRepository.save(p);
	}

	@Override
	public void deleteProject(int id) {
		projectRepository.deleteById(id);
	}

	@Override
	public List<ProjectMember> getProjectMembers(int id) {
		List<ProjectMember> members = memberRepository.findByProjectId(id).stream()
				.filter(m -> "APPROVED".equalsIgnoreCase(resolveStatus(m)))
				.collect(Collectors.toList());

		Project project = getProjectById(id);
		if (project != null) {
			ProjectMember owner = new ProjectMember();
			owner.setProjectId(id);
			owner.setUserId(project.getOwnerId());
			owner.setUsername(project.getOwnerUsername() != null ? project.getOwnerUsername() : "Owner");
			owner.setRole("OWNER");
			owner.setStatus("APPROVED");
			owner.setCanEdit(true);
			members.add(0, owner);
		}
		
		return members;
	}

	@Override
	public void approveCollaborator(int pId, int uId) {
		memberRepository.findByProjectIdAndUserId(pId, uId).ifPresent(m -> {
			m.setStatus("APPROVED");
			m.setCanEdit(false);
			m.setRole("APPROVED");
			memberRepository.save(m);
			Project project = getProjectById(pId);
			sendProjectAccessNotification(project, m.getUserId(), m.getUsername(), null, project.getOwnerId(),
					project.getOwnerUsername(), "ACCESS_GRANTED",
					"You have been added as a collaborator to project \"" + project.getName() + "\".");
		});
	}

	@Override
	public List<ProjectMember> getPendingRequests(int id) {
		return memberRepository.findByProjectId(id).stream().filter(m -> "PENDING".equalsIgnoreCase(resolveStatus(m)))
				.collect(Collectors.toList());
	}

	@Override
	public void removeProjectMember(int pId, int uId) {
		memberRepository.findByProjectIdAndUserId(pId, uId).ifPresent(memberRepository::delete);
	}

	@Override
	public void updateMemberEditPermission(int projectId, int userId, boolean canEdit) {
		ProjectMember member = memberRepository.findByProjectIdAndUserId(projectId, userId)
				.orElseThrow(() -> new RuntimeException("Project member not found"));
		if (!"APPROVED".equalsIgnoreCase(resolveStatus(member))) {
			throw new RuntimeException("Edit permission can only be changed for approved collaborators.");
		}
		member.setCanEdit(canEdit);
		memberRepository.save(member);
		Project project = getProjectById(projectId);
		sendProjectAccessNotification(project, member.getUserId(), member.getUsername(), null, project.getOwnerId(),
				project.getOwnerUsername(),
				canEdit ? "EDIT_ACCESS_GRANTED" : "EDIT_ACCESS_REVOKED",
				canEdit ? "You have been granted edit access to project \"" + project.getName() + "\"."
						: "Your edit access has been revoked for project \"" + project.getName() + "\".");
	}

	@Override
	public void grantEditAccessByEmail(int projectId, String email) {
		if (email == null || email.isBlank()) {
			throw new RuntimeException("Email is required.");
		}
		UserSummaryResponse user = restTemplate.getForObject(
				"http://auth-service/api/v1/auth/by-email?email="
						+ org.springframework.web.util.UriUtils.encodeQueryParam(email.trim(), StandardCharsets.UTF_8),
				UserSummaryResponse.class);
		if (user == null || user.getUserId() == null) {
			throw new RuntimeException("No user found with this email.");
		}

		Project project = getProjectById(projectId);
		if (project.getOwnerId() == user.getUserId()) {
			throw new RuntimeException("Owner already has full edit access.");
		}

		ProjectMember member = memberRepository.findByProjectIdAndUserId(projectId, user.getUserId()).orElseGet(() -> {
			ProjectMember newMember = new ProjectMember();
			newMember.setProjectId(projectId);
			newMember.setUserId(user.getUserId());
			return newMember;
		});

		member.setUsername(user.getUsername());
		member.setStatus("APPROVED");
		member.setCanEdit(true);
		member.setRole("EDITOR");
		memberRepository.save(member);
		sendProjectAccessNotification(project, user.getUserId(), user.getUsername(), user.getEmail(),
				project.getOwnerId(), project.getOwnerUsername(),
				"EDIT_ACCESS_GRANTED", "You have been granted edit access to project \"" + project.getName() + "\".");
	}

	@Override
	public List<Project> getProjectsByLanguage(String lang) {
		return projectRepository.findByLanguage(lang);
	}

	@Override
	public List<Project> searchProjects(String name, int uid) {
		List<Project> projects = projectRepository.findByNameContainingIgnoreCase(name);
		applyStarStatus(projects, uid);
		return projects;
	}

	@Override
	public Project updateProject(int id, Project details) {
		Project p = getProjectById(id);
		if (details.getName() != null) {
			p.setName(details.getName());
		}
		if (details.getDescription() != null) {
			p.setDescription(details.getDescription());
		}
		if (details.getVisibility() != null) {
			p.setVisibility(details.getVisibility());
		}
		return projectRepository.save(p);
	}

	private String resolveStatus(ProjectMember member) {
		if (member == null) {
			return null;
		}
		if (member.getStatus() != null && !member.getStatus().isBlank()) {
			return member.getStatus();
		}
		return "EDITOR".equalsIgnoreCase(member.getRole()) ? "APPROVED" : "PENDING";
	}

	private void sendProjectAccessNotification(Project project, Integer recipientId, String recipientName,
			String recipientEmail, Integer senderId, String senderName, String type, String message) {
		if (recipientId == null) {
			return;
		}
		try {
			notificationProducer.send(recipientId, senderId, senderName, recipientEmail, (long) project.getProjectId(),
					type, message, project.getName(), recipientName);
		} catch (Exception ignored) {
			// Queue delivery should not block the main action.
		}
		try {
			NotificationRequest notification = NotificationRequest.builder().recipientId(recipientId).senderId(senderId)
					.senderName(senderName).relatedId((long) project.getProjectId()).message(message).type(type)
					.projectName(project.getName()).recipientName(recipientName).build();
			String url = "http://notification-service/api/v1/notifications/send";
			if (recipientEmail != null && !recipientEmail.isBlank()) {
				url += "?emails=" + org.springframework.web.util.UriUtils.encodeQueryParam(recipientEmail,
						StandardCharsets.UTF_8);
			}
			restTemplate.postForEntity(url, notification, Void.class);
		} catch (Exception ignored) {
			// HTTP fallback should not block the main action.
		}
	}
}
