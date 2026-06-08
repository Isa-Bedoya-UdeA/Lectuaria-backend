package com.lectuaria.backend.controller.notification;

import com.lectuaria.backend.dto.notification.NotificationDTO;
import com.lectuaria.backend.security.AuthenticatedUserResolver;
import com.lectuaria.backend.service.notification.INotificationService;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final INotificationService notificationService;
    private final AuthenticatedUserResolver userResolver;

    public NotificationController(INotificationService notificationService,
                                 AuthenticatedUserResolver userResolver) {
        this.notificationService = notificationService;
        this.userResolver = userResolver;
    }

    @GetMapping
    public ResponseEntity<CollectionModel<NotificationDTO>> getUserNotifications(
            @RequestParam(required = false) Boolean unreadOnly) {
        Long userId = userResolver.requireCurrentUserId();
        List<NotificationDTO> notifications = notificationService.getUserNotifications(userId, unreadOnly);
        return ResponseEntity.ok(CollectionModel.of(notifications,
                linkTo(methodOn(NotificationController.class).getUserNotifications(unreadOnly)).withSelfRel(),
                linkTo(methodOn(NotificationController.class).getUnreadCount()).withRel("unread-count"),
                linkTo(methodOn(NotificationController.class).markAllAsRead()).withRel("mark-all-read")));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<EntityModel<java.util.Map<String, Long>>> getUnreadCount() {
        Long userId = userResolver.requireCurrentUserId();
        Long count = notificationService.getUnreadCount(userId);
        java.util.Map<String, Long> body = java.util.Collections.singletonMap("unreadCount", count);
        return ResponseEntity.ok(EntityModel.of(body,
                linkTo(methodOn(NotificationController.class).getUnreadCount()).withSelfRel(),
                linkTo(methodOn(NotificationController.class).getUserNotifications(null)).withRel("notifications")));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<EntityModel<NotificationDTO>> markAsRead(@PathVariable Long id) {
        Long userId = userResolver.requireCurrentUserId();
        NotificationDTO notification = notificationService.markAsRead(id, userId);
        return ResponseEntity.ok(EntityModel.of(notification,
                linkTo(methodOn(NotificationController.class).markAsRead(id)).withSelfRel(),
                linkTo(methodOn(NotificationController.class).deleteNotification(id)).withRel("delete")));
    }

    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead() {
        Long userId = userResolver.requireCurrentUserId();
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(@PathVariable Long id) {
        Long userId = userResolver.requireCurrentUserId();
        notificationService.deleteNotification(id, userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteAllNotifications() {
        Long userId = userResolver.requireCurrentUserId();
        notificationService.deleteAllNotifications(userId);
        return ResponseEntity.noContent().build();
    }
}
