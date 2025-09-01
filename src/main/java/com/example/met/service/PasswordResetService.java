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
        // NO LOGGING FOR NORMAL FLOW - ONLY ERRORS

        // STEP 1: Check if employee exists in database
        Optional<Employee> employeeOpt = employeeRepository.findById(email);
        if (employeeOpt.isEmpty()) {
            return "If the email address is registered with us, you will receive a password reset link shortly.";
        }

        Employee employee = employeeOpt.get();

        // STEP 2: Delete any existing tokens for this email
        passwordResetTokenRepository.deleteByEmail(email);

        // STEP 3: Generate new secure token
        String token = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(tokenExpirationHours);

        // STEP 4: Save token to database
        PasswordResetToken resetToken = new PasswordResetToken(token, email, expiresAt);
        passwordResetTokenRepository.save(resetToken);

        // STEP 5: Construct reset link
        String resetLink = resetPasswordUrl + "?token=" + token;

        // STEP 6: Send email
        try {
            emailService.sendPasswordResetEmail(employee.getName(), email, resetLink);
        } catch (Exception e) {
            // ONLY LOG CRITICAL ERRORS
            log.error("Email send failed for {}", email);
            passwordResetTokenRepository.delete(resetToken);
            throw new RuntimeException("Failed to send password reset email. Please try again later.");
        }

        return "If the email address is registered with us, you will receive a password reset link shortly.";
    }

    @Transactional
    public String resetPassword(String token, String newPassword) {
        // NO LOGGING FOR NORMAL FLOW

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

        // STEP 4: Verify employee still exists
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

        // STEP 6: Mark token as used
        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        // STEP 7: Send confirmation email
        try {
            emailService.sendPasswordResetConfirmationEmail(employee.getName(), employee.getEmail());
        } catch (Exception e) {
            // ONLY LOG CRITICAL EMAIL FAILURES
            log.error("Confirmation email failed for {}", employee.getEmail());
        }

        return "Password has been reset successfully. You can now login with your new password.";
    }

    public boolean isValidToken(String token) {
        // NO LOGGING FOR NORMAL VALIDATION

        Optional<PasswordResetToken> tokenOpt = passwordResetTokenRepository.findByToken(token);
        if (tokenOpt.isEmpty()) {
            return false;
        }

        PasswordResetToken resetToken = tokenOpt.get();

        if (resetToken.isExpired() || resetToken.isUsed()) {
            return false;
        }

        Optional<Employee> employeeOpt = employeeRepository.findById(resetToken.getEmail());
        return employeeOpt.isPresent();
    }

    @Transactional
    public void cleanupExpiredTokens() {
        // NO LOGGING FOR ROUTINE CLEANUP
        int deletedCount = passwordResetTokenRepository.deleteExpiredTokens(LocalDateTime.now());

        // Only log if there's an unusually high number of expired tokens (potential issue)
        if (deletedCount > 50) {
            log.warn("High number of expired tokens cleaned: {}", deletedCount);
        }
    }

    public boolean isEmailRegistered(String email) {
        // NO LOGGING FOR EMAIL CHECKS
        return employeeRepository.existsById(email);
    }

    public Optional<Employee> getEmployeeForVerification(String email) {
        // NO LOGGING FOR VERIFICATION
        return employeeRepository.findById(email);
    }
}