package com.lectuaria.backend.service.auth.impl;

import com.lectuaria.backend.exception.BusinessException;
import com.lectuaria.backend.exception.ResourceNotFoundException;
import com.lectuaria.backend.model.auth.PasswordResetToken;
import com.lectuaria.backend.model.auth.User;
import com.lectuaria.backend.repository.auth.PasswordResetTokenRepository;
import com.lectuaria.backend.repository.auth.UserRepository;
import com.lectuaria.backend.service.auth.IEmailService;
import com.lectuaria.backend.service.auth.IPasswordResetService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

@Service
public class PasswordResetServiceImpl implements IPasswordResetService {

    // Reutilizable: SecureRandom es thread-safe y costoso de inicializar.
    // Lo dejamos como campo estatico para que todas las instancias lo
    // compartan en vez de crear uno nuevo en cada llamada a generateSecureToken.
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();


    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final IEmailService emailService;

    @Value("${frontend.url}")
    private String frontendUrl;

    @Value("${platform.name:Lectuaria}")
    private String platformName;

    private static final int EXPIRY_HOURS = 24;

    public PasswordResetServiceImpl(UserRepository userRepository,
                                    PasswordResetTokenRepository tokenRepository,
                                    PasswordEncoder passwordEncoder,
                                    IEmailService emailService) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @Override
    @Transactional
    public void requestPasswordReset(String email) {
        userRepository.findByEmail(email.toLowerCase()).ifPresent(user -> {
            // Invalidate any existing tokens for this user
            tokenRepository.markAllAsUsedByUserId(user.getId());

            // Generate a secure token
            String token = generateSecureToken();
            Instant expiresAt = Instant.now().plus(EXPIRY_HOURS, ChronoUnit.HOURS);

            // Create and save the token
            PasswordResetToken resetToken = new PasswordResetToken(token, user, expiresAt);
            tokenRepository.save(resetToken);

            // Build reset URL
            String resetUrl = frontendUrl + "/reset-password?token=" + token;

            // Send email
            emailService.sendPasswordResetEmail(
                    user.getEmail(),
                    user.getFullName(),
                    resetUrl,
                    EXPIRY_HOURS
            );
        });
        // Always return success to prevent email enumeration attacks
    }

    @Override
    @Transactional
    public void resetPassword(String token, String newPassword) {
        // Validate password confirmation
        if (newPassword == null || newPassword.length() < 8) {
            throw new BusinessException("La contraseña debe tener al menos 8 caracteres");
        }

        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new BusinessException("Token inválido o expirado"));

        if (!resetToken.isValid()) {
            throw new BusinessException("Token inválido o expirado");
        }

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Mark token as used
        resetToken.setUsed(true);
        tokenRepository.save(resetToken);
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}