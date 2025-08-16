package com.example.met.repository;

import com.example.met.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    /**
     * Find a reset token by its token string
     */
    Optional<PasswordResetToken> findByToken(String token);

    /**
     * Find active (unused and not expired) token for an email
     */
    @Query("SELECT p FROM PasswordResetToken p WHERE p.email = :email AND p.used = false AND p.expiresAt > :now")
    Optional<PasswordResetToken> findActiveTokenByEmail(@Param("email") String email, @Param("now") LocalDateTime now);

    /**
     * Find any unused token for an email (regardless of expiration)
     */
    Optional<PasswordResetToken> findByEmailAndUsedFalse(String email);

    /**
     * Find all tokens for a specific email
     */
    List<PasswordResetToken> findByEmailOrderByCreatedAtDesc(String email);

    /**
     * Delete all tokens for a specific email
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM PasswordResetToken p WHERE p.email = :email")
    int deleteByEmail(@Param("email") String email);

    /**
     * Delete expired tokens
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM PasswordResetToken p WHERE p.expiresAt < :now")
    int deleteExpiredTokens(@Param("now") LocalDateTime now);

    /**
     * Delete used tokens older than specified date
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM PasswordResetToken p WHERE p.used = true AND p.createdAt < :cutoffDate")
    int deleteOldUsedTokens(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Count active tokens for an email
     */
    @Query("SELECT COUNT(p) FROM PasswordResetToken p WHERE p.email = :email AND p.used = false AND p.expiresAt > :now")
    long countActiveTokensByEmail(@Param("email") String email, @Param("now") LocalDateTime now);

    /**
     * Find all expired tokens
     */
    @Query("SELECT p FROM PasswordResetToken p WHERE p.expiresAt < :now")
    List<PasswordResetToken> findExpiredTokens(@Param("now") LocalDateTime now);

    /**
     * Find tokens that will expire within specified hours
     */
    @Query("SELECT p FROM PasswordResetToken p WHERE p.expiresAt BETWEEN :now AND :futureTime AND p.used = false")
    List<PasswordResetToken> findTokensExpiringWithin(@Param("now") LocalDateTime now, @Param("futureTime") LocalDateTime futureTime);

    /**
     * Check if email has any recent reset requests (within last hour)
     */
    @Query("SELECT COUNT(p) > 0 FROM PasswordResetToken p WHERE p.email = :email AND p.createdAt > :oneHourAgo")
    boolean hasRecentResetRequest(@Param("email") String email, @Param("oneHourAgo") LocalDateTime oneHourAgo);
}