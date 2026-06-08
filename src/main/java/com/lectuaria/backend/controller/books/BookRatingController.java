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
import com.lectuaria.backend.security.AuthenticatedUserResolver;
import com.lectuaria.backend.service.book.IBookRatingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/api/books")
public class BookRatingController {

    private final IBookRatingService bookRatingService;
    private final AuthenticatedUserResolver userResolver;

    public BookRatingController(IBookRatingService bookRatingService,
                                 AuthenticatedUserResolver userResolver) {
        this.bookRatingService = bookRatingService;
        this.userResolver = userResolver;
    }

    @PutMapping("/{bookId}/rating")
    public ResponseEntity<EntityModel<BookRatingResponseDTO>> rateBook(
            @PathVariable @NonNull Long bookId,
            @Valid @RequestBody @NonNull BookRatingRequestDTO request,
            HttpServletRequest httpRequest) {
        User user = userResolver.requireCurrentUser(httpRequest);
        requireReader(user, "calificar libros");
        BookRatingResponseDTO response = bookRatingService.rateBook(bookId, user, request.getRating());
        return ResponseEntity.ok(EntityModel.of(response,
                linkTo(methodOn(BookRatingController.class).rateBook(bookId, request, httpRequest)).withSelfRel(),
                linkTo(methodOn(BookRatingController.class).getUserRating(bookId, httpRequest)).withRel("self-rating")));
    }

    @GetMapping("/{bookId}/rating")
    public ResponseEntity<EntityModel<BookRatingResponseDTO>> getUserRating(
            @PathVariable @NonNull Long bookId,
            HttpServletRequest httpRequest) {
        User user = userResolver.requireCurrentUser(httpRequest);
        return ResponseEntity.ok(EntityModel.of(bookRatingService.getBookRating(bookId, user),
                linkTo(methodOn(BookRatingController.class).getUserRating(bookId, httpRequest)).withSelfRel()));
    }

    @GetMapping("/{bookId}/ratings")
    public ResponseEntity<CollectionModel<BookRatingWithUserDTO>> getAllBookRatings(
            @PathVariable @NonNull Long bookId) {
        return ResponseEntity.ok(CollectionModel.of(bookRatingService.getAllBookRatings(bookId),
                linkTo(methodOn(BookRatingController.class).getAllBookRatings(bookId)).withSelfRel()));
    }

    @PutMapping("/ratings/{ratingId}")
    public ResponseEntity<EntityModel<BookRatingResponseDTO>> updateRating(
            @PathVariable @NonNull Long ratingId,
            @RequestBody @NonNull BookRatingRequestDTO ratingRequest,
            HttpServletRequest httpRequest) {
        User user = userResolver.requireCurrentUser(httpRequest);
        requireReader(user, "actualizar calificaciones");
        return ResponseEntity.ok(EntityModel.of(
                bookRatingService.updateRating(ratingId, ratingRequest.getRating(), user),
                linkTo(methodOn(BookRatingController.class).updateRating(ratingId, ratingRequest, httpRequest)).withSelfRel()));
    }

    @DeleteMapping("/ratings/{ratingId}")
    public ResponseEntity<Void> deleteRating(
            @PathVariable @NonNull Long ratingId,
            HttpServletRequest httpRequest) {
        User user = userResolver.requireCurrentUser(httpRequest);
        requireReader(user, "eliminar calificaciones");
        bookRatingService.deleteRating(ratingId, user);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{bookId}/reviews")
    public ResponseEntity<EntityModel<BookReviewResponseDTO>> saveReview(
            @PathVariable @NonNull Long bookId,
            @Valid @RequestBody @NonNull BookReviewUpsertRequestDTO request,
            HttpServletRequest httpRequest) {
        User user = userResolver.requireCurrentUser(httpRequest);
        requireReader(user, "escribir reseñas");
        return ResponseEntity.ok(EntityModel.of(bookRatingService.saveReview(bookId, user, request),
                linkTo(methodOn(BookRatingController.class).saveReview(bookId, request, httpRequest)).withSelfRel()));
    }

    @PostMapping("/{bookId}/reviews/preview")
    public ResponseEntity<EntityModel<BookReviewPreviewResponseDTO>> previewReview(
            @PathVariable @NonNull Long bookId,
            @Valid @RequestBody @NonNull BookReviewPreviewRequestDTO request,
            HttpServletRequest httpRequest) {
        userResolver.requireCurrentUser(httpRequest);
        return ResponseEntity.ok(EntityModel.of(bookRatingService.previewReview(bookId, request),
                linkTo(methodOn(BookRatingController.class).saveReview(bookId, null, httpRequest)).withRel("save")));
    }

    @GetMapping("/{bookId}/reviews")
    public ResponseEntity<CollectionModel<BookReviewResponseDTO>> getPublishedReviews(
            @PathVariable @NonNull Long bookId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(defaultValue = "MOST_RECENT") String sort) {
        PaginatedResponse<BookReviewResponseDTO> response =
                bookRatingService.getPublishedReviews(bookId, page, size, sort);
        return ResponseEntity.ok(CollectionModel.of(response.getContent(),
                linkTo(methodOn(BookRatingController.class).getPublishedReviews(bookId, page, size, sort)).withSelfRel()));
    }

    @PutMapping("/reviews/{reviewId}")
    public ResponseEntity<EntityModel<BookReviewResponseDTO>> updateReview(
            @PathVariable @NonNull Long reviewId,
            @Valid @RequestBody @NonNull BookReviewUpsertRequestDTO request,
            HttpServletRequest httpRequest) {
        User user = userResolver.requireCurrentUser(httpRequest);
        requireReader(user, "editar reseñas");
        return ResponseEntity.ok(EntityModel.of(bookRatingService.updateReview(reviewId, user, request),
                linkTo(methodOn(BookRatingController.class).updateReview(reviewId, request, httpRequest)).withSelfRel()));
    }

    @DeleteMapping("/reviews/{reviewId}")
    public ResponseEntity<Void> deleteReview(
            @PathVariable @NonNull Long reviewId,
            HttpServletRequest httpRequest) {
        User user = userResolver.requireCurrentUser(httpRequest);
        requireReaderOrAdmin(user, "eliminar reseñas");
        bookRatingService.deleteReview(reviewId, user);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{bookId}/interaction")
    public ResponseEntity<Void> deleteUserInteraction(
            @PathVariable @NonNull Long bookId,
            HttpServletRequest httpRequest) {
        User user = userResolver.requireCurrentUser(httpRequest);
        requireReaderOrAdmin(user, "eliminar calificaciones");
        bookRatingService.deleteRatingByBookAndUser(bookId, user);
        return ResponseEntity.noContent().build();
    }

    private void requireReader(User user, String action) {
        if (user.getRole() != UserRole.READER) {
            throw new UnauthorizedException("Solo los usuarios lectores pueden " + action);
        }
    }

    private void requireReaderOrAdmin(User user, String action) {
        if (user.getRole() != UserRole.READER && user.getRole() != UserRole.ADMIN) {
            throw new UnauthorizedException("Solo los usuarios lectores pueden " + action);
        }
    }
}
