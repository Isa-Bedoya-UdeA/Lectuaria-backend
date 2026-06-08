package com.lectuaria.backend.exception.notification;

import com.lectuaria.backend.exception.DomainException;
import org.springframework.http.HttpStatus;

/**
 * Preferencia de notificacion no encontrada. HTTP 404.
 */
public class NotificationPreferenceNotFoundException extends DomainException {
    public NotificationPreferenceNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, "NOTIFICATION_PREFERENCE_NOT_FOUND");
    }
}
