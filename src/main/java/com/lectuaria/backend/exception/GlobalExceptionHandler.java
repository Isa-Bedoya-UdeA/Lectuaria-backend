package com.lectuaria.backend.exception;

import com.lectuaria.backend.dto.ErrorResponseDTO;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * Manejador global y unico de excepciones para toda la API REST.
 *
 * Jerarquía de excepciones usada (todas extienden {@link DomainException}):
 *   - BusinessException           -> 400  (regla de negocio violada)
 *   - ConflictException           -> 409  (estado conflictivo / duplicado)
 *   - ResourceNotFoundException   -> 404  (recurso no encontrado)
 *   - UnauthorizedException       -> 401  (no autenticado)
 *   - ForbiddenException          -> 403  (autenticado sin permisos)
 *   - ValidationException         -> 400  (validacion semantica con lista)
 *   - TokenException              -> 401  (token invalido)
 *   - InvalidCredentialsException -> 401  (credenciales invalidas)
 *
 * Cualquier excepcion no mapeada explicitamente cae en el handler
 * generico y se traduce a HTTP 500 con un mensaje neutro (nunca se
 * filtra el detalle interno al cliente).
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handler principal: maneja cualquier subclase de {@link DomainException}
     * usando el status HTTP que la excepcion declara.
     */
    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponseDTO> handleDomainException(
            DomainException ex, HttpServletRequest request) {
        logger.warn("Domain exception [{}] en {}: {}", ex.getCode(), request.getRequestURI(), ex.getMessage());
        // Si la excepcion expone una lista de errores (e.g. ValidationException,
        // ConflictException), la propagamos al cliente.
        List<String> errors = null;
        if (ex instanceof ValidationException) {
            errors = ((ValidationException) ex).getErrors();
        } else if (ex instanceof ConflictException) {
            errors = ((ConflictException) ex).getErrors();
        }
        return ResponseEntity.status(ex.getStatus())
                .body(new ErrorResponseDTO(ex.getMessage(), errors, ex.getCode(), null));
    }

    /**
     * Bean Validation (anotaciones @NotNull, @Size, etc. en DTOs).
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleBeanValidation(MethodArgumentNotValidException exception) {
        List<String> errors = exception.getBindingResult()
                .getAllErrors()
                .stream()
                .map(error -> error instanceof FieldError fieldError
                        ? fieldError.getDefaultMessage()
                        : error.getDefaultMessage())
                .toList();
        return ResponseEntity.badRequest()
                .body(new ErrorResponseDTO("No se pudo completar la solicitud.", errors, "VALIDATION_ERROR", null));
    }

    /**
     * Argumentos invalidos (IllegalArgumentException).
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDTO> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        logger.warn("Illegal argument en {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponseDTO(ex.getMessage(), null, "INVALID_ARGUMENT", null));
    }

    /**
     * Spring Security: usuario autenticado pero sin permisos.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDTO> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        logger.warn("Access denied en {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponseDTO("Acceso denegado", null, "FORBIDDEN", null));
    }

    /**
     * Spring Security: error de autenticacion.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponseDTO> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        logger.warn("Authentication error en {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponseDTO("No autenticado", null, "UNAUTHORIZED", null));
    }

    /**
     * Handler generico: cualquier otra excepcion no contemplada.
     * El detalle interno NO se filtra al cliente.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGenericException(Exception ex, HttpServletRequest request) {
        logger.error("Error inesperado en {}", request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponseDTO(
                        "Ocurrió un error inesperado en el servidor",
                        null,
                        "INTERNAL_ERROR",
                        null));
    }
}
