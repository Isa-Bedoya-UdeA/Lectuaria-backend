package com.lectuaria.backend.exception.book;

import com.lectuaria.backend.dto.ErrorResponseDTO;
import com.lectuaria.backend.exception.BookAlreadyExistsInLibraryException;
import com.lectuaria.backend.exception.ConflictException;
import com.lectuaria.backend.exception.ForbiddenException;
import com.lectuaria.backend.exception.ResourceNotFoundException;
import com.lectuaria.backend.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
@Order(60)
public class BookExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(BookExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        logger.warn("Book resource not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponseDTO(ex.getMessage(), List.of("Recurso no encontrado")));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponseDTO> handleConflict(ConflictException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponseDTO(exception.getMessage(), exception.getErrors()));
    }

    @ExceptionHandler(BookAlreadyExistsInLibraryException.class)
    public ResponseEntity<ErrorResponseDTO> handleBookAlreadyExists(BookAlreadyExistsInLibraryException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponseDTO(ex.getMessage(), List.of("El libro ya existe en la biblioteca")));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponseDTO> handleUnauthorized(UnauthorizedException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponseDTO(ex.getMessage(), List.of("Inicia sesión para continuar.")));
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponseDTO> handleForbidden(ForbiddenException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponseDTO(ex.getMessage(), List.of("Acceso denegado")));
    }
}
