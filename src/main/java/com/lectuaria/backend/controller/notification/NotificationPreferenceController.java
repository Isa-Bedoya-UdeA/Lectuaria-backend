package com.lectuaria.backend.controller.notification;

import com.lectuaria.backend.dto.notification.NotificationPreferenceDTO;
import com.lectuaria.backend.model.auth.User;
import com.lectuaria.backend.model.notification.NotificationType;
import com.lectuaria.backend.repository.auth.UserRepository;
import com.lectuaria.backend.service.notification.INotificationPreferenceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notification-preferences")
public class NotificationPreferenceController {

    private final INotificationPreferenceService preferenceService;
    private final UserRepository userRepository;

    public NotificationPreferenceController(INotificationPreferenceService preferenceService, UserRepository userRepository) {
        this.preferenceService = preferenceService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<List<NotificationPreferenceDTO>> getUserPreferences() {
        Long userId = getCurrentUserId();
        List<NotificationPreferenceDTO> preferences = preferenceService.getUserPreferences(userId);
        return ResponseEntity.ok(preferences);
    }

    @PutMapping("/{type}")
    public ResponseEntity<NotificationPreferenceDTO> updatePreference(
            @PathVariable NotificationType type,
            @RequestParam boolean enabled) {
        Long userId = getCurrentUserId();
        NotificationPreferenceDTO preference = preferenceService.updatePreference(userId, type, enabled);
        return ResponseEntity.ok(preference);
    }

    @PostMapping("/reset")
    public ResponseEntity<Void> resetToDefaults() {
        Long userId = getCurrentUserId();
        preferenceService.resetToDefaults(userId);
        return ResponseEntity.ok().build();
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            String email = authentication.getName();
            User user = userRepository.findByEmail(email).orElse(null);
            if (user != null) {
                return user.getId();
            }
        }
        return null;
    }
}
