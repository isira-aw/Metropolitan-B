package com.example.met.service;

import com.example.met.entity.Employee;
import com.example.met.entity.PasswordResetToken;
import com.example.met.repository.EmployeeRepository;
import com.example.met.repository.PasswordResetTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final EmployeeRepository employeeRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.password-reset.token-expiration-hours:24}")
    private int tokenExpirationHours;

    @Value("${app.frontend.reset-password-url:http://localhost:3000/reset-password}")
    private String resetPasswordUrl;

    @Transactional
    public String initiatePasswordReset(String email) {
        try {
            // Check if employee exists
            Optional<Employee> employeeOpt = employeeRepository.findById(email);
            if (employeeOpt.isEmpty()) {
                return "If the email address is registered with us, you will receive a password reset link shortly.";
            }

            Employee employee = employeeOpt.get();

            // Delete existing tokens
            passwordResetTokenRepository.deleteByEmail(email);

            // Generate new token
            String token = UUID.randomUUID().toString();
            LocalDateTime expiresAt = LocalDateTime.now().plusHours(tokenExpirationHours);

            // Save token
            PasswordResetToken resetToken = new PasswordResetToken(token, email, expiresAt);
            passwordResetTokenRepository.save(resetToken);

            // Create reset link
            String resetLink = resetPasswordUrl + "?token=" + token;

            // Send email
            emailService.sendPasswordResetEmail(employee.getName(), email, resetLink);

            return "If the email address is registered with us, you will receive a password reset link shortly.";

        } catch (Exception e) {
            throw new RuntimeException("Failed to process password reset request");
        }
    }

    @Transactional
    public String resetPassword(String token, String newPassword) {
        try {
            // Validate token
            Optional<PasswordResetToken> tokenOpt = passwordResetTokenRepository.findByToken(token);
            if (tokenOpt.isEmpty()) {
                throw new IllegalArgumentException("Invalid reset token");
            }

            PasswordResetToken resetToken = tokenOpt.get();

            // Check expiration
            if (resetToken.isExpired()) {
                passwordResetTokenRepository.delete(resetToken);
                throw new IllegalArgumentException("Reset token has expired. Please request a new password reset.");
            }

            // Check if used
            if (resetToken.isUsed()) {
                throw new IllegalArgumentException("Reset token has already been used. Please request a new password reset.");
            }

            // Get employee
            Optional<Employee> employeeOpt = employeeRepository.findById(resetToken.getEmail());
            if (employeeOpt.isEmpty()) {
                passwordResetTokenRepository.delete(resetToken);
                throw new IllegalArgumentException("Employee account not found");
            }

            Employee employee = employeeOpt.get();

            // Update password
            String encodedPassword = passwordEncoder.encode(newPassword);
            employee.setPassword(encodedPassword);
            employeeRepository.save(employee);

            // Mark token as used
            resetToken.setUsed(true);
            passwordResetTokenRepository.save(resetToken);

            // Send confirmation email (ignore failures)
            try {
                emailService.sendPasswordResetConfirmationEmail(employee.getName(), employee.getEmail());
            } catch (Exception ignored) {
                // Ignore email failures
            }

            return "Password has been reset successfully. You can now login with your new password.";

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to reset password");
        }
    }

    public boolean isValidToken(String token) {
        try {
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

        } catch (Exception e) {
            return false;
        }
    }

    @Transactional
    public void cleanupExpiredTokens() {
        try {
            passwordResetTokenRepository.deleteExpiredTokens(LocalDateTime.now());
        } catch (Exception ignored) {
            // Ignore cleanup failures
        }
    }

    public boolean isEmailRegistered(String email) {
        try {
            return employeeRepository.existsById(email);
        } catch (Exception e) {
            return false;
        }
    }

    public Optional<Employee> getEmployeeForVerification(String email) {
        try {
            return employeeRepository.findById(email);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}