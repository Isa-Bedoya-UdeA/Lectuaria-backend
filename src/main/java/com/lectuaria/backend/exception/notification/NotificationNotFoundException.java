package com.lectuaria.backend.exception.notification;

import com.lectuaria.backend.exception.DomainException;
import org.springframework.http.HttpStatus;

/**
 * Notificacion no encontrada. HTTP 404.
 */
public class NotificationNotFoundException extends DomainException {
    public NotificationNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, "NOTIFICATION_NOT_FOUND");
    }
}
