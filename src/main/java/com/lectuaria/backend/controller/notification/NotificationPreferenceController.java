package com.lectuaria.backend.controller.notification;

import com.lectuaria.backend.dto.notification.NotificationPreferenceDTO;
import com.lectuaria.backend.model.notification.NotificationType;
import com.lectuaria.backend.security.AuthenticatedUserResolver;
import com.lectuaria.backend.service.notification.INotificationPreferenceService;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/api/notification-preferences")
public class NotificationPreferenceController {

    private final INotificationPreferenceService preferenceService;
    private final AuthenticatedUserResolver userResolver;

    public NotificationPreferenceController(INotificationPreferenceService preferenceService,
                                            AuthenticatedUserResolver userResolver) {
        this.preferenceService = preferenceService;
        this.userResolver = userResolver;
    }

    @GetMapping
    public ResponseEntity<CollectionModel<NotificationPreferenceDTO>> getUserPreferences() {
        Long userId = userResolver.requireCurrentUserId();
        return ResponseEntity.ok(CollectionModel.of(preferenceService.getUserPreferences(userId),
                linkTo(methodOn(NotificationPreferenceController.class).getUserPreferences()).withSelfRel(),
                linkTo(methodOn(NotificationPreferenceController.class).resetToDefaults()).withRel("reset")));
    }

    @PutMapping("/{type}")
    public ResponseEntity<EntityModel<NotificationPreferenceDTO>> updatePreference(
            @PathVariable NotificationType type,
            @RequestParam boolean enabled) {
        Long userId = userResolver.requireCurrentUserId();
        return ResponseEntity.ok(EntityModel.of(preferenceService.updatePreference(userId, type, enabled),
                linkTo(methodOn(NotificationPreferenceController.class).updatePreference(type, enabled)).withSelfRel(),
                linkTo(methodOn(NotificationPreferenceController.class).getUserPreferences()).withRel("all")));
    }

    @PostMapping("/reset")
    public ResponseEntity<Void> resetToDefaults() {
        Long userId = userResolver.requireCurrentUserId();
        preferenceService.resetToDefaults(userId);
        return ResponseEntity.ok().build();
    }
}
