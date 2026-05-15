package com.authservice.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.authservice.entity.User;
import com.authservice.repository.UserRepository;
import com.authservice.serviceImpl.AuthServiceImpl;

/**
 * Unit Tests for AuthServiceImpl Ensures core business logic for registration
 * and login is flaw-free.
 */
@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private EmailService emailService;

	@InjectMocks
	private AuthServiceImpl authService;

	private User testUser;

	@BeforeEach
	void setUp() {
		testUser = new User();
		testUser.setUserId(1);
		testUser.setUsername("abhay_dev");
		testUser.setEmail("abhay@codesync.com");
		testUser.setPasswordHash("StrongPass1!");
	}

	@Test
	void testRegisterUser_Success() {
		// Arrange: Setup mock behavior for a new user registration
		when(userRepository.existsByEmail(anyString())).thenReturn(false);
		when(userRepository.findByUsername(anyString())).thenReturn(java.util.Optional.empty());
		@SuppressWarnings("unchecked")
		java.util.Map<String, String> registrationOtps = (java.util.Map<String, String>) ReflectionTestUtils
				.getField(AuthServiceImpl.class, "registrationOtps");
		registrationOtps.put(testUser.getEmail(), "123456");
		when(passwordEncoder.encode("StrongPass1!")).thenReturn("hashedPassword");
		when(userRepository.save(any(User.class))).thenReturn(testUser);

		// Act: Call the service method
		User savedUser = authService.register(testUser, "123456");

		// Assert: Verify outcomes and security requirements
		assertNotNull(savedUser);
		assertEquals("hashedPassword", testUser.getPasswordHash()); // Ensure password was hashed
		verify(userRepository, times(1)).save(any(User.class));
	}

	@Test
	void testRegisterUser_DuplicateEmail_ShouldThrowException() {
		// Arrange: OTP is valid, but email becomes duplicate before final registration
		when(userRepository.existsByEmail("abhay@codesync.com")).thenReturn(true);
		@SuppressWarnings("unchecked")
		java.util.Map<String, String> registrationOtps = (java.util.Map<String, String>) ReflectionTestUtils
				.getField(AuthServiceImpl.class, "registrationOtps");
		registrationOtps.put(testUser.getEmail(), "123456");

		// Act & Assert: Verify our custom error message is thrown
		RuntimeException exception = assertThrows(RuntimeException.class, () -> {
			authService.register(testUser, "123456");
		});

		assertEquals("Email already exists", exception.getMessage());
		verify(userRepository, never()).save(any(User.class)); // Ensure no data was saved
	}

	@Test
	void testLogin_UserNotFound_ShouldThrowException() {
		// Arrange
		when(userRepository.findByUsername("unknown_user")).thenReturn(java.util.Optional.empty());

		// Act & Assert
		RuntimeException exception = assertThrows(RuntimeException.class, () -> {
			authService.login("unknown_user", "password");
		});

		assertEquals("Invalid username or password", exception.getMessage());
	}
}
