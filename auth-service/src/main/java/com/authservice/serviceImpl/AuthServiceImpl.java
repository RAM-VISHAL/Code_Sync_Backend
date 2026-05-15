package com.authservice.serviceImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.authservice.config.JwtUtils;
import com.authservice.entity.User;
import com.authservice.repository.UserRepository;
import com.authservice.service.AuthService;
import com.authservice.service.EmailService;
import java.util.List;

@Service
public class AuthServiceImpl implements AuthService {

	@Autowired
	private UserRepository userRepository;
	@Autowired
	private PasswordEncoder passwordEncoder;
	@Autowired
	private JwtUtils jwtUtils;
	@Autowired
	private EmailService emailService;

	private static final java.util.Map<String, String> registrationOtps = new java.util.concurrent.ConcurrentHashMap<>();

	private void validatePassword(String password) {
		if (password == null || password.length() < 8) {
			throw new RuntimeException("Password must be at least 8 characters long");
		}
		if (!password.matches(".*[A-Z].*")) {
			throw new RuntimeException("Password must contain at least one uppercase letter");
		}
		if (!password.matches(".*[a-z].*")) {
			throw new RuntimeException("Password must contain at least one lowercase letter");
		}
		if (!password.matches(".*\\d.*")) {
			throw new RuntimeException("Password must contain at least one number");
		}
		if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) {
			throw new RuntimeException("Password must contain at least one special character");
		}
	}

	@Override
	public void sendRegistrationOtp(String email, String username) {
		if (userRepository.existsByEmail(email))
			throw new RuntimeException("Email already exists");
		if (userRepository.findByUsername(username).isPresent()) {
			throw new RuntimeException("Username already exists");
		}

		String otp = String.format("%06d", new java.util.Random().nextInt(999999));
		emailService.sendRegistrationOtpEmail(email, otp);
		registrationOtps.put(email, otp);
	}

	@Override
	public User register(User user, String otp) {
		// Verify OTP
		String storedOtp = registrationOtps.get(user.getEmail());
		if (storedOtp == null || !storedOtp.equals(otp)) {
			throw new RuntimeException("Invalid or expired registration OTP");
		}

		// Validation: Ensure email/username uniqueness before hashing
		if (userRepository.existsByEmail(user.getEmail()))
			throw new RuntimeException("Email already exists");

		if (userRepository.findByUsername(user.getUsername()).isPresent()) {
			throw new RuntimeException("Username already exists");
		}

		validatePassword(user.getPasswordHash());

		user.setPasswordHash(passwordEncoder.encode(user.getPasswordHash()));
		User savedUser = userRepository.save(user);

		// Remove the OTP from map after successful registration
		registrationOtps.remove(user.getEmail());

		// Non-blocking: Sends email in a background thread to keep response time fast
		new Thread(() -> emailService.sendWelcomeEmail(savedUser.getEmail(), savedUser.getUsername())).start();
		return savedUser;
	}

	@Override
	public String login(String username, String password) {
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new RuntimeException("Invalid username or password"));

		if (passwordEncoder.matches(password, user.getPasswordHash())) {
			return jwtUtils.generateToken(username, user.getRole().name());
		} else {
			throw new RuntimeException("Invalid username or password");
		}
	}

	@Override
	public User getUserById(int userId) {
		return userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User profile not found"));
	}

	@Override
	public List<User> searchUsers(String query) {
		return userRepository.searchByUsername(query);
	}

	@Override
	public boolean validateToken(String token) {
		return jwtUtils.validateToken(token);
	}

	@Override
	public User getUserByEmail(String email) {
		return userRepository.findByEmail(email).orElse(null);
	}

	@Override
	public User getByUsername(String username) {
		return userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
	}

	@Override
	public User updateProfile(int userId, User userDetails) {
		User user = getUserById(userId);
		user.setFullName(userDetails.getFullName());
		user.setBio(userDetails.getBio());
		user.setAvatarUrl(userDetails.getAvatarUrl());

		if (userDetails.getUsername() != null && !userDetails.getUsername().trim().isEmpty()
				&& !userDetails.getUsername().equals(user.getUsername())) {
			if (userRepository.findByUsername(userDetails.getUsername().trim()).isPresent()) {
				throw new RuntimeException("Username already exists");
			}
			user.setUsername(userDetails.getUsername().trim());
		}

		if (userDetails.getEmail() != null && !userDetails.getEmail().trim().isEmpty()
				&& !userDetails.getEmail().equals(user.getEmail())) {
			if (userRepository.existsByEmail(userDetails.getEmail().trim())) {
				throw new RuntimeException("Email already exists");
			}
			user.setEmail(userDetails.getEmail().trim());
		}

		user.setGithubLink(userDetails.getGithubLink());
		user.setLinkedinLink(userDetails.getLinkedinLink());
		user.setTwitterLink(userDetails.getTwitterLink());
		return userRepository.save(user);
	}

	@Override
	public void changePassword(int userId, String newPassword) {
		validatePassword(newPassword);
		User user = getUserById(userId);
		user.setPasswordHash(passwordEncoder.encode(newPassword));
		userRepository.save(user);
	}

	@Override
	public void sendPasswordResetOtp(String email) {
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new RuntimeException("User not found with this email"));

		// Generate 6-digit OTP
		String otp = String.format("%06d", new java.util.Random().nextInt(999999));

		user.setResetOtp(passwordEncoder.encode(otp));
		user.setOtpExpiry(java.time.LocalDateTime.now().plusMinutes(15));
		userRepository.save(user);

		try {
			emailService.sendPasswordResetOtpEmail(email, otp);
		} catch (RuntimeException ex) {
			user.setResetOtp(null);
			user.setOtpExpiry(null);
			userRepository.save(user);
			throw ex;
		}
	}

	@Override
	public void resetPasswordWithOtp(String email, String otp, String newPassword) {
		User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));

		if (user.getResetOtp() == null || user.getOtpExpiry() == null
				|| user.getOtpExpiry().isBefore(java.time.LocalDateTime.now())) {
			throw new RuntimeException("OTP expired or invalid");
		}

		if (!passwordEncoder.matches(otp, user.getResetOtp())) {
			throw new RuntimeException("Invalid OTP");
		}

		validatePassword(newPassword);
		// Reset password and clear OTP
		user.setPasswordHash(passwordEncoder.encode(newPassword));
		user.setResetOtp(null);
		user.setOtpExpiry(null);

		userRepository.save(user);
	}

	@Override
	public void deactivateAccount(int userId) {
		userRepository.deleteById(userId);
	}

	@Override
	public int getFreeCreditsRemaining(int userId) {
		return getUserById(userId).getFreeCreditsRemaining();
	}

	@Override
	public int consumeFreeCredits(int userId, int amount) {
		if (amount <= 0) {
			return getFreeCreditsRemaining(userId);
		}

		User user = getUserById(userId);
		if (user.getFreeCreditsRemaining() < amount) {
			throw new RuntimeException("Your 5 free credits are finished. Please subscribe to continue using premium services.");
		}

		user.setFreeCreditsRemaining(user.getFreeCreditsRemaining() - amount);
		userRepository.save(user);
		return user.getFreeCreditsRemaining();
	}

	// Unused in JWT stateless context, but kept for interface compliance
	@Override
	public void logout(String token) {
	}

	@Override
	public String refreshToken(String token) {
		String username = jwtUtils.getUsernameFromToken(token);
		User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
		return jwtUtils.generateToken(username, user.getRole().name());
	}
}
