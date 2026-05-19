package com.lectuaria.backend.controller.library;

import com.lectuaria.backend.dto.library.LibrarySummaryDTO;
import com.lectuaria.backend.dto.statistics.LibraryStatisticsDTO;
import com.lectuaria.backend.exception.UnauthorizedException;
import com.lectuaria.backend.model.auth.User;
import com.lectuaria.backend.repository.auth.UserRepository;
import com.lectuaria.backend.security.JwtService;
import com.lectuaria.backend.service.library.ILibraryService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/libraries")
public class LibraryController {

    private final ILibraryService libraryService;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public LibraryController(ILibraryService libraryService, JwtService jwtService, UserRepository userRepository) {
        this.libraryService = libraryService;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<List<LibrarySummaryDTO>> getAllLibraries() {
        return ResponseEntity.ok(libraryService.getAllLibraries());
    }

     @GetMapping("/me/statistics")
    public ResponseEntity<LibraryStatisticsDTO> getMyLibraryStatistics(HttpServletRequest request) {
        return ResponseEntity.ok(libraryService.getMyLibraryStatistics(extractUser(request)));
    }

    private User extractUser(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException("Token de autorización requerido");
        }
        String token = authHeader.substring(7);
        String email = jwtService.extractEmail(token);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Usuario no encontrado"));
    }
}
