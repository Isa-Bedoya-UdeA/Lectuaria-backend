package com.lectuaria.backend.security;

import com.lectuaria.backend.exception.UnauthorizedException;
import com.lectuaria.backend.model.auth.User;
import com.lectuaria.backend.repository.auth.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Helper para resolver el usuario autenticado a partir de la peticion HTTP.
 *
 * Centraliza la logica que antes estaba duplicada como metodo privado
 * {@code extractUser(HttpServletRequest)} en cada controller, lo cual
 * violaba el principio DRY.
 *
 * Modos de uso:
 *   - {@link #requireCurrentUser(HttpServletRequest)}   -> lanza 401 si no hay token
 *   - {@link #tryGetCurrentUser(HttpServletRequest)}    -> devuelve {@code null} si no hay token
 *   - {@link #requireCurrentUserId()}                  -> usa el SecurityContextHolder de Spring Security
 *   - {@link #tryGetCurrentUserId()}                   -> idem pero sin lanzar
 *
 * En los controllers se recomienda inyectar este componente y reemplazar
 * los metodos privados {@code extractUser} / {@code getCurrentUserId}.
 */
@Component
public class AuthenticatedUserResolver {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public AuthenticatedUserResolver(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    /**
     * Resuelve el usuario a partir del header Authorization: Bearer &lt;token&gt;.
     * Lanza 401 si el token no esta presente, es invalido o el usuario no existe.
     */
    @NonNull
    public User requireCurrentUser(HttpServletRequest request) {
        String token = extractBearerToken(request);
        String email = jwtService.extractEmail(token);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Usuario no encontrado"));
    }

    /**
     * Variante tolerante: si no hay token, devuelve {@code null}.
     * Usado por endpoints publicos que opcionalmente enriquecen la respuesta
     * con informacion del usuario autenticado.
     */
    public User tryGetCurrentUser(HttpServletRequest request) {
        try {
            String token = extractBearerTokenOrNull(request);
            if (token == null) {
                return null;
            }
            String email = jwtService.extractEmail(token);
            return userRepository.findByEmail(email).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Resuelve el id de usuario desde el SecurityContextHolder de Spring Security
     * (cuando el filtro JWT ya pobló la autenticacion). Lanza 401 si no hay auth.
     */
    @NonNull
    public Long requireCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("Usuario no autenticado");
        }
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new UnauthorizedException("Usuario no encontrado"));
        return user.getId();
    }

    /**
     * Variante tolerante: devuelve {@code null} si no hay auth.
     */
    public Long tryGetCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return userRepository.findByEmail(authentication.getName())
                .map(User::getId)
                .orElse(null);
    }

    private String extractBearerToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException("Token de autorización requerido");
        }
        return authHeader.substring(7);
    }

    private String extractBearerTokenOrNull(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        return authHeader.substring(7);
    }
}
