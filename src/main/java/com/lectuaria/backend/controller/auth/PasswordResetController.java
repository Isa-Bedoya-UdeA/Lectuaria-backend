package com.lectuaria.backend.controller.auth;

import com.lectuaria.backend.dto.auth.PasswordResetRequestDTO;
import com.lectuaria.backend.dto.auth.ForgotPasswordRequestDTO;
import com.lectuaria.backend.service.auth.IPasswordResetService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class PasswordResetController {

    private final IPasswordResetService passwordResetService;

    public PasswordResetController(IPasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    /**
     * Request a password reset email.
     * Always returns 200 OK to prevent email enumeration attacks.
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequestDTO request) {
        passwordResetService.requestPasswordReset(request.getEmail());
        return ResponseEntity.ok(Map.of("message", "Si el correo existe, se ha enviado un enlace de recuperación"));
    }

    /**
     * Reset password using a token.
     */
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @RequestBody PasswordResetRequestDTO request) {
        passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "Contraseña actualizada correctamente"));
    }
}