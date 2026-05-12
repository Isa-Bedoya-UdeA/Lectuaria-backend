package com.lectuaria.backend.controller.notification;

import com.lectuaria.backend.dto.notification.NotificationDTO;
import com.lectuaria.backend.model.auth.User;
import com.lectuaria.backend.repository.auth.UserRepository;
import com.lectuaria.backend.service.notification.INotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final INotificationService notificationService;
    private final UserRepository userRepository;

    public NotificationController(INotificationService notificationService, UserRepository userRepository) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<List<NotificationDTO>> getUserNotifications(
            @RequestParam(required = false) Boolean unreadOnly) {
        Long userId = getCurrentUserId();
        List<NotificationDTO> notifications = notificationService.getUserNotifications(userId, unreadOnly);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Long> getUnreadCount() {
        Long userId = getCurrentUserId();
        Long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(count);
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<NotificationDTO> markAsRead(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        NotificationDTO notification = notificationService.markAsRead(id, userId);
        return ResponseEntity.ok(notification);
    }

    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead() {
        Long userId = getCurrentUserId();
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        notificationService.deleteNotification(id, userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteAllNotifications() {
        Long userId = getCurrentUserId();
        notificationService.deleteAllNotifications(userId);
        return ResponseEntity.noContent().build();
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
