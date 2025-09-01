package com.example.met.controller;

import com.example.met.dto.request.SendJobCardEmailRequest;
import com.example.met.dto.response.ApiResponse;
import com.example.met.dto.response.EmailResponse;
import com.example.met.service.EmailService;
import com.example.met.service.GenEmailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/emails")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class EmailController {

    private final GenEmailService emailService;

    @PostMapping("/jobcard")
    public ResponseEntity<ApiResponse<EmailResponse>> sendJobCardEmail(
            @Valid @RequestBody SendJobCardEmailRequest request) {
        log.info("Request to send email for job card: {}", request.getJobCardId());

        try {
            EmailResponse emailResponse = emailService.sendJobCardEmail(request);
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

    @GetMapping("/jobcard/{jobCardId}")
    public ResponseEntity<ApiResponse<List<EmailResponse>>> getJobCardEmails(
            @PathVariable UUID jobCardId) {
        log.info("Request to get email history for job card: {}", jobCardId);

        try {
            List<EmailResponse> emails = emailService.getJobCardEmails(jobCardId);
            ApiResponse<List<EmailResponse>> response = ApiResponse.success(
                    "Email history retrieved successfully", emails);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error retrieving email history for job card: {}", jobCardId, e);
            ApiResponse<List<EmailResponse>> response = ApiResponse.error(
                    "Failed to retrieve email history: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}