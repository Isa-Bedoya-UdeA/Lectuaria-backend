package com.lectuaria.backend.controller.friendship;

import com.lectuaria.backend.dto.common.UserSearchResponseDTO;
import com.lectuaria.backend.exception.UnauthorizedException;
import com.lectuaria.backend.model.auth.User;
import com.lectuaria.backend.model.auth.UserRole;
import com.lectuaria.backend.repository.auth.UserRepository;
import com.lectuaria.backend.security.JwtService;
import com.lectuaria.backend.service.friendship.IFriendshipService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/friendships")
public class FriendshipController {

    private final IFriendshipService friendshipService;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private static final Logger logger = LoggerFactory.getLogger(FriendshipController.class);

    public FriendshipController(IFriendshipService friendshipService, JwtService jwtService,
            UserRepository userRepository) {
        this.friendshipService = friendshipService;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @GetMapping("/search")
    public ResponseEntity<List<UserSearchResponseDTO>> searchReaders(
            @RequestParam String query,
            HttpServletRequest request) {
        // Search is public - extract user optionally for friendship status enrichment
        User user = tryExtractUser(request);
        if (user != null && user.getRole() == UserRole.LIBRARIAN) {
            throw new UnauthorizedException("Los bibliotecarios no tienen acceso a opciones de amistad");
        }
        return ResponseEntity.ok(friendshipService.searchReaders(query, user));
    }

    @GetMapping
    public ResponseEntity<List<UserSearchResponseDTO>> getFriends(HttpServletRequest request) {
        User user = extractUserFromRequest(request);
        if (user.getRole() == UserRole.LIBRARIAN) {
            throw new UnauthorizedException("Los bibliotecarios no tienen acceso a opciones de amistad");
        }
        return ResponseEntity.ok(friendshipService.getFriends(user));
    }

    @GetMapping("/requests/pending")
    public ResponseEntity<List<UserSearchResponseDTO>> getPendingRequests(HttpServletRequest request) {
        User user = extractUserFromRequest(request);
        if (user.getRole() == UserRole.LIBRARIAN) {
            throw new UnauthorizedException("Los bibliotecarios no tienen acceso a opciones de amistad");
        }
        return ResponseEntity.ok(friendshipService.getPendingRequests(user));
    }

    @PostMapping("/requests/{receiverId}")
    public ResponseEntity<Void> sendFriendshipRequest(
            @PathVariable Long receiverId,
            HttpServletRequest request) {
        User user = extractUserFromRequest(request);
        if (user.getRole() == UserRole.LIBRARIAN) {
            throw new UnauthorizedException("Los bibliotecarios no tienen acceso a opciones de amistad");
        }
        friendshipService.sendFriendshipRequest(receiverId, user);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/requests/{requestId}/accept")
    public ResponseEntity<Void> acceptFriendshipRequest(
            @PathVariable Long requestId,
            HttpServletRequest request) {
        User user = extractUserFromRequest(request);
        friendshipService.acceptFriendshipRequest(requestId, user);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/requests/{requestId}/reject")
    public ResponseEntity<Void> rejectFriendshipRequest(
            @PathVariable Long requestId,
            HttpServletRequest request) {
        User user = extractUserFromRequest(request);
        friendshipService.rejectFriendshipRequest(requestId, user);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/requests/{requestId}")
    public ResponseEntity<Void> cancelFriendshipRequest(
            @PathVariable Long requestId,
            HttpServletRequest request) {
        User user = extractUserFromRequest(request);
        friendshipService.cancelFriendshipRequest(requestId, user);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{friendId}")
    public ResponseEntity<Void> removeFriendship(
            @PathVariable Long friendId,
            HttpServletRequest request) {
        User user = extractUserFromRequest(request);
        if (user.getRole() == UserRole.LIBRARIAN) {
            throw new UnauthorizedException("Los bibliotecarios no tienen acceso a opciones de amistad");
        }
        friendshipService.removeFriendship(friendId, user);
        return ResponseEntity.ok().build();
    }

    private @NonNull User extractUserFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.warn("Authorization header missing or invalid for URI: {}", request.getRequestURI());
            throw new UnauthorizedException("Token de autorización requerido");
        }

        String token = authHeader.substring(7);
        String email;
        try {
            email = jwtService.extractEmail(token);
        } catch (Exception e) {
            logger.error("Error extracting email from token: {}", e.getMessage());
            throw new UnauthorizedException("Token inválido o expirado");
        }

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Usuario no encontrado"));
    }

    /**
     * Tries to extract the user from the request without throwing if no token is
     * present.
     */
    private User tryExtractUser(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        try {
            String token = authHeader.substring(7);
            String email = jwtService.extractEmail(token);
            return userRepository.findByEmail(email).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
}
