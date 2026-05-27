package com.lectuaria.backend.security;

import io.jsonwebtoken.Claims;
import java.util.function.Function;

public interface JwtService {

    String generateAccessToken(String email, String role);
    String generateRefreshToken(String email);

    String extractEmail(String token);
    String extractRole(String token);
    boolean isValid(String token);
    boolean isTokenValid(String token, String userEmail);
    <T> T extractClaim(String token, Function<Claims, T> claimsResolver);
    Claims extractAllClaims(String token);
}
