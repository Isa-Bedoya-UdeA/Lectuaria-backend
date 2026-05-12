package com.lectuaria.backend.dto.notification;

import com.lectuaria.backend.model.notification.NotificationType;

import java.time.Instant;

public class NotificationDTO {
    private Long id;
    private NotificationType notificationType;
    private String message;
    private Long referenceId;
    private boolean isRead;
    private Instant createdAt;

    public NotificationDTO() {}

    public NotificationDTO(Long id, NotificationType notificationType, String message, Long referenceId, boolean isRead, Instant createdAt) {
        this.id = id;
        this.notificationType = notificationType;
        this.message = message;
        this.referenceId = referenceId;
        this.isRead = isRead;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public NotificationType getNotificationType() { return notificationType; }
    public String getMessage() { return message; }
    public Long getReferenceId() { return referenceId; }
    public boolean isRead() { return isRead; }
    public Instant getCreatedAt() { return createdAt; }

    public void setId(Long id) { this.id = id; }
    public void setNotificationType(NotificationType notificationType) { this.notificationType = notificationType; }
    public void setMessage(String message) { this.message = message; }
    public void setReferenceId(Long referenceId) { this.referenceId = referenceId; }
    public void setRead(boolean read) { isRead = read; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
