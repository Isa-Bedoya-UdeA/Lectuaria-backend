package com.lectuaria.backend.controller.books;

import com.lectuaria.backend.dto.book.BookRatingRequestDTO;
import com.lectuaria.backend.dto.book.BookRatingResponseDTO;
import com.lectuaria.backend.dto.book.BookRatingWithUserDTO;
import com.lectuaria.backend.dto.book.BookReviewPreviewRequestDTO;
import com.lectuaria.backend.dto.book.BookReviewPreviewResponseDTO;
import com.lectuaria.backend.dto.book.BookReviewResponseDTO;
import com.lectuaria.backend.dto.book.BookReviewUpsertRequestDTO;
import com.lectuaria.backend.dto.common.PaginatedResponse;

import com.lectuaria.backend.exception.UnauthorizedException;
import com.lectuaria.backend.model.auth.User;
import com.lectuaria.backend.model.auth.UserRole;
import com.lectuaria.backend.repository.auth.UserRepository;
import com.lectuaria.backend.security.JwtService;
import com.lectuaria.backend.service.book.IBookRatingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.lang.NonNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@RestController
@RequestMapping("/api/books")
public class BookRatingController {

    private final IBookRatingService bookRatingService;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    public BookRatingController(IBookRatingService bookRatingService, UserRepository userRepository,
            JwtService jwtService) {
        this.bookRatingService = bookRatingService;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    @PutMapping("/{bookId}/rating")
    public ResponseEntity<BookRatingResponseDTO> rateBook(
            @PathVariable @NonNull Long bookId,
            @Valid @RequestBody @NonNull BookRatingRequestDTO request,
            HttpServletRequest httpRequest) {
        User user = extractUserFromRequest(httpRequest);
        if (user.getRole() != UserRole.READER) {
            throw new UnauthorizedException("Solo los usuarios lectores pueden calificar libros");
        }

        return ResponseEntity.ok(bookRatingService.rateBook(bookId, user, request.getRating()));
    }

    @GetMapping("/{bookId}/rating")
    public ResponseEntity<BookRatingResponseDTO> getUserRating(
            @PathVariable @NonNull Long bookId,
            HttpServletRequest httpRequest) {
        User user = extractUserFromRequest(httpRequest);
        return ResponseEntity.ok(bookRatingService.getBookRating(bookId, user));
    }

    @GetMapping("/{bookId}/ratings")
    public ResponseEntity<List<BookRatingWithUserDTO>> getAllBookRatings(
            @PathVariable @NonNull Long bookId) {
        return ResponseEntity.ok(bookRatingService.getAllBookRatings(bookId));
    }

    @PutMapping("/ratings/{ratingId}")
    public ResponseEntity<BookRatingResponseDTO> updateRating(
            @PathVariable @NonNull Long ratingId,
            @RequestBody @NonNull BookRatingRequestDTO ratingRequest,
            HttpServletRequest httpRequest) {
        User user = extractUserFromRequest(httpRequest);
        if (user.getRole() != UserRole.READER) {
            throw new UnauthorizedException("Solo los usuarios lectores pueden actualizar calificaciones");
        }

        BookRatingResponseDTO response = bookRatingService.updateRating(ratingId, ratingRequest.getRating(), user);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/ratings/{ratingId}")
    public ResponseEntity<Void> deleteRating(
            @PathVariable @NonNull Long ratingId,
            HttpServletRequest httpRequest) {
        User user = extractUserFromRequest(httpRequest);

        if (user.getRole() != UserRole.READER) {
            throw new UnauthorizedException("Solo los usuarios lectores pueden eliminar calificaciones");
        }

        bookRatingService.deleteRating(ratingId, user);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{bookId}/reviews")
    public ResponseEntity<BookReviewResponseDTO> saveReview(
            @PathVariable @NonNull Long bookId,
            @Valid @RequestBody @NonNull BookReviewUpsertRequestDTO request,
            HttpServletRequest httpRequest) {
        User user = extractUserFromRequest(httpRequest);

        if (user.getRole() != UserRole.READER) {
            throw new UnauthorizedException("Solo los usuarios lectores pueden escribir reseñas");
        }

        return ResponseEntity.ok(bookRatingService.saveReview(bookId, user, request));
    }

    @PostMapping("/{bookId}/reviews/preview")
    public ResponseEntity<BookReviewPreviewResponseDTO> previewReview(
            @PathVariable @NonNull Long bookId,
            @Valid @RequestBody @NonNull BookReviewPreviewRequestDTO request,
            HttpServletRequest httpRequest) {
        extractUserFromRequest(httpRequest);
        return ResponseEntity.ok(bookRatingService.previewReview(bookId, request));
    }

    @GetMapping("/{bookId}/reviews")
    public ResponseEntity<PaginatedResponse<BookReviewResponseDTO>> getPublishedReviews(
            @PathVariable @NonNull Long bookId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        return ResponseEntity.ok(bookRatingService.getPublishedReviews(bookId, page, size));
    }

    @PutMapping("/reviews/{reviewId}")
    public ResponseEntity<BookReviewResponseDTO> updateReview(
            @PathVariable @NonNull Long reviewId,
            @Valid @RequestBody @NonNull BookReviewUpsertRequestDTO request,
            HttpServletRequest httpRequest) {
        User user = extractUserFromRequest(httpRequest);

        if (user.getRole() != UserRole.READER) {
            throw new UnauthorizedException("Solo los usuarios lectores pueden editar reseñas");
        }

        return ResponseEntity.ok(bookRatingService.updateReview(reviewId, user, request));
    }

    @DeleteMapping("/reviews/{reviewId}")
    public ResponseEntity<Void> deleteReview(
            @PathVariable @NonNull Long reviewId,
            HttpServletRequest httpRequest) {
        User user = extractUserFromRequest(httpRequest);

        if (user.getRole() != UserRole.READER && user.getRole() != UserRole.ADMIN) {
            throw new UnauthorizedException("Solo los usuarios lectores pueden eliminar reseñas");
        }

        bookRatingService.deleteReview(reviewId, user);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{bookId}/interaction")
    public ResponseEntity<Void> deleteUserInteraction(
            @PathVariable @NonNull Long bookId,
            HttpServletRequest httpRequest) {
        User user = extractUserFromRequest(httpRequest);

        if (user.getRole() != UserRole.READER && user.getRole() != UserRole.ADMIN) {
            throw new UnauthorizedException("Solo los usuarios lectores pueden eliminar calificaciones");
        }

        bookRatingService.deleteRatingByBookAndUser(bookId, user);
        return ResponseEntity.noContent().build();
    }

    private static final Logger logger = LoggerFactory.getLogger(BookRatingController.class);

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
                .orElseThrow(() -> {
                    logger.warn("User with email {} not found in database", email);
                    return new UnauthorizedException("Usuario no encontrado");
                });
    }
}
