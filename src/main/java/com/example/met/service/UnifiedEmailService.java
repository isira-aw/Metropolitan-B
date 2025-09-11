package com.example.met.service;

import com.example.met.dto.request.SendJobCardEmailRequest;
import com.example.met.dto.response.EmailResponse;
import com.example.met.entity.EmailEntity;
import com.example.met.entity.JobCard;
import com.example.met.enums.EmailStatus;
import com.example.met.repository.EmailRepository;
import com.example.met.repository.JobCardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class UnifiedEmailService {

    private final EmailRepository emailRepository;
    private final JobCardRepository jobCardRepository;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.mail.password-reset.subject:Password Reset Request - Metropolitan System}")
    private String passwordResetSubject;

    @Value("${app.mail.password-reset-confirmation.subject:Password Reset Confirmation - Metropolitan System}")
    private String passwordResetConfirmationSubject;

    @Value("${app.company.name:Metropolitan}")
    private String companyName;

    @Value("${app.company.support-email:support@metropolitan.com}")
    private String supportEmail;

    /**
     * Send password reset email
     */
    public void sendPasswordResetEmail(String name, String toEmail, String resetLink) {
        try {
            log.info("Preparing to send password reset email to: {}", toEmail);

            if (!isValidEmail(toEmail)) {
                throw new IllegalArgumentException("Invalid email address: " + toEmail);
            }

            SimpleMailMessage message = createEmailMessage();
            message.setTo(toEmail);
            message.setSubject(passwordResetSubject);

            String emailBody = buildPasswordResetEmailBody(name, resetLink, toEmail);
            message.setText(emailBody);

            sendEmailWithRetry(message, toEmail, "password reset");
            log.info("Password reset email sent successfully to: {}", toEmail);

        } catch (Exception e) {
            log.error("Failed to send password reset email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send password reset email: " + e.getMessage(), e);
        }
    }

    /**
     * Send password reset confirmation email
     */
    public void sendPasswordResetConfirmationEmail(String name, String toEmail) {
        try {
            log.info("Preparing to send password reset confirmation email to: {}", toEmail);

            if (!isValidEmail(toEmail)) {
                log.warn("Invalid email address for confirmation: {}", toEmail);
                return; // Don't throw exception for confirmation emails
            }

            SimpleMailMessage message = createEmailMessage();
            message.setTo(toEmail);
            message.setSubject(passwordResetConfirmationSubject);

            String emailBody = buildPasswordResetConfirmationEmailBody(name, toEmail);
            message.setText(emailBody);

            sendEmailWithRetry(message, toEmail, "password reset confirmation");
            log.info("Password reset confirmation email sent successfully to: {}", toEmail);

        } catch (Exception e) {
            log.error("Failed to send password reset confirmation email to: {}", toEmail, e);
            // Don't throw exception here as it's not critical
        }
    }

    /**
     * Send job card email with database tracking
     */
    @Transactional
    public EmailResponse sendJobCardEmail(SendJobCardEmailRequest request) {
        // Verify job card exists
        JobCard jobCard = jobCardRepository.findById(request.getJobCardId())
                .orElseThrow(() -> new RuntimeException("Job card not found"));

        // Get current user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String senderEmail = authentication != null ? authentication.getName() : "system";

        // Create email entity
        EmailEntity emailEntity = new EmailEntity();
        emailEntity.setJobCardId(request.getJobCardId());
        emailEntity.setRecipientEmail(request.getRecipientEmail());
        emailEntity.setRecipientName(request.getRecipientName());
        emailEntity.setSubject(request.getSubject());
        emailEntity.setMessage(request.getMessage());
        emailEntity.setSentBy(senderEmail);
        emailEntity.setStatus(EmailStatus.PENDING);

        try {
            if (!isValidEmail(request.getRecipientEmail())) {
                throw new IllegalArgumentException("Invalid recipient email address");
            }

            // Send email
            SimpleMailMessage message = createEmailMessage();
            message.setTo(request.getRecipientEmail());
            message.setSubject(request.getSubject());

            // Add footer to message
            String fullMessage = request.getMessage() +
                    "\n\n---\n" +
                    "This is an automated message from " + companyName + ".\n" +
                    "Please do not reply to this email.\n" +
                    "For inquiries, please contact us at: " + supportEmail;

            message.setText(fullMessage);

            sendEmailWithRetry(message, request.getRecipientEmail(), "job card");

            // Update status and sent time
            emailEntity.setStatus(EmailStatus.SENT);
            emailEntity.setSentAt(LocalDateTime.now());

            log.info("Job card email sent successfully to {} for job card {}",
                    request.getRecipientEmail(), request.getJobCardId());

        } catch (Exception e) {
            log.error("Failed to send job card email to {} for job card {}",
                    request.getRecipientEmail(), request.getJobCardId(), e);
            emailEntity.setStatus(EmailStatus.FAILED);
            throw new RuntimeException("Failed to send job card email: " + e.getMessage(), e);
        }

        // Save email record
        EmailEntity savedEmail = emailRepository.save(emailEntity);
        return mapToEmailResponse(savedEmail);
    }

    /**
     * Send test email for verification
     */
    public void sendTestEmail(String toEmail) {
        try {
            log.info("Sending test email to: {}", toEmail);

            if (!isValidEmail(toEmail)) {
                throw new IllegalArgumentException("Invalid email address: " + toEmail);
            }

            SimpleMailMessage message = createEmailMessage();
            message.setTo(toEmail);
            message.setSubject("Test Email - " + companyName + " System");
            message.setText("This is a test email to verify email configuration is working properly.\n\n" +
                    "If you receive this email, the email service is configured correctly.\n\n" +
                    "Best regards,\nThe " + companyName + " Team");

            sendEmailWithRetry(message, toEmail, "test");
            log.info("Test email sent successfully to: {}", toEmail);

        } catch (Exception e) {
            log.error("Failed to send test email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send test email: " + e.getMessage(), e);
        }
    }

    /**
     * Create base email message with common settings
     */
    private SimpleMailMessage createEmailMessage() {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        return message;
    }

    /**
     * Send email with retry mechanism
     */
    private void sendEmailWithRetry(SimpleMailMessage message, String toEmail, String emailType) {
        int maxRetries = 3;
        int attempt = 0;
        Exception lastException = null;

        while (attempt < maxRetries) {
            try {
                attempt++;
                log.debug("Attempting to send {} email to {} (attempt {}/{})", emailType, toEmail, attempt, maxRetries);

                mailSender.send(message);
                log.debug("Email sent successfully on attempt {}", attempt);
                return; // Success, exit the method

            } catch (Exception e) {
                lastException = e;
                log.warn("Failed to send {} email to {} on attempt {}: {}",
                        emailType, toEmail, attempt, e.getMessage());

                if (attempt < maxRetries) {
                    try {
                        // Wait before retry (exponential backoff)
                        Thread.sleep(1000 * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Email sending interrupted", ie);
                    }
                }
            }
        }

        // If we get here, all retries failed
        throw new RuntimeException("Failed to send " + emailType + " email after " + maxRetries + " attempts", lastException);
    }

    /**
     * Validate email address format
     */
    private boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }

        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return email.matches(emailRegex);
    }

    /**
     * Build password reset email body
     */
    private String buildPasswordResetEmailBody(String name, String resetLink, String userEmail) {
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
                userEmail, companyName, companyName, supportEmail);
    }

    /**
     * Build password reset confirmation email body
     */
    private String buildPasswordResetConfirmationEmailBody(String name, String userEmail) {
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
                name, companyName, currentDateTime, userEmail,
                supportEmail, companyName, supportEmail);
    }

    /**
     * Map EmailEntity to EmailResponse
     */
    private EmailResponse mapToEmailResponse(EmailEntity entity) {
        return EmailResponse.builder()
                .emailId(entity.getEmailId())
                .jobCardId(entity.getJobCardId())
                .recipientEmail(entity.getRecipientEmail())
                .recipientName(entity.getRecipientName())
                .subject(entity.getSubject())
                .message(entity.getMessage())
                .sentAt(entity.getSentAt())
                .status(entity.getStatus())
                .build();
    }
}