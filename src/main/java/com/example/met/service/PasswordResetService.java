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

    @Value("${app.frontend.reset-password-url:http://localhost:5173/reset-password}")
    private String resetPasswordUrl;

    @Transactional
    public String initiatePasswordReset(String email) {
        // Reduced logging - only debug level for normal flow
        if (log.isDebugEnabled()) {
            log.debug("Initiating password reset for: {}", email);
        }

        // STEP 1: Check if employee exists in database
        Optional<Employee> employeeOpt = employeeRepository.findById(email);
        if (employeeOpt.isEmpty()) {
            // Don't log this for security reasons - reduces log volume
            return "If the email address is registered with us, you will receive a password reset link shortly.";
        }

        Employee employee = employeeOpt.get();

        // STEP 2: Delete any existing tokens for this email to ensure only one active token
        passwordResetTokenRepository.deleteByEmail(email);
        // Removed: log.info("Cleared any existing reset tokens for email: {}", email);

        // STEP 3: Generate new secure token
        String token = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(tokenExpirationHours);

        // STEP 4: Save token to database
        PasswordResetToken resetToken = new PasswordResetToken(token, email, expiresAt);
        passwordResetTokenRepository.save(resetToken);

        // Only log in debug mode
        if (log.isDebugEnabled()) {
            log.debug("Created reset token for: {} (expires: {})", email, expiresAt);
        }

        // STEP 5: Construct reset link
        String resetLink = resetPasswordUrl + "?token=" + token;

        // STEP 6: Send email only if employee exists
        try {
            emailService.sendPasswordResetEmail(employee.getName(), email, resetLink);
            // Removed verbose logging
        } catch (Exception e) {
            // Keep error logging but simplified
            log.error("Email send failed for {}: {}", email, e.getMessage());
            // Delete the token if email sending fails
            passwordResetTokenRepository.delete(resetToken);
            throw new RuntimeException("Failed to send password reset email. Please try again later.");
        }

        return "If the email address is registered with us, you will receive a password reset link shortly.";
    }

    @Transactional
    public String resetPassword(String token, String newPassword) {
        // Reduced logging
        if (log.isDebugEnabled()) {
            log.debug("Password reset attempt with token");
        }

        // STEP 1: Validate token exists
        Optional<PasswordResetToken> tokenOpt = passwordResetTokenRepository.findByToken(token);
        if (tokenOpt.isEmpty()) {
            throw new IllegalArgumentException("Invalid reset token");
        }

        PasswordResetToken resetToken = tokenOpt.get();

        // STEP 2: Check if token is expired
        if (resetToken.isExpired()) {
            passwordResetTokenRepository.delete(resetToken);
            throw new IllegalArgumentException("Reset token has expired. Please request a new password reset.");
        }

        // STEP 3: Check if token is already used
        if (resetToken.isUsed()) {
            throw new IllegalArgumentException("Reset token has already been used. Please request a new password reset.");
        }

        // STEP 4: Verify employee still exists (double check)
        Optional<Employee> employeeOpt = employeeRepository.findById(resetToken.getEmail());
        if (employeeOpt.isEmpty()) {
            passwordResetTokenRepository.delete(resetToken);
            throw new IllegalArgumentException("Employee account not found");
        }

        Employee employee = employeeOpt.get();

        // STEP 5: Update password
        String encodedPassword = passwordEncoder.encode(newPassword);
        employee.setPassword(encodedPassword);
        employeeRepository.save(employee);
        // Removed: log.info("Password updated successfully for employee: {}", employee.getEmail());

        // STEP 6: Mark token as used and save
        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        // STEP 7: Send confirmation email (optional)
        try {
            emailService.sendPasswordResetConfirmationEmail(employee.getName(), employee.getEmail());
        } catch (Exception e) {
            // Only log errors, not warnings
            log.error("Confirmation email failed for {}: {}", employee.getEmail(), e.getMessage());
            // Don't fail the password reset if confirmation email fails
        }

        // Single success log
        if (log.isDebugEnabled()) {
            log.debug("Password reset completed for: {}", employee.getEmail());
        }

        return "Password has been reset successfully. You can now login with your new password.";
    }

    public boolean isValidToken(String token) {
        // Removed: log.info("Verifying reset token validity");

        Optional<PasswordResetToken> tokenOpt = passwordResetTokenRepository.findByToken(token);
        if (tokenOpt.isEmpty()) {
            return false;
        }

        PasswordResetToken resetToken = tokenOpt.get();

        // Check if token is expired
        if (resetToken.isExpired()) {
            return false;
        }

        // Check if token is already used
        if (resetToken.isUsed()) {
            return false;
        }

        // Verify employee still exists
        Optional<Employee> employeeOpt = employeeRepository.findById(resetToken.getEmail());
        if (employeeOpt.isEmpty()) {
            return false;
        }

        // Only log successful validations in debug mode
        if (log.isDebugEnabled()) {
            log.debug("Token validation successful for: {}", resetToken.getEmail());
        }
        return true;
    }

    @Transactional
    public void cleanupExpiredTokens() {
        // Reduced logging frequency
        if (log.isDebugEnabled()) {
            log.debug("Starting cleanup of expired password reset tokens");
        }

        int deletedCount = passwordResetTokenRepository.deleteExpiredTokens(LocalDateTime.now());

        // Only log if tokens were actually deleted
        if (deletedCount > 0) {
            log.info("Cleaned up {} expired tokens", deletedCount);
        }
    }

    // Additional method to check if email exists without creating token
    public boolean isEmailRegistered(String email) {
        boolean exists = employeeRepository.existsById(email);
        // Removed verbose logging - only debug mode
        if (log.isDebugEnabled()) {
            log.debug("Email check for {}: {}", email, exists ? "EXISTS" : "NOT_FOUND");
        }
        return exists;
    }

    // Method to get employee details for verification (without sensitive data)
    public Optional<Employee> getEmployeeForVerification(String email) {
        Optional<Employee> employee = employeeRepository.findById(email);
        // Simplified logging
        if (employee.isEmpty() && log.isDebugEnabled()) {
            log.debug("Employee verification failed for: {}", email);
        }
        return employee;
    }
}