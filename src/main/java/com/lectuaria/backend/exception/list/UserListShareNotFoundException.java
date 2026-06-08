package com.lectuaria.backend.exception.list;

import com.lectuaria.backend.exception.DomainException;
import org.springframework.http.HttpStatus;

/**
 * Lista compartida no encontrada. HTTP 404.
 */
public class UserListShareNotFoundException extends DomainException {
    public UserListShareNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, "USER_LIST_SHARE_NOT_FOUND");
    }
}
