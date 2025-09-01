package com.example.met.controller;

import com.example.met.dto.request.SendEmailRequest;
import com.example.met.dto.response.ApiResponse;
import com.example.met.dto.response.EmailResponse;
import com.example.met.entity.EmailRecord;
import com.example.met.service.CoreEmailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/emails")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class EmailController {

    private final CoreEmailService emailService;

    @PostMapping("/jobcard")
    public ResponseEntity<ApiResponse<EmailResponse>> sendJobCardEmail(
            @Valid @RequestBody SendEmailRequest request) {

        try {
            // Get current user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String senderEmail = authentication != null ? authentication.getName() : "SYSTEM";

            // Send email
            EmailRecord emailRecord = emailService.sendEmail(
                    request.getRecipientEmail(),
                    request.getRecipientName(),
                    request.getSubject(),
                    request.getMessage(),
                    request.getJobCardId(),
                    senderEmail
            );

            EmailResponse response = EmailResponse.fromEntity(emailRecord);
            ApiResponse<EmailResponse> apiResponse = ApiResponse.success(
                    "Email sent successfully", response);

            return ResponseEntity.ok(apiResponse);

        } catch (Exception e) {
            ApiResponse<EmailResponse> apiResponse = ApiResponse.error(
                    "Failed to send email: " + e.getMessage());
            return ResponseEntity.badRequest().body(apiResponse);
        }
    }

    @GetMapping("/jobcard/{jobCardId}")
    public ResponseEntity<ApiResponse<List<EmailResponse>>> getJobCardEmails(
            @PathVariable UUID jobCardId) {

        try {
            List<EmailRecord> emailRecords = emailService.getEmailHistory(jobCardId);
            List<EmailResponse> responses = emailRecords.stream()
                    .map(EmailResponse::fromEntity)
                    .collect(Collectors.toList());

            ApiResponse<List<EmailResponse>> apiResponse = ApiResponse.success(
                    "Email history retrieved successfully", responses);

            return ResponseEntity.ok(apiResponse);

        } catch (Exception e) {
            ApiResponse<List<EmailResponse>> apiResponse = ApiResponse.error(
                    "Failed to retrieve email history: " + e.getMessage());
            return ResponseEntity.badRequest().body(apiResponse);
        }
    }

    @PostMapping("/test")
    public ResponseEntity<ApiResponse<EmailResponse>> sendTestEmail(
            @RequestParam String email) {

        try {
            EmailRecord emailRecord = emailService.sendTestEmail(email);
            EmailResponse response = EmailResponse.fromEntity(emailRecord);

            ApiResponse<EmailResponse> apiResponse = ApiResponse.success(
                    "Test email sent successfully", response);

            return ResponseEntity.ok(apiResponse);

        } catch (Exception e) {
            ApiResponse<EmailResponse> apiResponse = ApiResponse.error(
                    "Failed to send test email: " + e.getMessage());
            return ResponseEntity.badRequest().body(apiResponse);
        }
    }

    @GetMapping("/failed")
    public ResponseEntity<ApiResponse<List<EmailResponse>>> getFailedEmails() {
        try {
            List<EmailRecord> failedEmails = emailService.getAllFailedEmails();
            List<EmailResponse> responses = failedEmails.stream()
                    .map(EmailResponse::fromEntity)
                    .collect(Collectors.toList());

            ApiResponse<List<EmailResponse>> apiResponse = ApiResponse.success(
                    "Failed emails retrieved successfully", responses);

            return ResponseEntity.ok(apiResponse);

        } catch (Exception e) {
            ApiResponse<List<EmailResponse>> apiResponse = ApiResponse.error(
                    "Failed to retrieve failed emails: " + e.getMessage());
            return ResponseEntity.badRequest().body(apiResponse);
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<EmailStats>> getEmailStats() {
        try {
            long sentCount = emailService.getEmailStats(EmailRecord.EmailStatus.SENT);
            long failedCount = emailService.getEmailStats(EmailRecord.EmailStatus.FAILED);
            long pendingCount = emailService.getEmailStats(EmailRecord.EmailStatus.PENDING);

            EmailStats stats = new EmailStats(sentCount, failedCount, pendingCount);

            ApiResponse<EmailStats> apiResponse = ApiResponse.success(
                    "Email statistics retrieved successfully", stats);

            return ResponseEntity.ok(apiResponse);

        } catch (Exception e) {
            ApiResponse<EmailStats> apiResponse = ApiResponse.error(
                    "Failed to retrieve email statistics: " + e.getMessage());
            return ResponseEntity.badRequest().body(apiResponse);
        }
    }

    // Inner class for email statistics
    public static class EmailStats {
        public long sent;
        public long failed;
        public long pending;

        public EmailStats(long sent, long failed, long pending) {
            this.sent = sent;
            this.failed = failed;
            this.pending = pending;
        }
    }
}