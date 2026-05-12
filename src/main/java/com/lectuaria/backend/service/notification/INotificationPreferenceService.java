package com.lectuaria.backend.service.notification;

import com.lectuaria.backend.dto.notification.NotificationPreferenceDTO;
import com.lectuaria.backend.model.notification.NotificationType;

import java.util.List;

public interface INotificationPreferenceService {
    List<NotificationPreferenceDTO> getUserPreferences(Long userId);
    NotificationPreferenceDTO updatePreference(Long userId, NotificationType notificationType, boolean isEnabled);
    void resetToDefaults(Long userId);
}
