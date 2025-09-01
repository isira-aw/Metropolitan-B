package com.example.met.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:noreply@metropolitan.com}")
    private String fromEmail;

    @Value("${app.company.name:Metropolitan}")
    private String companyName;

    @Value("${app.company.support-email:support@metropolitan.com}")
    private String supportEmail;

    public void sendPasswordResetEmail(String name, String toEmail, String resetLink) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Password Reset Request - " + companyName);

            String emailBody = String.format(
                    "Dear %s,\n\n" +
                            "You have requested to reset your password for your %s account.\n\n" +
                            "Please click the link below to reset your password:\n" +
                            "%s\n\n" +
                            "This link will expire in 24 hours.\n\n" +
                            "If you did not request this, please ignore this email.\n\n" +
                            "Best regards,\n" +
                            "The %s Team\n\n" +
                            "For support: %s",
                    name, companyName, resetLink, companyName, supportEmail
            );

            message.setText(emailBody);
            mailSender.send(message);

        } catch (Exception e) {
            throw new RuntimeException("Failed to send password reset email", e);
        }
    }

    public void sendPasswordResetConfirmationEmail(String name, String toEmail) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Password Reset Confirmation - " + companyName);

            String emailBody = String.format(
                    "Dear %s,\n\n" +
                            "Your password has been successfully reset for your %s account.\n\n" +
                            "If you did not make this change, please contact us immediately at %s.\n\n" +
                            "Best regards,\n" +
                            "The %s Team",
                    name, companyName, supportEmail, companyName
            );

            message.setText(emailBody);
            mailSender.send(message);

        } catch (Exception e) {
            // Don't throw exception for confirmation emails
        }
    }

    public void sendTestEmail(String toEmail) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Test Email - " + companyName);
            message.setText("This is a test email to verify configuration.");

            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send test email", e);
        }
    }
}