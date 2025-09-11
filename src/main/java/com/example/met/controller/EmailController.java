package com.example.met.controller;

import com.example.met.dto.request.SendJobCardEmailRequest;
import com.example.met.dto.response.ApiResponse;
import com.example.met.dto.response.EmailResponse;
import com.example.met.service.UnifiedEmailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/emails")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class EmailController {

    private final UnifiedEmailService unifiedEmailService; // Changed from GenEmailService

    @PostMapping("/jobcard")
    public ResponseEntity<ApiResponse<EmailResponse>> sendJobCardEmail(
            @Valid @RequestBody SendJobCardEmailRequest request) {
        log.info("Request to send email for job card: {}", request.getJobCardId());

        try {
            EmailResponse emailResponse = unifiedEmailService.sendJobCardEmail(request);
            ApiResponse<EmailResponse> response = ApiResponse.success(
                    "Email sent successfully", emailResponse);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error sending email for job card: {}", request.getJobCardId(), e);
            ApiResponse<EmailResponse> response = ApiResponse.error(
                    "Failed to send email: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/test")
    public ResponseEntity<ApiResponse<String>> sendTestEmail(@RequestParam String email) {
        log.info("Request to send test email to: {}", email);

        try {
            unifiedEmailService.sendTestEmail(email);
            ApiResponse<String> response = ApiResponse.success(
                    "Test email sent successfully", "Test email sent to " + email);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error sending test email to: {}", email, e);
            ApiResponse<String> response = ApiResponse.error(
                    "Failed to send test email: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}