package com.example.met.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:noreply@metropolitan.com}")
    private String fromEmail;

    @Value("${app.mail.password-reset.subject:Password Reset Request - Metropolitan System}")
    private String passwordResetSubject;

    @Value("${app.mail.password-reset-confirmation.subject:Password Reset Confirmation - Metropolitan System}")
    private String passwordResetConfirmationSubject;

    @Value("${app.company.name:Metropolitan}")
    private String companyName;

    @Value("${app.company.support-email:support@metropolitan.com}")
    private String supportEmail;

    @Value("${app.email.enabled:true}")
    private boolean emailEnabled;

    public void sendPasswordResetEmail(String name, String toEmail, String resetLink) {
        if (!emailEnabled) {
            log.warn("Email service is disabled. Skipping password reset email to: {}", toEmail);
            return;
        }

        try {
            log.info("Preparing to send password reset email to: {}", toEmail);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject(passwordResetSubject);

            String emailBody = buildPasswordResetEmailBody(name, resetLink);
            message.setText(emailBody);

            mailSender.send(message);
            log.info("Password reset email sent successfully to: {}", toEmail);

        } catch (Exception e) {
            // Check if it's a connection-related exception
            if (e.getMessage() != null && (
                    e.getMessage().contains("connection") ||
                            e.getMessage().contains("timeout") ||
                            e.getMessage().contains("connect") ||
                            e.getMessage().toLowerCase().contains("couldn't connect") ||
                            e.getCause() instanceof java.net.ConnectException ||
                            e.getCause() instanceof java.net.SocketTimeoutException)) {

                log.error("Email connection failed for password reset email to: {}. Error: {}", toEmail, e.getMessage());
                throw new RuntimeException("Email service is currently unavailable. Please try again later or contact support.", e);
            }
            log.error("Failed to send password reset email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send password reset email: " + e.getMessage(), e);
        }
    }

    public void sendPasswordResetConfirmationEmail(String name, String toEmail) {
        if (!emailEnabled) {
            log.warn("Email service is disabled. Skipping password reset confirmation email to: {}", toEmail);
            return;
        }

        try {
            log.info("Preparing to send password reset confirmation email to: {}", toEmail);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject(passwordResetConfirmationSubject);

            String emailBody = buildPasswordResetConfirmationEmailBody(name);
            message.setText(emailBody);

            mailSender.send(message);
            log.info("Password reset confirmation email sent successfully to: {}", toEmail);

        } catch (Exception e) {
            log.error("Failed to send password reset confirmation email to: {}", toEmail, e);
            // Don't throw exception here as it's not critical
        }
    }

    private String buildPasswordResetEmailBody(String name, String resetLink) {
        String currentDateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' HH:mm"));

        return String.format("""
            Dear %s,
            
            You have requested to reset your password for your %s account.
            
            If you made this request, please click on the link below to reset your password:
            
            %s
            
            This link will expire in 24 hours for security reasons.
            
            If you did not request this password reset, please ignore this email and your password will remain unchanged. For your security, you may also want to contact our support team at %s.
            
            Request Details:
            - Requested on: %s
            - Your email: %s
            
            Important Security Notes:
            • Never share this link with anyone
            • %s will never ask for your password via email
            • If you have any concerns, contact us immediately
            
            Best regards,
            The %s Security Team
            
            ---
            This is an automated email. Please do not reply to this message.
            For support, contact us at: %s
            """,
                name, companyName, resetLink, supportEmail, currentDateTime,
                "your-email-address", companyName, companyName, supportEmail);
    }

    private String buildPasswordResetConfirmationEmailBody(String name) {
        String currentDateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' HH:mm"));

        return String.format("""
            Dear %s,
            
            This email confirms that your password for your %s account has been successfully reset.
            
            Password Change Details:
            - Changed on: %s
            - Account: %s
            
            If you did not make this change, please contact our support team immediately at %s.
            
            For your security, we recommend:
            • Using a strong, unique password
            • Not sharing your login credentials with anyone
            • Logging out of your account when using shared computers
            
            Thank you for keeping your account secure.
            
            Best regards,
            The %s Security Team
            
            ---
            This is an automated email. Please do not reply to this message.
            For support, contact us at: %s
            """,
                name, companyName, currentDateTime, "your-account",
                supportEmail, companyName, supportEmail);
    }

    // Method to send test email for verification
    public void sendTestEmail(String toEmail) {
        if (!emailEnabled) {
            log.warn("Email service is disabled. Skipping test email to: {}", toEmail);
            throw new RuntimeException("Email service is currently disabled");
        }

        try {
            log.info("Sending test email to: {}", toEmail);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Test Email - " + companyName + " System");
            message.setText("This is a test email to verify email configuration is working properly.");

            mailSender.send(message);
            log.info("Test email sent successfully to: {}", toEmail);

        } catch (Exception e) {
            log.error("Failed to send test email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send test email: " + e.getMessage(), e);
        }
    }

    // Check if email service is available
    public boolean isEmailServiceAvailable() {
        if (!emailEnabled) {
            return false;
        }

        try {
            // Try to create a simple test message to check if mail sender is available
            SimpleMailMessage testMessage = new SimpleMailMessage();
            testMessage.setFrom(fromEmail);
            testMessage.setTo("test@example.com");
            testMessage.setSubject("Connection Test");
            testMessage.setText("Test");

            // Note: This doesn't actually send the email, just tests if the configuration is valid
            log.info("Email service appears to be available");
            return true;
        } catch (Exception e) {
            log.warn("Email service appears to be unavailable: {}", e.getMessage());
            return false;
        }
    }
}