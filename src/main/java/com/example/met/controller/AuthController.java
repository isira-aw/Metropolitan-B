package com.example.met.controller;

import com.example.met.dto.request.ForgotPasswordRequest;
import com.example.met.dto.request.LoginRequest;
import com.example.met.dto.request.RegisterRequest;
import com.example.met.dto.request.ResetPasswordRequest;
import com.example.met.dto.response.ApiResponse;
import com.example.met.dto.response.EmployeeResponse;
import com.example.met.dto.response.LoginResponse;
import com.example.met.entity.Employee;
import com.example.met.service.AuthService;
import com.example.met.service.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:3000", "https://metropolitan-d-production.up.railway.app"})
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login request received for email: {}", request.getEmail());

        try {
            LoginResponse loginResponse = authService.login(request);
            ApiResponse<LoginResponse> response = ApiResponse.success("Login successful", loginResponse);
            log.info("Login successful for email: {}", request.getEmail());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Login failed for email: {}", request.getEmail(), e);
            ApiResponse<LoginResponse> response = ApiResponse.error("Invalid email or password");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<EmployeeResponse>> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Registration request received for email: {}", request.getEmail());

        try {
            Employee employee = authService.register(request);

            EmployeeResponse employeeResponse = new EmployeeResponse();
            employeeResponse.setEmail(employee.getEmail());
            employeeResponse.setName(employee.getName());
            employeeResponse.setContactNumber(employee.getContactNumber());
            employeeResponse.setRole(employee.getRole());
            employeeResponse.setCreatedAt(employee.getCreatedAt());
            employeeResponse.setUpdatedAt(employee.getUpdatedAt());

            ApiResponse<EmployeeResponse> response = ApiResponse.success("Registration successful", employeeResponse);
            log.info("Registration successful for email: {}", request.getEmail());
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (Exception e) {
            log.error("Registration failed for email: {}", request.getEmail(), e);
            ApiResponse<EmployeeResponse> response = ApiResponse.error("Registration failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        log.info("Forgot password request received for email: {}", email);

        try {
            // STEP 1: Check if email exists in database FIRST
            boolean emailExists = passwordResetService.isEmailRegistered(email);
            log.info("Email existence check for {}: {}", email, emailExists ? "EXISTS" : "NOT_FOUND");

            if (!emailExists) {
                log.warn("Forgot password requested for non-existent email: {}", email);
                // For security reasons, return the same message whether email exists or not
                ApiResponse<String> response = ApiResponse.success(
                        "If the email address is registered with us, you will receive a password reset link shortly.",
                        "Password reset request processed"
                );
                return ResponseEntity.ok(response);
            }

            // STEP 2: Process reset for existing email (this will send the email)
            String result = passwordResetService.initiatePasswordReset(email);

            ApiResponse<String> response = ApiResponse.success(result, "Password reset email sent");
            log.info("Password reset process completed for email: {}", email);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error processing forgot password request for email: {}", email, e);
            ApiResponse<String> response = ApiResponse.error("Failed to process password reset request. Please try again later.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }


    @GetMapping("/verify-reset-token/{token}")
    public ResponseEntity<ApiResponse<String>> verifyResetToken(@PathVariable String token) {
        log.info("Token verification request received");

        try {
            boolean isValid = passwordResetService.isValidToken(token);
            if (isValid) {
                ApiResponse<String> response = ApiResponse.success("Token is valid", "Token verified successfully");
                log.info("Token verification successful");
                return ResponseEntity.ok(response);
            } else {
                ApiResponse<String> response = ApiResponse.error("Invalid or expired token");
                log.warn("Token verification failed");
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            log.error("Error verifying reset token", e);
            ApiResponse<String> response = ApiResponse.error("Failed to verify token. Please try again.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Additional endpoint to check if email exists (optional, for frontend validation)
    @PostMapping("/check-email")
    public ResponseEntity<ApiResponse<Boolean>> checkEmailExists(@RequestBody ForgotPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        log.info("Email existence check request for: {}", email);

        try {
            boolean exists = passwordResetService.isEmailRegistered(email);
            ApiResponse<Boolean> response = ApiResponse.success("Email check completed", exists);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error checking email existence for: {}", email, e);
            ApiResponse<Boolean> response = ApiResponse.error("Failed to check email");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        log.info("Reset password request received with token");

        try {
            // Validate that passwords match
            if (!request.getNewPassword().equals(request.getConfirmPassword())) {
                log.warn("Password reset failed: passwords do not match");
                ApiResponse<String> response = ApiResponse.error("Passwords do not match");
                return ResponseEntity.badRequest().body(response);
            }

            // Validate password strength
            if (request.getNewPassword().length() < 6) {
                log.warn("Password reset failed: password too short");
                ApiResponse<String> response = ApiResponse.error("Password must be at least 6 characters long");
                return ResponseEntity.badRequest().body(response);
            }

            if (request.getNewPassword().length() > 20) {
                log.warn("Password reset failed: password too long");
                ApiResponse<String> response = ApiResponse.error("Password must be less than 20 characters long");
                return ResponseEntity.badRequest().body(response);
            }

            String result = passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
            ApiResponse<String> response = ApiResponse.success(result, "Password reset successful");
            log.info("Password reset completed successfully");
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Password reset failed with invalid argument: {}", e.getMessage());
            ApiResponse<String> response = ApiResponse.error(e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("Error processing password reset", e);
            ApiResponse<String> response = ApiResponse.error("Failed to reset password. Please try again later.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}