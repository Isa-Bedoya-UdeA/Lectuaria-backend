package com.lectuaria.backend.repository.notification;

import com.lectuaria.backend.model.notification.NotificationPreference;
import com.lectuaria.backend.model.notification.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, Long> {

    List<NotificationPreference> findByUserId(Long userId);

    Optional<NotificationPreference> findByUserIdAndNotificationType(Long userId, NotificationType notificationType);

    void deleteByUserId(Long userId);
}
