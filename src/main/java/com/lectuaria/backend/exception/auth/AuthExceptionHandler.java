package com.lectuaria.backend.exception.auth;

import com.lectuaria.backend.dto.ErrorResponseDTO;
import com.lectuaria.backend.exception.InvalidCredentialsException;
import com.lectuaria.backend.exception.TokenException;
import com.lectuaria.backend.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class AuthExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(AuthExceptionHandler.class);

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponseDTO> handleUnauthorized(UnauthorizedException exception, HttpServletRequest request) {
        logger.warn("Unauthorized access attempt for path: {}", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponseDTO(exception.getMessage(), List.of("Inicia sesión para continuar.")));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponseDTO> handleInvalidCredentials(InvalidCredentialsException ex, HttpServletRequest request) {
        logger.warn("Invalid credentials attempted for path: {}", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponseDTO(ex.getMessage(), List.of("Credenciales inválidas")));
    }

    @ExceptionHandler(TokenException.class)
    public ResponseEntity<ErrorResponseDTO> handleTokenException(TokenException ex, HttpServletRequest request) {
        logger.warn("Token error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponseDTO(ex.getMessage(), List.of("Error de token")));
    }
}
