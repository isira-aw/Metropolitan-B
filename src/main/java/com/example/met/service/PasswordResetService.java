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
    private final CoreEmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.password-reset.token-expiration-hours:24}")
    private int tokenExpirationHours;

    @Value("${app.frontend.reset-password-url:https://metropolitan-d-production.up.railway.app/reset-password}")
    private String resetPasswordUrl;

    public String initiatePasswordReset(String email) {
        try {
            if (email == null || email.trim().isEmpty()) {
                return "If the email address is registered with us, you will receive a password reset link shortly.";
            }

            Optional<Employee> employeeOpt = employeeRepository.findById(email);
            if (employeeOpt.isEmpty()) {
                return "If the email address is registered with us, you will receive a password reset link shortly.";
            }

            Employee employee = employeeOpt.get();

            // Clean up existing tokens
            try {
                passwordResetTokenRepository.deleteByEmail(email);
            } catch (Exception ignored) {
                // Ignore cleanup errors
            }

            // Generate new token
            String token = UUID.randomUUID().toString();
            LocalDateTime expiresAt = LocalDateTime.now().plusHours(tokenExpirationHours);

            // Save token
            PasswordResetToken resetToken = new PasswordResetToken(token, email, expiresAt);
            passwordResetTokenRepository.save(resetToken);

            // Create reset link
            String resetLink = resetPasswordUrl + "?token=" + token;

            // Send email using new email service
            try {
                emailService.sendPasswordResetEmail(email, employee.getName(), resetLink);
            } catch (Exception e) {
                // Delete token if email fails
                try {
                    passwordResetTokenRepository.delete(resetToken);
                } catch (Exception ignored) {
                    // Ignore delete errors
                }
                // Still return success message for security
            }

            return "If the email address is registered with us, you will receive a password reset link shortly.";

        } catch (Exception e) {
            return "If the email address is registered with us, you will receive a password reset link shortly.";
        }
    }

    @Transactional
    public String resetPassword(String token, String newPassword) {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid reset token");
        }

        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }

        Optional<PasswordResetToken> tokenOpt = passwordResetTokenRepository.findByToken(token);
        if (tokenOpt.isEmpty()) {
            throw new IllegalArgumentException("Invalid reset token");
        }

        PasswordResetToken resetToken = tokenOpt.get();

        if (resetToken.isExpired()) {
            try {
                passwordResetTokenRepository.delete(resetToken);
            } catch (Exception ignored) {}
            throw new IllegalArgumentException("Reset token has expired. Please request a new password reset.");
        }

        if (resetToken.isUsed()) {
            throw new IllegalArgumentException("Reset token has already been used. Please request a new password reset.");
        }

        Optional<Employee> employeeOpt = employeeRepository.findById(resetToken.getEmail());
        if (employeeOpt.isEmpty()) {
            try {
                passwordResetTokenRepository.delete(resetToken);
            } catch (Exception ignored) {}
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

        return "Password has been reset successfully. You can now login with your new password.";
    }

    public boolean isValidToken(String token) {
        try {
            if (token == null || token.trim().isEmpty()) {
                return false;
            }

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

    public void cleanupExpiredTokens() {
        try {
            passwordResetTokenRepository.deleteExpiredTokens(LocalDateTime.now());
        } catch (Exception ignored) {}
    }

    public boolean isEmailRegistered(String email) {
        try {
            if (email == null || email.trim().isEmpty()) {
                return false;
            }
            return employeeRepository.existsById(email);
        } catch (Exception e) {
            return false;
        }
    }

    public Optional<Employee> getEmployeeForVerification(String email) {
        try {
            if (email == null || email.trim().isEmpty()) {
                return Optional.empty();
            }
            return employeeRepository.findById(email);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}