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

    // AuthController.java - POST /auth/forgot-password
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        log.info("Forgot password request received for email: {}", email);

        // STEP 1: Check if email exists in database
        boolean emailExists = passwordResetService.isEmailRegistered(email);

        if (!emailExists) {
            // Return same message for security (don't reveal email doesn't exist)
            return ResponseEntity.ok(ApiResponse.success(
                    "If the email address is registered with us, you will receive a password reset link shortly.",
                    "Password reset request processed"));
        }

        // STEP 2: Process reset for existing email
        String result = passwordResetService.initiatePasswordReset(email);
        return ResponseEntity.ok(ApiResponse.success(result, "Password reset email sent"));
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
}