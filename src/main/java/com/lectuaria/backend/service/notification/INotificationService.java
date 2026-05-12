package com.lectuaria.backend.service.notification;

import com.lectuaria.backend.dto.notification.NotificationDTO;
import com.lectuaria.backend.model.notification.NotificationType;

import java.util.List;

public interface INotificationService {
    NotificationDTO createNotification(Long userId, NotificationType notificationType, String message, Long referenceId);
    List<NotificationDTO> getUserNotifications(Long userId, Boolean unreadOnly);
    NotificationDTO markAsRead(Long notificationId, Long userId);
    void markAllAsRead(Long userId);
    void deleteNotification(Long notificationId, Long userId);
    void deleteAllNotifications(Long userId);
    void deleteOldNotifications();
    Long getUnreadCount(Long userId);
}
