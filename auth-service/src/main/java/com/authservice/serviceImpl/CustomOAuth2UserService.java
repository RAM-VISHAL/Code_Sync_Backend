package com.authservice.serviceImpl;

import com.authservice.entity.Role;
import com.authservice.entity.User;
import com.authservice.repository.UserRepository;
import com.authservice.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private EmailService emailService;

	@Override
	public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
		OAuth2User oAuth2User = super.loadUser(userRequest);
		String provider = userRequest.getClientRegistration().getRegistrationId().toUpperCase();
		return processOAuth2User(oAuth2User, provider);
	}

	private OAuth2User processOAuth2User(OAuth2User oAuth2User, String provider) {
		Map<String, Object> attributes = oAuth2User.getAttributes();
		String email = (String) attributes.get("email");

		if (email == null && "GITHUB".equals(provider)) {
			email = (String) attributes.get("login") + "@github.com";
		}

		if (email == null) {
			throw new OAuth2AuthenticationException("Email not found from OAuth2 provider");
		}

		Optional<User> userOptional = userRepository.findByEmail(email);
		User user;

		if (userOptional.isPresent()) {
			// Existing User: Just update provider and save
			user = userOptional.get();
			user.setProvider(provider);
			userRepository.save(user);
		} else {
			// New User: Auto-Register
			user = new User();
			user.setEmail(email);

			String name = (String) attributes.get("name");
			user.setFullName(name != null ? name : (String) attributes.get("login"));
			user.setUsername(email.split("@")[0] + "_" + provider.toLowerCase());

			user.setProvider(provider);
			user.setRole(Role.DEVELOPER);
			user.setActive(true);
			user.setCreatedAt(LocalDateTime.now());
			user.setPasswordHash("OAUTH2_USER");

			// Save the user FIRST
			userRepository.save(user);

			// SEND WELCOME EMAIL TO FIRST TIME OAUTH USERS
			final String finalEmail = user.getEmail();
			final String finalUsername = user.getUsername();
			new Thread(() -> {
				emailService.sendWelcomeEmail(finalEmail, finalUsername);
			}).start();
		}

		// Inject the internal DB username into the OAuth2User attributes
		Map<String, Object> customAttributes = new java.util.HashMap<>(attributes);
		customAttributes.put("codesync_username", user.getUsername());

		// Return a modified OAuth2User where getName() returns the database username
		return new org.springframework.security.oauth2.core.user.DefaultOAuth2User(oAuth2User.getAuthorities(),
				customAttributes, "codesync_username");
	}
}
