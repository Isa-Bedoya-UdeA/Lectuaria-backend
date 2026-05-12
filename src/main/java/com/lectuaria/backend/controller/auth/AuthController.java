package com.lectuaria.backend.controller.auth;

import com.lectuaria.backend.dto.auth.LoginRequestDTO;
import com.lectuaria.backend.dto.auth.LoginResponseDTO;
import com.lectuaria.backend.dto.auth.ProfileResponseDTO;
import com.lectuaria.backend.dto.auth.ProfileUpdateRequestDTO;
import com.lectuaria.backend.dto.auth.RegisterRequestDTO;
import com.lectuaria.backend.dto.auth.RegisterResponseDTO;
import com.lectuaria.backend.exception.UnauthorizedException;
import com.lectuaria.backend.service.auth.IAuthService;
import com.lectuaria.backend.security.JwtService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final IAuthService authService;
    private final JwtService jwtService;

    public AuthController(IAuthService authService, JwtService jwtService) {
        this.authService = authService;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponseDTO register(@Valid @RequestBody RegisterRequestDTO request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(
            @Valid @RequestBody LoginRequestDTO request,
            HttpServletResponse response) {

        LoginResponseDTO loginResponse = authService.login(request);

        long maxAgeSeconds = request.isRememberMe()
                ? 60 * 60 * 24 * 30 // 30 días
                : 60 * 60 * 8; // 8 horas

        @SuppressWarnings("null")
        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", loginResponse.getRefreshToken())
                .httpOnly(true)
                .secure(false) // Permitir en http://localhost
                .path("/")
                .maxAge(maxAgeSeconds)
                .sameSite("Lax")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(loginResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            HttpServletRequest request,
            HttpServletResponse response) {

        String refreshToken = extractRefreshTokenFromCookie(request);

        if (refreshToken != null) {
            authService.logout(refreshToken);
        }

        ResponseCookie deleteCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0)
                .sameSite("Lax")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
                .body(Map.of("message", "Sesión cerrada"));
    }

    @GetMapping("/me")
    public ProfileResponseDTO getMyProfile(HttpServletRequest request) {
        String email = extractEmail(request);
        return authService.getProfile(email);
    }

    @PutMapping("/me")
    public ProfileResponseDTO updateMyProfile(HttpServletRequest request,
            @Valid @RequestBody ProfileUpdateRequestDTO profileUpdateRequest) { // ← Agregar @Valid
        String email = extractEmail(request);
        return authService.updateProfile(email, profileUpdateRequest);
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponseDTO> refresh(
            HttpServletRequest request,
            HttpServletResponse response) {

        String refreshToken = extractRefreshTokenFromCookie(request);

        if (refreshToken == null) {
            throw new UnauthorizedException("Refresh token no encontrado");
        }

        LoginResponseDTO newTokens = authService.refresh(refreshToken);

        @SuppressWarnings("null")
        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", newTokens.getRefreshToken())
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(60 * 60 * 24 * 30) // 30 días
                .sameSite("Lax")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(newTokens);
    }

    @SuppressWarnings("null")
    private String extractRefreshTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }

        return Arrays.stream(request.getCookies())
                .filter(cookie -> "refreshToken".equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    private String extractEmail(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer "))
            throw new UnauthorizedException("Token requerido");

        String token = authHeader.substring(7);

        if (!jwtService.isValid(token))
            throw new UnauthorizedException("Token inválido");

        return jwtService.extractEmail(token);
    }
}