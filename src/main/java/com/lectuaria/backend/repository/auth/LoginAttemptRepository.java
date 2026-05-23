package com.lectuaria.backend.repository.auth;

import com.lectuaria.backend.model.auth.LoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {

    List<LoginAttempt> findByUserIdOrderByAttemptedAtDesc(Long userId);

    @Query("SELECT COUNT(la) FROM LoginAttempt la " +
            "WHERE la.user.id = :userId AND la.success = false AND la.attemptedAt >= :since")
    long countFailedSince(@Param("userId") Long userId, @Param("since") Instant since);

    @Query("SELECT COUNT(la) FROM LoginAttempt la " +
            "WHERE la.ipAddress = :ipAddress AND la.success = false AND la.attemptedAt >= :since")
    long countFailedByIpSince(@Param("ipAddress") String ipAddress, @Param("since") Instant since);

    @Modifying
    @Query("DELETE FROM LoginAttempt la WHERE la.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM LoginAttempt la WHERE la.attemptedAt < :before")
    void deleteOlderThan(@Param("before") Instant before);
}