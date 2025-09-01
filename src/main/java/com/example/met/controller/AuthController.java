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
        // Only log in debug mode for normal operations
        if (log.isDebugEnabled()) {
            log.debug("Login attempt for email: {}", request.getEmail());
        }

        try {
            LoginResponse loginResponse = authService.login(request);
            ApiResponse<LoginResponse> response = ApiResponse.success("Login successful", loginResponse);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // Keep error logging but make it less verbose
            log.warn("Login failed for email: {}: {}", request.getEmail(), e.getMessage());
            ApiResponse<LoginResponse> response = ApiResponse.error("Invalid email or password");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<EmployeeResponse>> register(@Valid @RequestBody RegisterRequest request) {
        // Reduced logging frequency
        if (log.isDebugEnabled()) {
            log.debug("Registration attempt for email: {}", request.getEmail());
        }

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
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (Exception e) {
            log.warn("Registration failed for email: {}: {}", request.getEmail(), e.getMessage());
            ApiResponse<EmployeeResponse> response = ApiResponse.error("Registration failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        // Reduce logging - only debug level for normal flow
        if (log.isDebugEnabled()) {
            log.debug("Password reset request for: {}", email);
        }

        try {
            // STEP 1: Check if email exists in database
            boolean emailExists = passwordResetService.isEmailRegistered(email);

            if (!emailExists) {
                // Removed detailed logging for security reasons
                ApiResponse<String> response = ApiResponse.success(
                        "If the email address is registered with us, you will receive a password reset link shortly.",
                        "Password reset request processed"
                );
                return ResponseEntity.ok(response);
            }

            // STEP 2: Get employee details for verification
            Optional<Employee> employeeOpt = passwordResetService.getEmployeeForVerification(email);
            if (employeeOpt.isEmpty()) {
                ApiResponse<String> response = ApiResponse.success(
                        "If the email address is registered with us, you will receive a password reset link shortly.",
                        "Password reset request processed"
                );
                return ResponseEntity.ok(response);
            }

            Employee employee = employeeOpt.get();
            // Reduced logging detail
            if (log.isDebugEnabled()) {
                log.debug("Processing password reset for verified employee: {}", employee.getEmail());
            }

            // STEP 3: Initiate password reset (this will send the email)
            String result = passwordResetService.initiatePasswordReset(email);

            ApiResponse<String> response = ApiResponse.success(result, "Password reset email sent");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Password reset error for: {}: {}", email, e.getMessage());
            ApiResponse<String> response = ApiResponse.error("Failed to process password reset request. Please try again later.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        // Reduced initial logging
        if (log.isDebugEnabled()) {
            log.debug("Password reset attempt with token");
        }

        try {
            // Validate that passwords match
            if (!request.getNewPassword().equals(request.getConfirmPassword())) {
                ApiResponse<String> response = ApiResponse.error("Passwords do not match");
                return ResponseEntity.badRequest().body(response);
            }

            // Validate password strength
            if (request.getNewPassword().length() < 6) {
                ApiResponse<String> response = ApiResponse.error("Password must be at least 6 characters long");
                return ResponseEntity.badRequest().body(response);
            }

            if (request.getNewPassword().length() > 20) {
                ApiResponse<String> response = ApiResponse.error("Password must be less than 20 characters long");
                return ResponseEntity.badRequest().body(response);
            }

            String result = passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
            ApiResponse<String> response = ApiResponse.success(result, "Password reset successful");
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            // Simplified error logging
            log.warn("Invalid password reset request: {}", e.getMessage());
            ApiResponse<String> response = ApiResponse.error(e.getMessage());
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("Password reset error: {}", e.getMessage());
            ApiResponse<String> response = ApiResponse.error("Failed to reset password. Please try again later.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/verify-reset-token/{token}")
    public ResponseEntity<ApiResponse<String>> verifyResetToken(@PathVariable String token) {
        try {
            boolean isValid = passwordResetService.isValidToken(token);
            if (isValid) {
                ApiResponse<String> response = ApiResponse.success("Token is valid", "Token verified successfully");
                return ResponseEntity.ok(response);
            } else {
                ApiResponse<String> response = ApiResponse.error("Invalid or expired token");
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            log.error("Token verification error: {}", e.getMessage());
            ApiResponse<String> response = ApiResponse.error("Failed to verify token. Please try again.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Additional endpoint to check if email exists (optional, for frontend validation)
    @PostMapping("/check-email")
    public ResponseEntity<ApiResponse<Boolean>> checkEmailExists(@RequestBody ForgotPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        try {
            boolean exists = passwordResetService.isEmailRegistered(email);
            ApiResponse<Boolean> response = ApiResponse.success("Email check completed", exists);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Email check error for {}: {}", email, e.getMessage());
            ApiResponse<Boolean> response = ApiResponse.error("Failed to check email");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}