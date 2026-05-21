package com.lectuaria.backend.repository.auth;

import com.lectuaria.backend.model.auth.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);

    @Modifying
    @Query("UPDATE PasswordResetToken p SET p.used = true WHERE p.user.id = :userId AND p.used = false")
    void markAllAsUsedByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM PasswordResetToken p WHERE p.expiresAt < :now OR p.used = true")
    void deleteExpiredOrUsed(@Param("now") Instant now);

    @Modifying
    @Query("DELETE FROM PasswordResetToken p WHERE p.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}