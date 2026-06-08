package com.lectuaria.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lectuaria.backend.dto.ErrorResponseDTO;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public JwtAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        // Usa el mismo ErrorResponseDTO estandarizado que el GlobalExceptionHandler,
        // para que el cliente siempre vea el mismo shape de error.
        ErrorResponseDTO body = new ErrorResponseDTO(
                "No autenticado",
                List.of("Inicia sesion para continuar."),
                "UNAUTHORIZED",
                null);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
