package com.lectuaria.backend.dto.notification;

import com.lectuaria.backend.model.notification.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateNotificationRequestDTO {
    @NotNull
    private NotificationType notificationType;

    @NotBlank
    private String message;

    private Long referenceId;

    public CreateNotificationRequestDTO() {}

    public CreateNotificationRequestDTO(NotificationType notificationType, String message, Long referenceId) {
        this.notificationType = notificationType;
        this.message = message;
        this.referenceId = referenceId;
    }

    public NotificationType getNotificationType() { return notificationType; }
    public String getMessage() { return message; }
    public Long getReferenceId() { return referenceId; }

    public void setNotificationType(NotificationType notificationType) { this.notificationType = notificationType; }
    public void setMessage(String message) { this.message = message; }
    public void setReferenceId(Long referenceId) { this.referenceId = referenceId; }
}
