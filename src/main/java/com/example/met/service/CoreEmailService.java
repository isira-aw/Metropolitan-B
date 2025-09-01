package com.example.met.service;

import com.example.met.entity.EmailRecord;
import com.example.met.repository.EmailRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CoreEmailService {

    private final JavaMailSender mailSender;
    private final EmailRecordRepository emailRecordRepository;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public EmailRecord sendEmail(String toEmail, String recipientName, String subject, String message, UUID jobCardId, String sentBy) {
        // Create email record
        EmailRecord emailRecord = new EmailRecord();
        emailRecord.setJobCardId(jobCardId);
        emailRecord.setRecipientEmail(toEmail);
        emailRecord.setRecipientName(recipientName);
        emailRecord.setSubject(subject);
        emailRecord.setMessage(message);
        emailRecord.setSentBy(sentBy);
        emailRecord.setStatus(EmailRecord.EmailStatus.PENDING);

        try {
            // Create and send email
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setFrom(fromEmail);
            mailMessage.setTo(toEmail);
            mailMessage.setSubject(subject);

            String fullMessage = message +
                    "\n\n---\n" +
                    "This is an automated message from Metropolitan Engineering.\n" +
                    "Please do not reply to this email.\n" +
                    "For inquiries, please contact us at: " + fromEmail;

            mailMessage.setText(fullMessage);

            // Send the email
            mailSender.send(mailMessage);

            // Update record as sent
            emailRecord.setStatus(EmailRecord.EmailStatus.SENT);
            emailRecord.setSentAt(LocalDateTime.now());

        } catch (Exception e) {
            // Update record as failed
            emailRecord.setStatus(EmailRecord.EmailStatus.FAILED);
            emailRecord.setErrorMessage(e.getMessage());

            // Save the record and re-throw exception
            emailRecordRepository.save(emailRecord);
            throw new RuntimeException("Failed to send email: " + e.getMessage(), e);
        }

        // Save and return the record
        return emailRecordRepository.save(emailRecord);
    }

    public EmailRecord sendPasswordResetEmail(String toEmail, String recipientName, String resetLink) {
        String subject = "Password Reset Request - Metropolitan Engineering";

        String message = String.format(
                "Dear %s,\n\n" +
                        "You have requested to reset your password for your Metropolitan Engineering account.\n\n" +
                        "Please click the link below to reset your password:\n" +
                        "%s\n\n" +
                        "This link will expire in 24 hours.\n\n" +
                        "If you did not request this, please ignore this email.\n\n" +
                        "Best regards,\n" +
                        "The Metropolitan Engineering Team",
                recipientName, resetLink
        );

        // Create email record for password reset
        EmailRecord emailRecord = new EmailRecord();
        emailRecord.setRecipientEmail(toEmail);
        emailRecord.setRecipientName(recipientName);
        emailRecord.setSubject(subject);
        emailRecord.setMessage(message);
        emailRecord.setSentBy("SYSTEM");
        emailRecord.setStatus(EmailRecord.EmailStatus.PENDING);

        try {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setFrom(fromEmail);
            mailMessage.setTo(toEmail);
            mailMessage.setSubject(subject);
            mailMessage.setText(message);

            mailSender.send(mailMessage);

            emailRecord.setStatus(EmailRecord.EmailStatus.SENT);
            emailRecord.setSentAt(LocalDateTime.now());

        } catch (Exception e) {
            emailRecord.setStatus(EmailRecord.EmailStatus.FAILED);
            emailRecord.setErrorMessage(e.getMessage());
            emailRecordRepository.save(emailRecord);
            throw new RuntimeException("Failed to send password reset email: " + e.getMessage(), e);
        }

        return emailRecordRepository.save(emailRecord);
    }

    public List<EmailRecord> getEmailHistory(UUID jobCardId) {
        return emailRecordRepository.findByJobCardIdOrderByCreatedAtDesc(jobCardId);
    }

    public List<EmailRecord> getAllFailedEmails() {
        return emailRecordRepository.findByStatusOrderByCreatedAtDesc(EmailRecord.EmailStatus.FAILED);
    }

    public long getEmailStats(EmailRecord.EmailStatus status) {
        return emailRecordRepository.countByStatus(status);
    }

    // Test email functionality
    public EmailRecord sendTestEmail(String toEmail) {
        String subject = "Test Email - Metropolitan Engineering";
        String message = "This is a test email to verify the email system is working properly.";

        return sendEmail(toEmail, "Test Recipient", subject, message, null, "SYSTEM_TEST");
    }
}