package com.authservice.resource;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import com.authservice.config.JwtUtils;
import com.authservice.dto.UserSummaryResponse;
import com.authservice.entity.User;
import com.authservice.service.AuthService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Auth Controller - Handles Identity Traffic
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthResource {

	@Autowired
	private AuthService authService;

	@Autowired
	private JwtUtils jwtUtils;

	@PostMapping("/register")
	public ResponseEntity<User> registerUser(@Valid @RequestBody User user, @RequestParam("otp") String otp) {
		return ResponseEntity.ok(authService.register(user, otp));
	}

	@PostMapping("/send-registration-otp")
	public ResponseEntity<?> sendRegistrationOtp(@RequestBody Map<String, String> request) {
		try {
			authService.sendRegistrationOtp(request.get("email"), request.get("username"));
			return ResponseEntity.ok(Map.of("message", "Registration OTP sent successfully"));
		} catch (Exception e) {
			return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
		}
	}

	@PostMapping("/login")
	public ResponseEntity<?> login(@RequestBody User loginRequest) {
		authService.login(loginRequest.getUsername(), loginRequest.getPasswordHash());
		User user = authService.getByUsername(loginRequest.getUsername());
		String token = jwtUtils.generateToken(user.getUsername(), user.getUserId(), user.getRole().name());

		Map<String, Object> response = new HashMap<>();
		response.put("token", token);
		response.put("userId", user.getUserId());
		response.put("username", user.getUsername());
		response.put("email", user.getEmail());
		response.put("role", user.getRole());
		response.put("avatarUrl", user.getAvatarUrl());
		response.put("fullName", user.getFullName());
		response.put("freeCreditsRemaining", user.getFreeCreditsRemaining());

		return ResponseEntity.ok(response);
	}

	@GetMapping("/me")
	public ResponseEntity<?> getCurrentUser(@RequestHeader(value = "Authorization", required = false) String authHeader,
			@RequestHeader(value = "X-Authenticated-User", required = false) String forwardedUsername,
			Authentication authentication) {
		if (forwardedUsername != null && !forwardedUsername.isBlank()) {
			User user = authService.getByUsername(forwardedUsername);
			String token = (authHeader != null && authHeader.startsWith("Bearer ")) ? authHeader.substring(7) : null;
			return ResponseEntity.ok(buildUserResponse(user, token));
		}

		// Fallback for session-based Authentication (if used)
		if (authentication != null && authentication.isAuthenticated()
				&& !authentication.getName().equals("anonymousUser")) {
			String email = (authentication
					.getPrincipal() instanceof org.springframework.security.oauth2.core.user.OAuth2User oAuth)
							? oAuth.getAttribute("email")
							: authentication.getName();

			User user = authService.getUserByEmail(email);
			String token = jwtUtils.generateToken(user.getUsername(), user.getUserId(), user.getRole().name());
			return ResponseEntity.ok(buildUserResponse(user, token));
		}

		return ResponseEntity.status(401).body("Not authenticated");
	}

	private Map<String, Object> buildUserResponse(User user, String token) {
		Map<String, Object> response = new HashMap<>();
		response.put("token", token);
		response.put("userId", user.getUserId());
		response.put("username", user.getUsername());
		response.put("email", user.getEmail());
		response.put("avatarUrl", user.getAvatarUrl());
		response.put("fullName", user.getFullName());
		response.put("role", user.getRole());
		response.put("freeCreditsRemaining", user.getFreeCreditsRemaining());
		return response;
	}

	@GetMapping("/profile/{id}")
	public ResponseEntity<User> getProfile(@PathVariable int id) {
		return ResponseEntity.ok(authService.getUserById(id));
	}

	@GetMapping("/by-email")
	public ResponseEntity<?> getByEmail(@RequestParam String email) {
		User user = authService.getUserByEmail(email);
		if (user == null) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(UserSummaryResponse.builder().userId(user.getUserId()).username(user.getUsername())
				.email(user.getEmail()).role(user.getRole() != null ? user.getRole().name() : null).build());
	}

	@GetMapping("/credits/{id}")
	public ResponseEntity<?> getCredits(@PathVariable int id) {
		return ResponseEntity.ok(Map.of("freeCreditsRemaining", authService.getFreeCreditsRemaining(id)));
	}

	@PostMapping("/credits/{id}/consume")
	public ResponseEntity<?> consumeCredits(@PathVariable int id,
			@RequestParam(value = "amount", defaultValue = "1") int amount) {
		int remaining = authService.consumeFreeCredits(id, amount);
		return ResponseEntity.ok(Map.of("freeCreditsRemaining", remaining));
	}

	@PutMapping("/profile/{id}")
	public ResponseEntity<User> updateProfile(@PathVariable int id, @RequestBody User user) {
		return ResponseEntity.ok(authService.updateProfile(id, user));
	}

	@GetMapping("/search")
	public ResponseEntity<List<User>> searchUsers(@RequestParam String query) {
		return ResponseEntity.ok(authService.searchUsers(query));
	}

	@PostMapping("/send-otp")
	public ResponseEntity<?> sendOtp(@RequestBody Map<String, String> request) {
		try {
			authService.sendPasswordResetOtp(request.get("email"));
			return ResponseEntity.ok(Map.of("message", "OTP sent successfully"));
		} catch (Exception e) {
			return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
		}
	}

	@PostMapping("/reset-password")
	public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
		try {
			authService.resetPasswordWithOtp(request.get("email"), request.get("otp"), request.get("newPassword"));
			return ResponseEntity.ok(Map.of("message", "Password reset successfully"));
		} catch (Exception e) {
			return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
		}
	}
}
