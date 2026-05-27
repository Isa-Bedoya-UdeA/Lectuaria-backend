package com.lectuaria.backend.controller.books;

import com.lectuaria.backend.dto.book.BookPublishRequestDTO;
import com.lectuaria.backend.dto.book.BookPublishResponseDTO;
import com.lectuaria.backend.exception.UnauthorizedException;
import com.lectuaria.backend.model.auth.User;
import com.lectuaria.backend.security.JwtService;
import com.lectuaria.backend.service.book.IBookPublishService;
import com.lectuaria.backend.service.storage.S3StorageService;
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
    private final S3StorageService s3StorageService;

    public BookPublishController(IBookPublishService bookPublishService, JwtService jwtService,
            UserRepository userRepository, S3StorageService s3StorageService) {
        this.bookPublishService = bookPublishService;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.s3StorageService = s3StorageService;
    }

    @PostMapping("/publish")
    public ResponseEntity<BookPublishResponseDTO> publishBook(
            @Valid @RequestBody BookPublishRequestDTO request,
            HttpServletRequest httpRequest) {

        Long librarianUserId = extractUserIdFromToken(httpRequest);

        BookPublishResponseDTO response = bookPublishService.publishBook(request, librarianUserId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/publish-with-cover")
    public ResponseEntity<BookPublishResponseDTO> publishBookWithCover(
            @RequestBody BookPublishRequestDTO request,
            HttpServletRequest httpRequest) throws Exception {

        Long librarianUserId = extractUserIdFromToken(httpRequest);

        // If cover image provided as base64, upload to S3 and set as coverUrl
        if (request.getCoverUrl() != null && request.getCoverUrl().startsWith("data:")) {
            String dataUri = request.getCoverUrl();
            String mimeType = dataUri.substring("data:".length(), dataUri.indexOf(";"));
            String base64Data = dataUri.substring(dataUri.indexOf(",") + 1);
            byte[] imageBytes = java.util.Base64.getDecoder().decode(base64Data);
            long isbn = request.getIsbn();
            String coverUrl = s3StorageService.uploadCoverBytes(imageBytes, isbn, mimeType);
            request.setCoverUrl(coverUrl);
            LOGGER.info("Cover image uploaded for ISBN {}: {}", isbn, coverUrl);
        }

        BookPublishResponseDTO response = bookPublishService.publishBook(request, librarianUserId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/prefill/{isbn}")
    public ResponseEntity<BookPublishRequestDTO> prefillFromOpenLibrary(@PathVariable @NonNull Long isbn,
            HttpServletRequest httpRequest) {
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

        String email = jwtService.extractEmail(token);

        // Buscar el usuario por email y retornar su ID
        return userRepository.findByEmail(email)
                .map(User::getId)
                .orElseThrow(() -> new UnauthorizedException("Usuario no encontrado"));
    }
}