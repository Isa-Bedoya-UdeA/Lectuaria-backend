package com.lectuaria.backend.exception.handler;

import com.lectuaria.backend.dto.common.ApiError;
import com.lectuaria.backend.exception.list.CannotShareWithSelfException;
import com.lectuaria.backend.exception.list.InvalidTokenException;
import com.lectuaria.backend.exception.list.UserListShareLinkNotFoundException;
import com.lectuaria.backend.exception.list.UserListShareNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class UserListShareExceptionHandler {

    @ExceptionHandler(UserListShareNotFoundException.class)
    public ResponseEntity<ApiError> handleUserListShareNotFound(UserListShareNotFoundException ex) {
        ApiError error = new ApiError(HttpStatus.NOT_FOUND.value(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(UserListShareLinkNotFoundException.class)
    public ResponseEntity<ApiError> handleUserListShareLinkNotFound(UserListShareLinkNotFoundException ex) {
        ApiError error = new ApiError(HttpStatus.NOT_FOUND.value(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(CannotShareWithSelfException.class)
    public ResponseEntity<ApiError> handleCannotShareWithSelf(CannotShareWithSelfException ex) {
        ApiError error = new ApiError(HttpStatus.BAD_REQUEST.value(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ApiError> handleInvalidToken(InvalidTokenException ex) {
        ApiError error = new ApiError(HttpStatus.BAD_REQUEST.value(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
}
