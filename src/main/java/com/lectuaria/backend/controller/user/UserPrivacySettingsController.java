package com.lectuaria.backend.controller.user;

import com.lectuaria.backend.dto.user.UpdatePrivacySettingsRequestDTO;
import com.lectuaria.backend.dto.user.UserPrivacySettingsDTO;
import com.lectuaria.backend.model.auth.User;
import com.lectuaria.backend.repository.auth.UserRepository;
import com.lectuaria.backend.security.JwtService;
import com.lectuaria.backend.service.user.IUserPrivacySettingsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/me/privacy")
public class UserPrivacySettingsController {

    private final IUserPrivacySettingsService privacyService;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public UserPrivacySettingsController(IUserPrivacySettingsService privacyService,
                                          JwtService jwtService,
                                          UserRepository userRepository) {
        this.privacyService = privacyService;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<UserPrivacySettingsDTO> getMyPrivacySettings(HttpServletRequest request) {
        User user = extractUser(request);
        return ResponseEntity.ok(privacyService.getSettings(user.getId()));
    }

    @PutMapping
    public ResponseEntity<UserPrivacySettingsDTO> updateMyPrivacySettings(
            HttpServletRequest request,
            @Valid @RequestBody UpdatePrivacySettingsRequestDTO dto) {
        User user = extractUser(request);
        return ResponseEntity.ok(privacyService.updateSettings(user.getId(), dto));
    }

    private User extractUser(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new com.lectuaria.backend.exception.UnauthorizedException("Token requerido");
        }
        String token = authHeader.substring(7);
        String email = jwtService.extractEmail(token);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new com.lectuaria.backend.exception.ResourceNotFoundException("Usuario no encontrado"));
    }
}