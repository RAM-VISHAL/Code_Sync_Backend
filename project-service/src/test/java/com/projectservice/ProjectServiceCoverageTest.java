package com.projectservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.projectservice.config.ProjectNotificationProducer;
import com.projectservice.dto.ProjectPermissionResponse;
import com.projectservice.dto.UserSummaryResponse;
import com.projectservice.entity.Project;
import com.projectservice.entity.ProjectMember;
import com.projectservice.entity.ProjectStar;
import com.projectservice.exception.GlobalExceptionHandler;
import com.projectservice.repository.ProjectMemberRepository;
import com.projectservice.repository.ProjectRepository;
import com.projectservice.repository.ProjectStarRepository;
import com.projectservice.resource.ProjectResource;
import com.projectservice.service.ProjectService;
import com.projectservice.serviceImpl.ProjectServiceImpl;

class ProjectServiceCoverageTest {

	private ProjectRepository projectRepository;
	private ProjectStarRepository starRepository;
	private ProjectMemberRepository memberRepository;
	private RestTemplate restTemplate;
	private ProjectNotificationProducer notificationProducer;
	private ProjectServiceImpl projectService;

	@BeforeEach
	void setUp() {
		projectRepository = org.mockito.Mockito.mock(ProjectRepository.class);
		starRepository = org.mockito.Mockito.mock(ProjectStarRepository.class);
		memberRepository = org.mockito.Mockito.mock(ProjectMemberRepository.class);
		restTemplate = org.mockito.Mockito.mock(RestTemplate.class);
		notificationProducer = org.mockito.Mockito.mock(ProjectNotificationProducer.class);

		projectService = new ProjectServiceImpl();
		ReflectionTestUtils.setField(projectService, "projectRepository", projectRepository);
		ReflectionTestUtils.setField(projectService, "starRepository", starRepository);
		ReflectionTestUtils.setField(projectService, "memberRepository", memberRepository);
		ReflectionTestUtils.setField(projectService, "restTemplate", restTemplate);
		ReflectionTestUtils.setField(projectService, "notificationProducer", notificationProducer);
	}

	@AfterEach
	void clearRequestContext() {
		RequestContextHolder.resetRequestAttributes();
	}

	@Test
	void serviceCoversCreationListsAndUpdates() {
		Project project = Project.builder().projectId(1).ownerId(7).name("CodeSync").visibility("PUBLIC").build();
		when(restTemplate.getForObject("http://payment-service/api/v1/payments/status/7", Map.class))
				.thenReturn(Map.of("isSubscribed", true));
		when(projectRepository.save(project)).thenReturn(project);
		when(projectRepository.findByOwnerId(7)).thenReturn(List.of(project));
		when(projectRepository.findByVisibility("PUBLIC")).thenReturn(List.of(project));
		when(projectRepository.findByNameContainingIgnoreCase("code")).thenReturn(List.of(project));
		when(starRepository.findStarredProjectIds(List.of(1), 7)).thenReturn(Set.of(1));
		when(projectRepository.findById(1)).thenReturn(Optional.of(project));

		assertEquals(project, projectService.createProject(project));
		assertTrue(projectService.getProjectsByOwner(7).get(0).isStarredByMe());
		assertTrue(projectService.getPublicProjects(7).get(0).isStarredByMe());
		assertTrue(projectService.searchProjects("code", 7).get(0).isStarredByMe());
		assertEquals(project, projectService.getProjectById(1));

		Project details = new Project();
		details.setName("Updated");
		details.setDescription("Desc");
		details.setVisibility("PRIVATE");
		when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));
		Project updated = projectService.updateProject(1, details);
		assertEquals("Updated", updated.getName());
		assertEquals("Desc", updated.getDescription());
		assertEquals("PRIVATE", updated.getVisibility());
	}

	@Test
	void serviceCoversStarPermissionsArchiveAndDelete() {
		Project project = Project.builder().projectId(1).ownerId(7).visibility("PRIVATE").starCount(1).build();
		ProjectStar star = new ProjectStar();
		when(projectRepository.findById(1)).thenReturn(Optional.of(project));
		when(starRepository.findByProjectIdAndUserId(1, 7)).thenReturn(Optional.of(star), Optional.empty());
		when(memberRepository.existsByProjectIdAndUserIdAndStatus(1, 8, "APPROVED")).thenReturn(true);
		when(memberRepository.existsByProjectIdAndUserIdAndStatusAndCanEditTrue(1, 8, "APPROVED")).thenReturn(true);
		when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));

		projectService.starProject(1, 7);
		assertEquals(0, project.getStarCount());
		projectService.starProject(1, 7);
		assertEquals(1, project.getStarCount());

		assertTrue(projectService.canViewProject(1, 7));
		assertTrue(projectService.canViewProject(1, 8));
		assertFalse(projectService.canViewProject(1, null));
		assertTrue(projectService.hasEditAccess(1, 7));
		assertTrue(projectService.hasEditAccess(1, 8));

		projectService.archiveProject(1);
		assertTrue(project.isArchived());
		projectService.deleteProject(1);
		verify(projectRepository).deleteById(1);
	}

	@Test
	void serviceCoversMembersPendingAndPermissions() {
		Project project = Project.builder().projectId(1).ownerId(7).ownerUsername("owner").name("Repo").visibility("PUBLIC").build();
		ProjectMember approved = new ProjectMember();
		approved.setProjectId(1);
		approved.setUserId(8);
		approved.setUsername("editor");
		approved.setStatus("APPROVED");
		approved.setCanEdit(true);

		ProjectMember pending = new ProjectMember();
		pending.setProjectId(1);
		pending.setUserId(9);
		pending.setUsername("viewer");
		pending.setStatus("PENDING");

		when(projectRepository.findById(1)).thenReturn(Optional.of(project));
		when(memberRepository.findByProjectId(1)).thenReturn(List.of(approved, pending));
		when(memberRepository.findByProjectIdAndUserId(1, 8)).thenReturn(Optional.of(approved));
		when(memberRepository.findByProjectIdAndUserId(1, 9)).thenReturn(Optional.of(pending));
		when(memberRepository.save(any(ProjectMember.class))).thenAnswer(invocation -> invocation.getArgument(0));

		List<ProjectMember> members = projectService.getProjectMembers(1);
		assertEquals(2, members.size());
		assertEquals("OWNER", members.get(0).getRole());
		assertEquals(1, projectService.getPendingRequests(1).size());

		ProjectPermissionResponse ownerPermissions = projectService.getProjectPermissions(1, 7);
		ProjectPermissionResponse memberPermissions = projectService.getProjectPermissions(1, 8);
		assertTrue(ownerPermissions.isOwner());
		assertTrue(memberPermissions.isCanEdit());

		projectService.approveCollaborator(1, 9);
		assertEquals("APPROVED", pending.getStatus());
		projectService.removeProjectMember(1, 9);
		verify(memberRepository).delete(pending);
	}

	@Test
	void serviceCoversCollaborationAndGrantByEmail() {
		Project project = Project.builder().projectId(1).ownerId(7).ownerUsername("owner").name("Repo").visibility("PUBLIC").build();
		ProjectMember member = new ProjectMember();
		member.setProjectId(1);
		member.setUserId(8);
		member.setUsername("editor");
		member.setStatus("APPROVED");
		member.setCanEdit(false);

		when(projectRepository.findById(1)).thenReturn(Optional.of(project));
		when(restTemplate.getForObject("http://payment-service/api/v1/payments/status/9", Map.class))
				.thenReturn(Map.of("isSubscribed", true));
		when(memberRepository.findByProjectIdAndUserId(1, 9)).thenReturn(Optional.empty());
		when(memberRepository.findByProjectIdAndUserId(1, 8)).thenReturn(Optional.of(member));
		when(memberRepository.save(any(ProjectMember.class))).thenAnswer(invocation -> invocation.getArgument(0));
		UserSummaryResponse userSummary = new UserSummaryResponse();
		userSummary.setUserId(8);
		userSummary.setUsername("editor");
		userSummary.setEmail("editor@example.com");
		when(restTemplate.getForObject(org.mockito.ArgumentMatchers.contains("http://auth-service/api/v1/auth/by-email"),
				eq(UserSummaryResponse.class))).thenReturn(userSummary);

		projectService.requestCollaboration(1, 9, "new-user");
		projectService.updateMemberEditPermission(1, 8, true);
		assertTrue(member.isCanEdit());
		projectService.grantEditAccessByEmail(1, "editor@example.com");
		assertEquals("EDITOR", member.getRole());
	}

	@Test
	void serviceCoversForkAndValidationPaths() {
		Project source = Project.builder().projectId(1).ownerId(7).ownerUsername("owner").name("Repo").language("java")
				.visibility("PUBLIC").forkCount(0).build();
		Project forked = Project.builder().projectId(2).ownerId(9).name("Repo-fork").visibility("PRIVATE").build();

		when(projectRepository.findById(1)).thenReturn(Optional.of(source));
		when(restTemplate.getForObject("http://payment-service/api/v1/payments/status/9", Map.class))
				.thenReturn(Map.of("isSubscribed", true));
		when(projectRepository.save(any(Project.class))).thenReturn(forked, source);
		when(restTemplate.postForEntity(org.mockito.ArgumentMatchers.contains("http://file-service/api/v1/files/clone"), eq(null), eq(Void.class)))
				.thenReturn(ResponseEntity.ok().build());

		Project result = projectService.forkProject(1, 9, "forker");
		assertEquals(2, result.getProjectId());

		Project privateProject = Project.builder().projectId(3).visibility("PRIVATE").build();
		when(projectRepository.findById(3)).thenReturn(Optional.of(privateProject));
		assertThrows(RuntimeException.class, () -> projectService.forkProject(3, 9, "forker"));
		assertThrows(RuntimeException.class, () -> projectService.grantEditAccessByEmail(1, " "));
	}

	@Test
	void resourceDelegatesAndAppliesAuthorizationChecks() {
		ProjectService service = org.mockito.Mockito.mock(ProjectService.class);
		ProjectResource resource = new ProjectResource();
		ReflectionTestUtils.setField(resource, "projectService", service);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("X-Authenticated-UserId", "7");
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

		Project project = Project.builder().projectId(1).ownerId(7).name("Repo").build();
		ProjectPermissionResponse ownerPermission = ProjectPermissionResponse.builder().owner(true).build();
		ProjectMember member = new ProjectMember();

		when(service.createProject(any(Project.class))).thenReturn(project);
		when(service.getProjectsByOwner(7)).thenReturn(List.of(project));
		when(service.getPublicProjects(7)).thenReturn(List.of(project));
		when(service.searchProjects("Repo", 7)).thenReturn(List.of(project));
		when(service.canViewProject(1, 7)).thenReturn(true);
		when(service.getProjectById(1)).thenReturn(project);
		when(service.getProjectPermissions(1, 7)).thenReturn(ownerPermission);
		when(service.forkProject(1, 7, "user")).thenReturn(project);
		when(service.updateProject(1, project)).thenReturn(project);
		when(service.hasEditAccess(1, 7)).thenReturn(true);
		when(service.getPendingRequests(1)).thenReturn(List.of(member));
		when(service.getProjectMembers(1)).thenReturn(List.of(member));

		assertEquals(project, resource.createProject(project).getBody());
		assertEquals(List.of(project), resource.getByOwner(7).getBody());
		assertEquals(List.of(project), resource.getPublic(null).getBody());
		assertEquals(204, resource.toggleStar(1, 99).getStatusCode().value());
		assertEquals(List.of(project), resource.search("Repo", 7).getBody());
		assertEquals(project, resource.getById(1).getBody());
		assertEquals(ownerPermission, resource.getMyPermissions(1).getBody());
		assertEquals(project, resource.fork(1, 99, "user").getBody());
		assertEquals(project, resource.updateProject(1, project).getBody());
		assertEquals(200, resource.requestAccess(1, 99, "user").getStatusCode().value());
		assertEquals(200, resource.approveAccess(1, 8).getStatusCode().value());
		assertEquals(Boolean.TRUE, resource.checkEditAccess(1, 99).getBody());
		assertEquals(1, resource.getPendingRequests(1).getBody().size());
		assertEquals(1, resource.getMembers(1).getBody().size());
		assertEquals(204, resource.removeMember(1, 8).getStatusCode().value());
		assertEquals(204, resource.updateEditPermission(1, 8, Map.of("canEdit", true)).getStatusCode().value());
		assertEquals(204, resource.grantEditByEmail(1, Map.of("email", "a@example.com")).getStatusCode().value());
		assertEquals(204, resource.archive(1).getStatusCode().value());
		assertEquals(204, resource.delete(1).getStatusCode().value());
	}

	@Test
	void resourceRejectsMissingAuthenticationAndUnauthorizedReads() {
		ProjectService service = org.mockito.Mockito.mock(ProjectService.class);
		ProjectResource resource = new ProjectResource();
		ReflectionTestUtils.setField(resource, "projectService", service);

		RequestContextHolder.resetRequestAttributes();
		assertThrows(RuntimeException.class, () -> resource.createProject(new Project()));

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("X-Authenticated-UserId", "7");
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
		when(service.canViewProject(1, 7)).thenReturn(false);
		when(service.getProjectPermissions(1, 7)).thenReturn(ProjectPermissionResponse.builder().owner(false).build());

		assertThrows(RuntimeException.class, () -> resource.getById(1));
		assertThrows(RuntimeException.class, () -> resource.getByOwner(8));
		assertThrows(RuntimeException.class, () -> resource.archive(1));
		verify(service, never()).archiveProject(1);
	}

	@Test
	void globalExceptionHandlerAndApplicationMainAreCovered() {
		GlobalExceptionHandler handler = new GlobalExceptionHandler();
		assertEquals("boom", handler.handleRuntimeException(new RuntimeException("boom")).getBody().get("message"));
		assertEquals("An unexpected project error occurred.",
				handler.handleGeneralException(new Exception("x")).getBody().get("message"));

		try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
			ProjectServiceApplication.main(new String[] { "test" });
			springApplication.verify(() -> SpringApplication.run(ProjectServiceApplication.class, new String[] { "test" }));
		}
	}
}
