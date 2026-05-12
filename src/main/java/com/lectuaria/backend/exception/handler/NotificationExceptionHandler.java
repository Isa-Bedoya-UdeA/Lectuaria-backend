package com.lectuaria.backend.exception.handler;

import com.lectuaria.backend.dto.common.ApiError;
import com.lectuaria.backend.exception.notification.NotificationNotFoundException;
import com.lectuaria.backend.exception.notification.NotificationPreferenceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class NotificationExceptionHandler {

    @ExceptionHandler(NotificationNotFoundException.class)
    public ResponseEntity<ApiError> handleNotificationNotFound(NotificationNotFoundException ex) {
        ApiError error = new ApiError(HttpStatus.NOT_FOUND.value(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(NotificationPreferenceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotificationPreferenceNotFound(NotificationPreferenceNotFoundException ex) {
        ApiError error = new ApiError(HttpStatus.NOT_FOUND.value(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
}
