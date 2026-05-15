package com.authservice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import com.authservice.config.JwtUtils;
import com.authservice.controller.DebugController;
import com.authservice.entity.Role;
import com.authservice.entity.User;
import com.authservice.exception.GlobalExceptionHandler;
import com.authservice.repository.UserRepository;
import com.authservice.resource.AdminUserController;
import com.authservice.resource.AuthResource;
import com.authservice.service.AuthService;

class AuthServiceCoverageTest {

	private AuthService authService;
	private JwtUtils jwtUtils;
	private AuthResource authResource;
	private User user;

	@BeforeEach
	void setUp() {
		authService = org.mockito.Mockito.mock(AuthService.class);
		jwtUtils = new JwtUtils();
		ReflectionTestUtils.setField(jwtUtils, "jwtSecret", "12345678901234567890123456789012");
		ReflectionTestUtils.setField(jwtUtils, "tokenExpiry", 60000L);

		authResource = new AuthResource();
		ReflectionTestUtils.setField(authResource, "authService", authService);
		ReflectionTestUtils.setField(authResource, "jwtUtils", jwtUtils);

		user = new User();
		user.setUserId(7);
		user.setUsername("raghav");
		user.setEmail("raghav@example.com");
		user.setRole(Role.DEVELOPER);
		user.setAvatarUrl("avatar.png");
		user.setFullName("Raghav");
		user.setFreeCreditsRemaining(3);
	}

	@Test
	void authResourceHandlesRegistrationLoginAndProfileFlows() {
		when(authService.register(user, "123456")).thenReturn(user);
		when(authService.getByUsername("raghav")).thenReturn(user);
		when(authService.getUserById(7)).thenReturn(user);
		when(authService.getUserByEmail("raghav@example.com")).thenReturn(user);
		when(authService.getFreeCreditsRemaining(7)).thenReturn(3);
		when(authService.consumeFreeCredits(7, 2)).thenReturn(1);
		when(authService.updateProfile(7, user)).thenReturn(user);
		when(authService.searchUsers("rag")).thenReturn(List.of(user));

		assertEquals(user, authResource.registerUser(user, "123456").getBody());
		assertEquals("raghav", ((Map<?, ?>) authResource.login(user).getBody()).get("username"));
		assertEquals("raghav", ((Map<?, ?>) authResource.getCurrentUser("Bearer abc", "raghav", null).getBody()).get("username"));
		assertEquals(user, authResource.getProfile(7).getBody());
		assertEquals("raghav", ((com.authservice.dto.UserSummaryResponse) authResource.getByEmail("raghav@example.com").getBody()).getUsername());
		assertEquals(3, ((Map<?, ?>) authResource.getCredits(7).getBody()).get("freeCreditsRemaining"));
		assertEquals(1, ((Map<?, ?>) authResource.consumeCredits(7, 2).getBody()).get("freeCreditsRemaining"));
		assertEquals(user, authResource.updateProfile(7, user).getBody());
		assertEquals(List.of(user), authResource.searchUsers("rag").getBody());

		verify(authService).login(user.getUsername(), user.getPasswordHash());
	}

	@Test
	void authResourceHandlesOtpAndUnauthenticatedFlows() {
		assertEquals("Registration OTP sent successfully",
				((Map<?, ?>) authResource.sendRegistrationOtp(Map.of("email", "a@b.com", "username", "a")).getBody()).get("message"));
		assertEquals("OTP sent successfully",
				((Map<?, ?>) authResource.sendOtp(Map.of("email", "a@b.com")).getBody()).get("message"));
		assertEquals("Password reset successfully",
				((Map<?, ?>) authResource.resetPassword(Map.of("email", "a@b.com", "otp", "123", "newPassword", "x")).getBody()).get("message"));
		assertEquals(401, authResource.getCurrentUser(null, null, null).getStatusCode().value());
		assertEquals(404, authResource.getByEmail("missing@example.com").getStatusCode().value());
	}

	@Test
	void authResourceUsesAuthenticationFallback() {
		when(authService.getUserByEmail("oauth@example.com")).thenReturn(user);
		TestingAuthenticationToken authentication = new TestingAuthenticationToken(
				new DefaultOAuth2User(List.of(new OAuth2UserAuthority(Map.of("email", "oauth@example.com"))), Map.of("email", "oauth@example.com"), "email"),
				null);
		authentication.setAuthenticated(true);

		Map<?, ?> body = (Map<?, ?>) authResource.getCurrentUser(null, null, authentication).getBody();
		assertEquals("raghav", body.get("username"));
		assertNotNull(body.get("token"));
	}

	@Test
	void adminAndDebugControllersWork() {
		UserRepository userRepository = org.mockito.Mockito.mock(UserRepository.class);
		AdminUserController adminUserController = new AdminUserController();
		ReflectionTestUtils.setField(adminUserController, "userRepository", userRepository);
		when(userRepository.findAll()).thenReturn(List.of(user));

		assertEquals(List.of(user), adminUserController.getAllUsers().getBody());
		assertEquals("User removed from platform", adminUserController.banUser(7).getBody());
		verify(userRepository).deleteById(7);

		DebugController debugController = new DebugController();
		ReflectionTestUtils.setField(debugController, "googleId", "abcde12345");
		Map<String, String> debugBody = debugController.check();
		assertEquals("Auth Service is ALIVE", debugBody.get("status"));
		assertEquals("true", debugBody.get("googleIdPresent"));
		assertEquals("abcde", debugBody.get("googleIdStart"));
	}

	@Test
	void jwtUtilsGeneratesAndValidatesTokens() {
		String token = jwtUtils.generateToken("raghav", 7, "USER");
		assertTrue(jwtUtils.validateToken(token));
		assertEquals("raghav", jwtUtils.getUsernameFromToken(token));
		assertFalse(jwtUtils.validateToken(token + "broken"));
	}

	@Test
	void globalExceptionHandlerBuildsExpectedPayloads() {
		GlobalExceptionHandler handler = new GlobalExceptionHandler();
		assertEquals("error", handler.handleRuntimeException(new RuntimeException("bad")).getBody().get("status"));
		assertEquals("bad", handler.handleRuntimeException(new RuntimeException("bad")).getBody().get("message"));
	}

	@Test
	void applicationMainDelegatesToSpringApplication() {
		try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
			AuthServiceApplication.main(new String[] { "test" });
			springApplication.verify(() -> SpringApplication.run(AuthServiceApplication.class, new String[] { "test" }));
		}
	}
}
