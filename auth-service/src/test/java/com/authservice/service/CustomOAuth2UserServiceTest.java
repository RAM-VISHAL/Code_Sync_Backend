package com.authservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.authservice.entity.User;
import com.authservice.repository.UserRepository;
import com.authservice.serviceImpl.CustomOAuth2UserService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CustomOAuth2UserServiceTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private EmailService emailService;

	@InjectMocks
	private CustomOAuth2UserService customOAuth2UserService;

	@Test
	void processOAuth2User_existingUserUpdatesProviderAndReturnsCodesyncUsername() {
		User existing = new User();
		existing.setUserId(7);
		existing.setEmail("dev@codesync.com");
		existing.setUsername("dev_user");
		existing.setProvider("LOCAL");

		when(userRepository.findByEmail("dev@codesync.com")).thenReturn(Optional.of(existing));
		when(userRepository.save(existing)).thenReturn(existing);

		OAuth2User oauthUser = new DefaultOAuth2User(List.of(new SimpleGrantedAuthority("ROLE_USER")),
				Map.of("email", "dev@codesync.com", "name", "Developer"), "email");

		OAuth2User result = ReflectionTestUtils.invokeMethod(customOAuth2UserService, "processOAuth2User", oauthUser,
				"GOOGLE");

		assertNotNull(result);
		assertEquals("dev_user", result.getName());
		assertEquals("GOOGLE", existing.getProvider());
		verify(userRepository).save(existing);
	}

	@Test
	void processOAuth2User_newGithubUserCreatesAccountAndSendsWelcomeEmail() {
		when(userRepository.findByEmail("octocat@github.com")).thenReturn(Optional.empty());
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		OAuth2User oauthUser = new DefaultOAuth2User(List.of(new SimpleGrantedAuthority("ROLE_USER")),
				Map.of("login", "octocat", "name", "Octo Cat"), "login");

		OAuth2User result = ReflectionTestUtils.invokeMethod(customOAuth2UserService, "processOAuth2User", oauthUser,
				"GITHUB");

		assertNotNull(result);
		assertEquals("octocat_github", result.getName());
		verify(userRepository).save(any(User.class));
		verify(emailService, timeout(1000)).sendWelcomeEmail(eq("octocat@github.com"), eq("octocat_github"));
	}

	@Test
	void processOAuth2User_missingEmailThrowsAuthenticationException() {
		OAuth2User oauthUser = new DefaultOAuth2User(List.of(new SimpleGrantedAuthority("ROLE_USER")),
				Map.of("name", "No Email User"), "name");

		assertThrows(OAuth2AuthenticationException.class, () -> ReflectionTestUtils
				.invokeMethod(customOAuth2UserService, "processOAuth2User", oauthUser, "GOOGLE"));
	}
}
