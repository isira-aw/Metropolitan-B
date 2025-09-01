package com.example.met.service;

import com.example.met.dto.request.SendJobCardEmailRequest;
import com.example.met.dto.response.EmailResponse;
import com.example.met.entity.EmailEntity;
import com.example.met.entity.JobCard;
import com.example.met.enums.EmailStatus;
import com.example.met.repository.EmailRepository;
import com.example.met.repository.JobCardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GenEmailService {

    private final EmailRepository emailRepository;
    private final JobCardRepository jobCardRepository;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Transactional
    public EmailResponse sendJobCardEmail(SendJobCardEmailRequest request) {
        // Verify job card exists
        JobCard jobCard = jobCardRepository.findById(request.getJobCardId())
                .orElseThrow(() -> new RuntimeException("Job card not found"));

        // Get current user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String senderEmail = authentication.getName();

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
            // Send email
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(request.getRecipientEmail());
            message.setSubject(request.getSubject());

            // Add footer to message
            String fullMessage = request.getMessage() +
                    "\n\n---\n" +
                    "This is an automated message from Metropolitan Engineering.\n" +
                    "Please do not reply to this email.\n" +
                    "For inquiries, please contact us at: " + fromEmail;

            message.setText(fullMessage);

            mailSender.send(message);

            // Update status and sent time
            emailEntity.setStatus(EmailStatus.SENT);
            emailEntity.setSentAt(LocalDateTime.now());

        } catch (Exception e) {
            emailEntity.setStatus(EmailStatus.FAILED);
        }

        // Save email record
        EmailEntity savedEmail = emailRepository.save(emailEntity);

        return mapToEmailResponse(savedEmail);
    }

    public List<EmailResponse> getJobCardEmails(UUID jobCardId) {
        List<EmailEntity> emails = emailRepository.findByJobCardIdOrderByCreatedAtDesc(jobCardId);
        return emails.stream()
                .map(this::mapToEmailResponse)
                .collect(Collectors.toList());
    }

    private EmailResponse mapToEmailResponse(EmailEntity entity) {
        return EmailResponse.builder()
                .id(entity.getEmailId()) // Use 'id' instead of 'emailId'
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