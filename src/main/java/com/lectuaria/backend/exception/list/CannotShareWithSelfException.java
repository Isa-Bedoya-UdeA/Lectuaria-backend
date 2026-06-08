package com.lectuaria.backend.exception.list;

import com.lectuaria.backend.exception.ConflictException;

import java.util.List;

/**
 * Intento de compartir una lista con uno mismo. HTTP 409.
 */
public class CannotShareWithSelfException extends ConflictException {
    public CannotShareWithSelfException(String message) {
        super(message, List.of("No puedes compartir una lista contigo mismo."));
    }
}
