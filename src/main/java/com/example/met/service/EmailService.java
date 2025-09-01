package com.example.met.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private static final ZoneId SRI_LANKA_ZONE = ZoneId.of("Asia/Colombo");

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:noreply@yourcompany.com}")
    private String fromEmail;

    @Value("${app.mail.password-reset.subject:Password Reset Request - MET System}")
    private String passwordResetSubject;

    public void sendPasswordResetEmail(String name, String toEmail, String resetLink) {
        try {
            log.info("Preparing to send password reset email to: {}", toEmail);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject(passwordResetSubject);

            String emailBody = buildPasswordResetEmailBody(name, resetLink, toEmail);
            message.setText(emailBody);

            mailSender.send(message);
            log.info("Password reset email sent successfully to: {}", toEmail);

        } catch (Exception e) {
            log.error("Failed to send password reset email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send password reset email: " + e.getMessage(), e);
        }
    }

    public void sendPasswordResetConfirmationEmail(String name, String toEmail) {
        try {
            log.info("Preparing to send password reset confirmation email to: {}", toEmail);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Password Reset Confirmation - MET System");

            String emailBody = buildPasswordResetConfirmationEmailBody(name);
            message.setText(emailBody);

            mailSender.send(message);
            log.info("Password reset confirmation email sent successfully to: {}", toEmail);

        } catch (Exception e) {
            log.error("Failed to send password reset confirmation email to: {}", toEmail, e);
            // Don't throw exception here as it's not critical
        }
    }

    private String buildPasswordResetEmailBody(String name, String resetLink, String toEmail) {
        String currentDateTime = LocalDateTime.now(SRI_LANKA_ZONE)
                .format(DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' HH:mm"));

        return String.format("""
            Dear %s,
            
            You have requested to reset your password for your MET System account.
            
            If you made this request, please click on the link below to reset your password:
            
            %s
            
            This link will expire in 24 hours for security reasons.
            
            Request Details:
            - Requested on: %s (Sri Lanka Time)
            - Your email: %s
            
            Important Security Notes:
            • Never share this link with anyone
            • MET System will never ask for your password via email
            • If you have any concerns, contact support immediately
            
            If you did not request this password reset, please ignore this email and your password will remain unchanged.
            
            Best regards,
            The MET System Team
            
            ---
            This is an automated email. Please do not reply to this message.
            """, name, resetLink, currentDateTime, toEmail);
    }

    private String buildPasswordResetConfirmationEmailBody(String name) {
        String currentDateTime = LocalDateTime.now(SRI_LANKA_ZONE)
                .format(DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' HH:mm"));

        return String.format("""
            Dear %s,
            
            This email confirms that your password for your MET System account has been successfully reset.
            
            Password Change Details:
            - Changed on: %s (Sri Lanka Time)
            
            If you did not make this change, please contact support immediately.
            
            For your security, we recommend:
            • Using a strong, unique password
            • Not sharing your login credentials with anyone
            • Logging out when using shared computers
            
            Thank you for keeping your account secure.
            
            Best regards,
            The MET System Team
            """, name, currentDateTime);
    }

    // Method to send test email for verification (optional)
    public void sendTestEmail(String toEmail) {
        try {
            log.info("Sending test email to: {}", toEmail);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Test Email - MET System");
            message.setText("This is a test email to verify email configuration is working properly.");

            mailSender.send(message);
            log.info("Test email sent successfully to: {}", toEmail);

        } catch (Exception e) {
            log.error("Failed to send test email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send test email: " + e.getMessage(), e);
        }
    }
}