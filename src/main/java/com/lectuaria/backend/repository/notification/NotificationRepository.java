package com.lectuaria.backend.repository.notification;

import com.lectuaria.backend.model.notification.Notification;
import com.lectuaria.backend.model.notification.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId);

    Optional<Notification> findByIdAndUserId(Long id, Long userId);

    long countByUserIdAndIsReadFalse(Long userId);

    void deleteByUserIdAndIsReadTrueAndCreatedAtBefore(Long userId, Instant date);

    List<Notification> findByUserIdAndNotificationTypeOrderByCreatedAtDesc(Long userId, NotificationType notificationType);
}
