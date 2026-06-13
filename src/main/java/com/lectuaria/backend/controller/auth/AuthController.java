package com.lectuaria.backend.controller.auth;

import com.lectuaria.backend.dto.auth.LoginRequestDTO;
import com.lectuaria.backend.dto.auth.LoginResponseDTO;
import com.lectuaria.backend.dto.auth.ProfileResponseDTO;
import com.lectuaria.backend.dto.auth.ProfileUpdateRequestDTO;
import com.lectuaria.backend.dto.auth.RegisterRequestDTO;
import com.lectuaria.backend.dto.auth.RegisterResponseDTO;
import com.lectuaria.backend.dto.auth.ChangePasswordRequestDTO;
import com.lectuaria.backend.exception.UnauthorizedException;
import com.lectuaria.backend.service.auth.IAuthService;
import com.lectuaria.backend.security.JwtService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    // Nombre de la cookie que transporta el refresh token. Se usa en
    // login (set), logout (clear) y refresh (read). Constante para
    // evitar duplicacion (S1192) y centralizar el cambio de nombre.
    private static final String REFRESH_TOKEN_COOKIE = "refreshToken";

    // Duraciones de la cookie de refresh token (en segundos).
    private static final long REFRESH_TOKEN_MAX_AGE_SECONDS_REMEMBER_ME = 60L * 60L * 24L * 30L; // 30 días
    private static final long REFRESH_TOKEN_MAX_AGE_SECONDS_DEFAULT = 60L * 60L * 8L;                  // 8 horas


    private final IAuthService authService;
    private final JwtService jwtService;

    public AuthController(IAuthService authService, JwtService jwtService) {
        this.authService = authService;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<EntityModel<RegisterResponseDTO>> register(@Valid @RequestBody RegisterRequestDTO request) {
        RegisterResponseDTO body = authService.register(request);
        EntityModel<RegisterResponseDTO> model = EntityModel.of(body);
        model.add(linkTo(methodOn(AuthController.class).login(new LoginRequestDTO(), null, null)).withRel("login"));
        return ResponseEntity.status(HttpStatus.CREATED).body(model);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(
            @Valid @RequestBody LoginRequestDTO request,
            HttpServletRequest httpRequest,
            HttpServletResponse response) {

        String ipAddress = getClientIp(httpRequest);
        LoginResponseDTO loginResponse = authService.login(request, ipAddress);

        long maxAgeSeconds = request.isRememberMe()
                ? REFRESH_TOKEN_MAX_AGE_SECONDS_REMEMBER_ME
                : REFRESH_TOKEN_MAX_AGE_SECONDS_DEFAULT;

        @SuppressWarnings("null")
        ResponseCookie refreshCookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE, loginResponse.getRefreshToken())
                .httpOnly(true)
                .secure(false)
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

        ResponseCookie deleteCookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE, "")
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
    public ResponseEntity<EntityModel<ProfileResponseDTO>> getMyProfile(HttpServletRequest request) {
        String email = extractEmail(request);
        ProfileResponseDTO profile = authService.getProfile(email);
        return ResponseEntity.ok(EntityModel.of(profile,
                linkTo(methodOn(AuthController.class).getMyProfile(request)).withSelfRel(),
                linkTo(methodOn(AuthController.class).updateMyProfile(request, null)).withRel("update"),
                linkTo(methodOn(AuthController.class).changePassword(request, null)).withRel("change-password")));
    }

    @PutMapping("/me")
    public ResponseEntity<EntityModel<ProfileResponseDTO>> updateMyProfile(HttpServletRequest request,
            @Valid @RequestBody ProfileUpdateRequestDTO profileUpdateRequest) {
        String email = extractEmail(request);
        ProfileResponseDTO profile = authService.updateProfile(email, profileUpdateRequest);
        return ResponseEntity.ok(EntityModel.of(profile,
                linkTo(methodOn(AuthController.class).updateMyProfile(request, profileUpdateRequest)).withSelfRel()));
    }

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            HttpServletRequest request,
            @Valid @RequestBody ChangePasswordRequestDTO dto) {
        String email = extractEmail(request);
        authService.changePassword(email, dto.getCurrentPassword(), dto.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "Contraseña actualizada correctamente"));
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
        ResponseCookie refreshCookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE, newTokens.getRefreshToken())
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(REFRESH_TOKEN_MAX_AGE_SECONDS_REMEMBER_ME) // 30 días
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
                .filter(cookie -> REFRESH_TOKEN_COOKIE.equals(cookie.getName()))
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

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}