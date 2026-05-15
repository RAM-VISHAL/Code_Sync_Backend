package com.projectservice.serviceTest;

import com.projectservice.entity.Project;
import com.projectservice.repository.ProjectRepository;
import com.projectservice.serviceImpl.ProjectServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProjectServiceTest {

	@Mock
	private ProjectRepository projectRepository;

	@Mock
	private RestTemplate restTemplate;

	@InjectMocks
	private ProjectServiceImpl projectService;

	@Test
	void testForkProject_PrivateProject_ShouldFail() {
		// Arrange
		Project privateProject = new Project();
		privateProject.setProjectId(1);
		privateProject.setVisibility("PRIVATE");

		when(projectRepository.findById(1)).thenReturn(Optional.of(privateProject));

		// Act & Assert
		RuntimeException ex = assertThrows(RuntimeException.class, () -> {
			projectService.forkProject(1, 99, "OWNER Name");
		});

		assertTrue(ex.getMessage().contains("Only public projects can be forked"));
	}
}