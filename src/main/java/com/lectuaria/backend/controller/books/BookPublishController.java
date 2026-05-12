package com.lectuaria.backend.controller.books;

import com.lectuaria.backend.dto.book.BookPublishRequestDTO;
import com.lectuaria.backend.dto.book.BookPublishResponseDTO;
import com.lectuaria.backend.exception.UnauthorizedException;
import com.lectuaria.backend.model.auth.User;
import com.lectuaria.backend.security.JwtService;
import com.lectuaria.backend.service.book.IBookPublishService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.lectuaria.backend.repository.auth.UserRepository;
import org.springframework.lang.NonNull;

@RestController
@RequestMapping("/api/books")
public class BookPublishController {

    private static final Logger LOGGER = LoggerFactory.getLogger(BookPublishController.class);
    private final IBookPublishService bookPublishService;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    public BookPublishController(IBookPublishService bookPublishService, JwtService jwtService,
            UserRepository userRepository) {
        this.bookPublishService = bookPublishService;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    @PostMapping("/publish")
    public ResponseEntity<BookPublishResponseDTO> publishBook(
            @Valid @RequestBody BookPublishRequestDTO request,
            HttpServletRequest httpRequest) {

        // Extraer user ID del token JWT (necesitas implementar este método)
        Long librarianUserId = extractUserIdFromToken(httpRequest);

        BookPublishResponseDTO response = bookPublishService.publishBook(request, librarianUserId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/prefill/{isbn}")
    public ResponseEntity<BookPublishRequestDTO> prefillFromOpenLibrary(@PathVariable @NonNull Long isbn,
            HttpServletRequest httpRequest) {
        // Extraer librarianUserId del token JWT
        Long librarianUserId = extractUserIdFromToken(httpRequest);

        BookPublishRequestDTO request = bookPublishService.prefillFromOpenLibrary(isbn, librarianUserId);

        LOGGER.info("Controller - Response data: Title={}, Authors={}, Description={}, CoverUrl={}, Publishers={}",
                request.getTitle(),
                request.getAuthors(),
                request.getDescription(),
                request.getCoverUrl(),
                request.getPublishers());

        return ResponseEntity.ok(request);
    }

    @SuppressWarnings("null")
    private @NonNull Long extractUserIdFromToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException("Token requerido");
        }

        String token = authHeader.substring(7);

        // Usar tu JwtService existente para extraer el email
        String email = jwtService.extractEmail(token);

        // Buscar el usuario por email y retornar su ID
        return userRepository.findByEmail(email)
                .map(User::getId)
                .orElseThrow(() -> new UnauthorizedException("Usuario no encontrado"));
    }
}