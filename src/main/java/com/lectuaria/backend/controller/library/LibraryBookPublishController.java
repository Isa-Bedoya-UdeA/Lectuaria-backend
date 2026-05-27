package com.lectuaria.backend.controller.library;

import com.lectuaria.backend.dto.book.BookPublishRequestDTO;
import com.lectuaria.backend.dto.book.BookPublishResponseDTO;
import com.lectuaria.backend.exception.UnauthorizedException;
import com.lectuaria.backend.model.auth.User;
import com.lectuaria.backend.model.library.Librarian;
import com.lectuaria.backend.security.JwtService;
import com.lectuaria.backend.service.book.IBookPublishService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.lectuaria.backend.repository.auth.UserRepository;
import com.lectuaria.backend.repository.library.LibrarianRepository;

@RestController
@RequestMapping("/api")
public class LibraryBookPublishController {

    private final IBookPublishService bookPublishService;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final LibrarianRepository librarianRepository;

    public LibraryBookPublishController(IBookPublishService bookPublishService, JwtService jwtService,
            UserRepository userRepository, LibrarianRepository librarianRepository) {
        this.bookPublishService = bookPublishService;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.librarianRepository = librarianRepository;
    }

    @PostMapping("/book-publish")
    public ResponseEntity<BookPublishResponseDTO> publishBook(
            @Valid @RequestBody BookPublishRequestDTO request,
            HttpServletRequest httpRequest) {

        Long librarianUserId = extractUserIdFromToken(httpRequest);

        BookPublishResponseDTO response = bookPublishService.publishBook(request, librarianUserId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/book-publish-test")
    public ResponseEntity<String> testEndpoint() {
        return ResponseEntity.ok("Endpoint de book-publish funciona sin autenticación");
    }

    @GetMapping("/check-role")
    public ResponseEntity<String> checkUserRole(HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                String email = jwtService.extractEmail(token);
                User user = userRepository.findByEmail(email).orElse(null);
                
                if (user != null) {
                    return ResponseEntity.ok("Usuario: " + email + ", Rol: " + user.getRole());
                }
            }
            return ResponseEntity.ok("Token inválido o usuario no encontrado");
        } catch (Exception e) {
            return ResponseEntity.ok("Error: " + e.getMessage());
        }
    }

    @GetMapping("/check-librarian")
    public ResponseEntity<String> checkLibrarianAssociation(HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                String email = jwtService.extractEmail(token);
                User user = userRepository.findByEmail(email).orElse(null);
                
                if (user != null) {
                    Librarian librarian = librarianRepository.findByUser(user).orElse(null);
                    if (librarian != null) {
                        return ResponseEntity.ok("Usuario: " + email + ", Biblioteca: " + librarian.getLibrary().getName());
                    } else {
                        return ResponseEntity.ok("Usuario: " + email + ", NO TIENE BIBLIOTECA ASOCIADA");
                    }
                }
            }
            return ResponseEntity.ok("Token inválido o usuario no encontrado");
        } catch (Exception e) {
            return ResponseEntity.ok("Error: " + e.getMessage());
        }
    }

    private Long extractUserIdFromToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException("Token requerido");
        }

        String token = authHeader.substring(7);

        String email = jwtService.extractEmail(token);

        // Buscar el usuario por email y retornar su ID
        return userRepository.findByEmail(email)
                .map(User::getId)
                .orElseThrow(() -> new UnauthorizedException("Usuario no encontrado"));
    }
}
