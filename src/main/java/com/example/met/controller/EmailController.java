package com.example.met.controller;

import com.example.met.dto.request.SendJobCardEmailRequest;
import com.example.met.dto.response.ApiResponse;
import com.example.met.dto.response.EmailResponse;
import com.example.met.service.GenEmailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/emails")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class EmailController {

    private final GenEmailService emailService;

    @PostMapping("/jobcard")
    public ResponseEntity<ApiResponse<EmailResponse>> sendJobCardEmail(
            @Valid @RequestBody SendJobCardEmailRequest request) {

        try {
            EmailResponse emailResponse = emailService.sendJobCardEmail(request);
            ApiResponse<EmailResponse> response = ApiResponse.success(
                    "Email sent successfully", emailResponse);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            ApiResponse<EmailResponse> response = ApiResponse.error(
                    "Failed to send email: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/jobcard/{jobCardId}")
    public ResponseEntity<ApiResponse<List<EmailResponse>>> getJobCardEmails(
            @PathVariable UUID jobCardId) {

        try {
            List<EmailResponse> emails = emailService.getJobCardEmails(jobCardId);
            ApiResponse<List<EmailResponse>> response = ApiResponse.success(
                    "Email history retrieved successfully", emails);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            ApiResponse<List<EmailResponse>> response = ApiResponse.error(
                    "Failed to retrieve email history: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/test")
    public ResponseEntity<ApiResponse<String>> testEmail(@RequestParam String email) {
        try {
            // Create a test email request
            SendJobCardEmailRequest testRequest = new SendJobCardEmailRequest();
            testRequest.setJobCardId(UUID.randomUUID()); // Dummy job card ID for test
            testRequest.setRecipientEmail(email);
            testRequest.setRecipientName("Test User");
            testRequest.setSubject("Test Email - Metropolitan Engineering");
            testRequest.setMessage("This is a test email to verify the email system is working properly.");

            EmailResponse emailResponse = emailService.sendJobCardEmail(testRequest);

            String message = emailResponse.getStatus().toString().equals("SENT")
                    ? "Test email sent successfully"
                    : "Test email failed to send";

            ApiResponse<String> response = ApiResponse.success(message, emailResponse.getStatus().toString());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            ApiResponse<String> response = ApiResponse.error("Test email failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}