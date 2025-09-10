package com.example.met.service;

import com.example.met.entity.Employee;
import com.example.met.entity.PasswordResetToken;
import com.example.met.repository.EmployeeRepository;
import com.example.met.repository.PasswordResetTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private final EmployeeRepository employeeRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.password-reset.token-expiration-hours:24}")
    private int tokenExpirationHours;

    @Value("${app.frontend.reset-password-url:https://metropolitan-d-production.up.railway.app/reset-password}")
    private String resetPasswordUrl;

    @Value("${app.email.enabled:true}")
    private boolean emailEnabled;

    @Transactional
    public String initiatePasswordReset(String email) {
        log.info("Initiating password reset for email: {}", email);

        // STEP 1: Check if employee exists in database
        Optional<Employee> employeeOpt = employeeRepository.findById(email);
        if (employeeOpt.isEmpty()) {
            log.warn("Password reset requested for non-existent email: {}", email);
            // For security reasons, don't reveal that email doesn't exist
            // Return success message but don't send email
            return "If the email address is registered with us, you will receive a password reset link shortly.";
        }

        Employee employee = employeeOpt.get();
        log.info("Employee found in database: {} - {}", employee.getEmail(), employee.getName());

        // STEP 2: Delete any existing tokens for this email to ensure only one active token
        passwordResetTokenRepository.deleteByEmail(email);
        log.info("Cleared any existing reset tokens for email: {}", email);

        // STEP 3: Generate new secure token
        String token = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(tokenExpirationHours);

        // STEP 4: Save token to database
        PasswordResetToken resetToken = new PasswordResetToken(token, email, expiresAt);
        passwordResetTokenRepository.save(resetToken);
        log.info("Created new reset token for email: {} (expires at: {})", email, expiresAt);

        // STEP 5: Construct reset link
        String resetLink = resetPasswordUrl + "?token=" + token;
        log.info("Generated reset link for email: {}", email);

        // STEP 6: Send email only if employee exists and email service is enabled
        if (!emailEnabled) {
            log.warn("Email service is disabled. Password reset token created but email not sent for: {}", email);
            return "Password reset token has been generated. Please contact system administrator as email service is currently unavailable.";
        }

        try {
            emailService.sendPasswordResetEmail(employee.getName(), email, resetLink);
            log.info("Password reset email sent successfully to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send password reset email to: {}", email, e);

            // Check if it's a connection issue vs other email issues
            if (e.getMessage().contains("connection") ||
                    e.getMessage().contains("timeout") ||
                    e.getMessage().contains("connect") ||
                    e.getCause() instanceof java.net.ConnectException) {

                log.warn("Email service connection issue detected. Keeping token active for: {}", email);
                // Don't delete the token - keep it active since the request is valid
                return "Password reset has been processed. Due to email service issues, please contact support for your reset link or try again later.";
            } else {
                // For other email errors, delete the token
                passwordResetTokenRepository.delete(resetToken);
                throw new RuntimeException("Failed to send password reset email. Please try again later.");
            }
        }

        return "If the email address is registered with us, you will receive a password reset link shortly.";
    }

    @Transactional
    public String resetPassword(String token, String newPassword) {
        log.info("Attempting to reset password with token");

        // STEP 1: Validate token exists
        Optional<PasswordResetToken> tokenOpt = passwordResetTokenRepository.findByToken(token);
        if (tokenOpt.isEmpty()) {
            log.warn("Password reset attempted with invalid token");
            throw new IllegalArgumentException("Invalid reset token");
        }

        PasswordResetToken resetToken = tokenOpt.get();
        log.info("Found reset token for email: {}", resetToken.getEmail());

        // STEP 2: Check if token is expired
        if (resetToken.isExpired()) {
            log.warn("Password reset attempted with expired token for email: {}", resetToken.getEmail());
            passwordResetTokenRepository.delete(resetToken);
            throw new IllegalArgumentException("Reset token has expired. Please request a new password reset.");
        }

        // STEP 3: Check if token is already used
        if (resetToken.isUsed()) {
            log.warn("Password reset attempted with already used token for email: {}", resetToken.getEmail());
            throw new IllegalArgumentException("Reset token has already been used. Please request a new password reset.");
        }

        // STEP 4: Verify employee still exists (double check)
        Optional<Employee> employeeOpt = employeeRepository.findById(resetToken.getEmail());
        if (employeeOpt.isEmpty()) {
            log.error("Employee not found for email during password reset: {}", resetToken.getEmail());
            passwordResetTokenRepository.delete(resetToken);
            throw new IllegalArgumentException("Employee account not found");
        }

        Employee employee = employeeOpt.get();
        log.info("Verified employee exists: {} - {}", employee.getEmail(), employee.getName());

        // STEP 5: Update password
        String encodedPassword = passwordEncoder.encode(newPassword);
        employee.setPassword(encodedPassword);
        employeeRepository.save(employee);
        log.info("Password updated successfully for employee: {}", employee.getEmail());

        // STEP 6: Mark token as used and save
        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
        log.info("Reset token marked as used for email: {}", employee.getEmail());

        // STEP 7: Send confirmation email (optional and non-critical)
        if (emailEnabled) {
            try {
                emailService.sendPasswordResetConfirmationEmail(employee.getName(), employee.getEmail());
                log.info("Password reset confirmation email sent to: {}", employee.getEmail());
            } catch (Exception e) {
                log.warn("Failed to send password reset confirmation email to: {} - Error: {}",
                        employee.getEmail(), e.getMessage());
                // Don't fail the password reset if confirmation email fails
            }
        } else {
            log.info("Email service disabled - skipping confirmation email for: {}", employee.getEmail());
        }

        log.info("Password reset completed successfully for email: {}", employee.getEmail());
        return "Password has been reset successfully. You can now login with your new password.";
    }

    public boolean isValidToken(String token) {
        log.info("Verifying reset token validity");

        Optional<PasswordResetToken> tokenOpt = passwordResetTokenRepository.findByToken(token);
        if (tokenOpt.isEmpty()) {
            log.warn("Token verification failed: token not found");
            return false;
        }

        PasswordResetToken resetToken = tokenOpt.get();

        // Check if token is expired
        if (resetToken.isExpired()) {
            log.warn("Token verification failed: token expired for email: {}", resetToken.getEmail());
            return false;
        }

        // Check if token is already used
        if (resetToken.isUsed()) {
            log.warn("Token verification failed: token already used for email: {}", resetToken.getEmail());
            return false;
        }

        // Verify employee still exists
        Optional<Employee> employeeOpt = employeeRepository.findById(resetToken.getEmail());
        if (employeeOpt.isEmpty()) {
            log.warn("Token verification failed: employee not found for email: {}", resetToken.getEmail());
            return false;
        }

        log.info("Token verification successful for email: {}", resetToken.getEmail());
        return true;
    }

    @Transactional
    public void cleanupExpiredTokens() {
        log.info("Starting cleanup of expired password reset tokens");

        int deletedCount = passwordResetTokenRepository.deleteExpiredTokens(LocalDateTime.now());
        log.info("Cleanup completed: {} expired tokens removed", deletedCount);
    }

    // Additional method to check if email exists without creating token
    public boolean isEmailRegistered(String email) {
        boolean exists = employeeRepository.existsById(email);
        log.info("Email registration check for {}: {}", email, exists ? "EXISTS" : "NOT_FOUND");
        return exists;
    }

    // Method to get employee details for verification (without sensitive data)
    public Optional<Employee> getEmployeeForVerification(String email) {
        Optional<Employee> employee = employeeRepository.findById(email);
        if (employee.isPresent()) {
            log.info("Employee verification successful for: {} - {}",
                    employee.get().getEmail(), employee.get().getName());
        } else {
            log.warn("Employee verification failed for: {}", email);
        }
        return employee;
    }

    // Method to check email service status
    public boolean isEmailServiceAvailable() {
        return emailEnabled && emailService.isEmailServiceAvailable();
    }
}