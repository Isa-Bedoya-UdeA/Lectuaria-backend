package com.lectuaria.backend.exception.list;

import com.lectuaria.backend.exception.ConflictException;

import java.util.List;

/**
 * La lista ya fue compartida con ese usuario. HTTP 409.
 */
public class AlreadySharedException extends ConflictException {
    public AlreadySharedException(String message) {
        super(message, List.of("La lista ya fue compartida con ese usuario."));
    }
}
