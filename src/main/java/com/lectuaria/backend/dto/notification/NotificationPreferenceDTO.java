package com.lectuaria.backend.dto.notification;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.lectuaria.backend.model.notification.NotificationType;

public class NotificationPreferenceDTO {
    private Long id;
    private NotificationType notificationType;
    private boolean isEnabled;

    public NotificationPreferenceDTO() {}

    public NotificationPreferenceDTO(Long id, NotificationType notificationType, boolean isEnabled) {
        this.id = id;
        this.notificationType = notificationType;
        this.isEnabled = isEnabled;
    }

    public Long getId() { return id; }
    public NotificationType getNotificationType() { return notificationType; }
    @JsonProperty("isEnabled")
    public boolean isEnabled() { return isEnabled; }

    public void setId(Long id) { this.id = id; }
    public void setNotificationType(NotificationType notificationType) { this.notificationType = notificationType; }
    public void setEnabled(boolean enabled) { isEnabled = enabled; }
}
