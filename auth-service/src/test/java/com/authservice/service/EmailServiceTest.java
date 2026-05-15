package com.authservice.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

	@Mock
	private JavaMailSender mailSender;

	@InjectMocks
	private EmailService emailService;

	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(emailService, "frontendUrl", "http://localhost:4200");
		when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
	}

	@Test
	void sendWelcomeEmail_sendsHtmlEmail() {
		emailService.sendWelcomeEmail("user@example.com", "codesync_user");

		verify(mailSender).send(any(MimeMessage.class));
	}

	@Test
	void sendRegistrationOtpEmail_mailFailureThrowsRuntimeException() {
		doThrow(new RuntimeException("smtp down")).when(mailSender).send(any(MimeMessage.class));

		assertThrows(RuntimeException.class,
				() -> emailService.sendRegistrationOtpEmail("user@example.com", "123456"));
	}
}
