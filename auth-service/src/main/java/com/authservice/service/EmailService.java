package com.authservice.service;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

	@Autowired
	private JavaMailSender mailSender;

	@Value("${FRONTEND_URL}")
	private String frontendUrl;

	private void sendHtmlEmail(String toEmail, String subject, String htmlTemplate, String failureMessage) {
		try {
			MimeMessage mimeMessage = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

			helper.setTo(toEmail);
			helper.setSubject(subject);
			helper.setText(htmlTemplate, true);

			mailSender.send(mimeMessage);
			System.out.println("Email sent successfully to " + toEmail + " with subject: " + subject);
		} catch (Exception e) {
			System.err.println(failureMessage + ": " + e.getMessage());
			throw new RuntimeException(failureMessage + ". Please verify SMTP credentials and try again.", e);
		}
	}

	public void sendWelcomeEmail(String toEmail, String username) {
		String subject = "Welcome to CodeSync";

		String htmlTemplate = """
				<!DOCTYPE html>
				<html>
				<head>
				    <style>
				        body { margin: 0; padding: 0; background-color: #f8fafc; font-family: 'Inter', sans-serif; }
				        .container { width: 100%%; background-color: #f8fafc; padding: 40px 0; }
				        .card { background-color: #ffffff; margin: 0 auto; max-width: 550px; border-radius: 16px; box-shadow: 0 4px 12px rgba(0,0,0,0.05); overflow: hidden; border: 1px solid #e2e8f0; }
				        .header { background-color: #070F2B; padding: 40px 30px; text-align: center; }
				        .content { padding: 40px; text-align: center; }
				        .welcome-badge { display: inline-block; padding: 6px 14px; border-radius: 50px; font-size: 12px; font-weight: 700; border: 1px solid #9290C3; color: #535C91; background-color: #f8fafc; margin-bottom: 20px; }
				        .title { font-size: 28px; font-weight: 800; color: #070F2B; margin: 10px 0; }
				        .msg { color: #64748b; font-size: 16px; line-height: 1.6; margin-bottom: 30px; }
				        .btn { background-color: #535C91; color: #ffffff !important; padding: 14px 32px; border-radius: 10px; text-decoration: none; font-weight: 600; display: inline-block; }
				        .footer { padding: 20px; text-align: center; font-size: 12px; color: #94a3b8; }
				    </style>
				</head>
				<body>
				    <div class='container'>
				        <div class='card'>
				            <div class='header'>
				                <h1 style='color:white; margin:0; font-size: 36px;'>Code<span style='color:#9290C3'>Sync</span></h1>
				                <p style='color:#9290C3; margin-top:10px; font-size:14px; letter-spacing: 1px;'>ONLINE COLLABORATION PLATFORM</p>
				            </div>
				            <div class='content'>
				                <div class='welcome-badge'>Welcome Aboard!</div>
				                <h3 style='margin-top:0;'>Hi %s,</h3>
				                <div class='title'>Ready to build together?</div>
				                <p class='msg'>We are absolutely thrilled to welcome you to CodeSync! Dive into a world of seamless real-time code collaboration, secure microservices architecture, and dynamic project mentorship. Your journey to build better software starts right here.</p>
				                <a href='%s/dashboard' class='btn'>Explore Dashboard</a>
				            </div>
				        </div>
				        <div class='footer'>CodeSync Microservices Framework - Bhopal, India<br>You received this email because you registered a new account.</div>
				    </div>
				</body>
				</html>
				""".formatted(username, frontendUrl);

		sendHtmlEmail(toEmail, subject, htmlTemplate, "Error sending welcome email");
	}

	public void sendRegistrationOtpEmail(String toEmail, String otp) {
		String subject = "CodeSync Registration OTP";

		String htmlTemplate = """
				<!DOCTYPE html>
				<html>
				<head>
				    <style>
				        body { margin: 0; padding: 0; background-color: #f8fafc; font-family: 'Inter', sans-serif; }
				        .container { width: 100%%; background-color: #f8fafc; padding: 40px 0; }
				        .card { background-color: #ffffff; margin: 0 auto; max-width: 550px; border-radius: 16px; box-shadow: 0 4px 12px rgba(0,0,0,0.05); overflow: hidden; border: 1px solid #e2e8f0; }
				        .header { background-color: #070F2B; padding: 40px 30px; text-align: center; }
				        .content { padding: 40px; text-align: center; }
				        .otp-badge { display: inline-block; padding: 12px 24px; border-radius: 8px; font-size: 24px; font-weight: 800; border: 2px dashed #9290C3; color: #535C91; background-color: #f8fafc; margin: 20px 0; letter-spacing: 4px; }
				        .title { font-size: 24px; font-weight: 800; color: #070F2B; margin: 10px 0; }
				        .msg { color: #64748b; font-size: 16px; line-height: 1.6; margin-bottom: 30px; }
				        .footer { padding: 20px; text-align: center; font-size: 12px; color: #94a3b8; }
				    </style>
				</head>
				<body>
				    <div class='container'>
				        <div class='card'>
				            <div class='header'>
				                <h1 style='color:white; margin:0; font-size: 36px;'>Code<span style='color:#9290C3'>Sync</span></h1>
				            </div>
				            <div class='content'>
				                <div class='title'>Verify Your Email</div>
				                <p class='msg'>Use the OTP below to complete your CodeSync registration.</p>
				                <div class='otp-badge'>%s</div>
				            </div>
				        </div>
				        <div class='footer'>CodeSync Microservices Framework - Registration Team</div>
				    </div>
				</body>
				</html>
				""".formatted(otp);

		sendHtmlEmail(toEmail, subject, htmlTemplate, "Error sending registration OTP email");
	}

	public void sendPasswordResetOtpEmail(String toEmail, String otp) {
		String subject = "CodeSync Password Reset OTP";

		String htmlTemplate = """
				<!DOCTYPE html>
				<html>
				<head>
				    <style>
				        body { margin: 0; padding: 0; background-color: #f8fafc; font-family: 'Inter', sans-serif; }
				        .container { width: 100%%; background-color: #f8fafc; padding: 40px 0; }
				        .card { background-color: #ffffff; margin: 0 auto; max-width: 550px; border-radius: 16px; box-shadow: 0 4px 12px rgba(0,0,0,0.05); overflow: hidden; border: 1px solid #e2e8f0; }
				        .header { background-color: #070F2B; padding: 40px 30px; text-align: center; }
				        .content { padding: 40px; text-align: center; }
				        .otp-badge { display: inline-block; padding: 12px 24px; border-radius: 8px; font-size: 24px; font-weight: 800; border: 2px dashed #9290C3; color: #535C91; background-color: #f8fafc; margin: 20px 0; letter-spacing: 4px; }
				        .title { font-size: 24px; font-weight: 800; color: #070F2B; margin: 10px 0; }
				        .msg { color: #64748b; font-size: 16px; line-height: 1.6; margin-bottom: 30px; }
				        .footer { padding: 20px; text-align: center; font-size: 12px; color: #94a3b8; }
				    </style>
				</head>
				<body>
				    <div class='container'>
				        <div class='card'>
				            <div class='header'>
				                <h1 style='color:white; margin:0; font-size: 36px;'>Code<span style='color:#9290C3'>Sync</span></h1>
				            </div>
				            <div class='content'>
				                <div class='title'>Password Reset Request</div>
				                <p class='msg'>We received a request to reset your password. Use the OTP below to proceed. This OTP is valid for 15 minutes.</p>
				                <div class='otp-badge'>%s</div>
				                <p class='msg' style='font-size: 14px;'>If you did not request this, you can safely ignore this email.</p>
				            </div>
				        </div>
				        <div class='footer'>CodeSync Microservices Framework - Security Team</div>
				    </div>
				</body>
				</html>
				""".formatted(otp);

		sendHtmlEmail(toEmail, subject, htmlTemplate, "Error sending password reset OTP email");
	}
}
