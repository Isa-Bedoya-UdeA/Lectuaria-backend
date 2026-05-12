package com.lectuaria.backend.exception.list;

import com.lectuaria.backend.exception.BusinessException;

public class PrivateListException extends BusinessException {
    public PrivateListException(String message) {
        super(message);
    }
}
