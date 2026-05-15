package com.authservice.service;

import java.util.List;

import com.authservice.entity.User;

public interface AuthService {
	User register(User user, String otp);

	void sendRegistrationOtp(String email, String username);

	String login(String username, String password);

	void logout(String token);

	boolean validateToken(String token);

	String refreshToken(String token);

	User getUserByEmail(String email);

	User getUserById(int userId);
	
	User getByUsername(String username);

	User updateProfile(int userId, User user);

	void changePassword(int userId, String newPassword);

	void sendPasswordResetOtp(String email);
	
	void resetPasswordWithOtp(String email, String otp, String newPassword);

	List<User> searchUsers(String query);

	void deactivateAccount(int userId);

	int getFreeCreditsRemaining(int userId);

	int consumeFreeCredits(int userId, int amount);
}
