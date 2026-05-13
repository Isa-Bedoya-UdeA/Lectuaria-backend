package com.lectuaria.backend.service.notification.impl;

import com.lectuaria.backend.dto.notification.NotificationDTO;
import com.lectuaria.backend.model.auth.User;
import com.lectuaria.backend.model.notification.Notification;
import com.lectuaria.backend.model.notification.NotificationPreference;
import com.lectuaria.backend.model.notification.NotificationType;
import com.lectuaria.backend.repository.auth.UserRepository;
import com.lectuaria.backend.repository.notification.NotificationPreferenceRepository;
import com.lectuaria.backend.repository.notification.NotificationRepository;
import com.lectuaria.backend.service.notification.INotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationServiceImpl implements INotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NotificationPreferenceRepository preferenceRepository;

    public NotificationServiceImpl(NotificationRepository notificationRepository, UserRepository userRepository,
                                    NotificationPreferenceRepository preferenceRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.preferenceRepository = preferenceRepository;
    }

    @Override
    @Transactional
    public NotificationDTO createNotification(Long userId, NotificationType notificationType, String message, Long referenceId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check if user has this notification type enabled
        NotificationPreference preference = preferenceRepository.findByUserIdAndNotificationType(userId, notificationType)
                .orElse(null);

        // If preference doesn't exist or is disabled, don't create notification
        if (preference == null || !preference.isEnabled()) {
            return null;
        }

        Notification notification = new Notification(user, notificationType, message, referenceId);
        notification = notificationRepository.save(notification);

        return mapToDTO(notification);
    }

    @Override
    public List<NotificationDTO> getUserNotifications(Long userId, Boolean unreadOnly) {
        List<Notification> notifications;
        if (unreadOnly != null && unreadOnly) {
            notifications = notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
        } else {
            notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        }
        return notifications.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public NotificationDTO markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        notification.setRead(true);
        notification = notificationRepository.save(notification);

        return mapToDTO(notification);
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        List<Notification> unreadNotifications = notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
        if (!unreadNotifications.isEmpty()) {
            unreadNotifications.forEach(notification -> notification.setRead(true));
            notificationRepository.saveAll(unreadNotifications);
        }
    }

    @Override
    @Transactional
    public void deleteNotification(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        notificationRepository.delete(notification);
    }

    @Override
    @Transactional
    public void deleteAllNotifications(Long userId) {
        List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        notificationRepository.deleteAll(notifications);
    }

    @Override
    @Transactional
    public void deleteOldNotifications() {
        Instant thirtyDaysAgo = Instant.now().minus(java.time.Duration.ofDays(30));
        notificationRepository.deleteByUserIdAndIsReadTrueAndCreatedAtBefore(null, thirtyDaysAgo);
    }

    @Override
    public Long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    private NotificationDTO mapToDTO(Notification notification) {
        return new NotificationDTO(
                notification.getId(),
                notification.getNotificationType(),
                notification.getMessage(),
                notification.getReferenceId(),
                notification.getShareToken(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}
