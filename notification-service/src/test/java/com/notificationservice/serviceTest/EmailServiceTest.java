package com.notificationservice.serviceTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.notificationservice.service.EmailService;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.util.List;
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
	}

	@Test
	void sendHtmlEmail_withRecipientsSendsMail() {
		when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));

		emailService.sendHtmlEmail(List.of("dev@example.com"), "Dev", "Demo Project", true);

		verify(mailSender).send(any(MimeMessage.class));
	}

	@Test
	void sendHtmlEmail_withoutRecipientsSkipsMail() {
		emailService.sendHtmlEmail(List.of(), "Dev", "Demo Project", false);

		verify(mailSender, never()).send(any(MimeMessage.class));
	}
}
