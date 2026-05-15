package com.authservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * User Entity - Core identity for CodeSync Developers & Admins
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int userId;

	@NotBlank(message = "Username is required")
	@Size(min = 4, max = 50, message = "Username must be between 4-50 characters")
	@Column(unique = true, nullable = false)
	private String username;

	@NotBlank(message = "Email is required")
	@Email(message = "Please provide a valid email")
	@Column(unique = true, nullable = false)
	private String email;

	@NotBlank(message = "Password cannot be empty")
	@Size(min = 8, message = "Password must be at least 8 characters")
	private String passwordHash;

	private String fullName;

	@Enumerated(EnumType.STRING)
	@Column(name = "user_role", nullable = false)
	private Role role = Role.DEVELOPER; // Default role

	@Column(columnDefinition = "LONGTEXT")
	private String avatarUrl;
	private String provider = "LOCAL"; // LOCAL, GITHUB, GOOGLE

	private boolean isActive = true;
	private LocalDateTime createdAt = LocalDateTime.now();
	private String bio;
	private String githubLink;
	private String linkedinLink;
	private String twitterLink;
	private int freeCreditsRemaining = 5;
	
	private String resetOtp;
	private LocalDateTime otpExpiry;

	// Constructor for reference-based tasks
	public User(int userId) {
		this.userId = userId;
	}
}
