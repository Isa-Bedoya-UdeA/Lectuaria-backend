package com.lectuaria.backend.repository.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lectuaria.backend.model.auth.RefreshToken;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

}