package com.lectuaria.backend.service.book.impl;

import com.lectuaria.backend.service.book.IBookRatingService;

import com.lectuaria.backend.dto.book.BookRatingResponseDTO;
import com.lectuaria.backend.dto.book.BookRatingWithUserDTO;
import com.lectuaria.backend.dto.book.BookReviewPreviewRequestDTO;
import com.lectuaria.backend.dto.book.BookReviewPreviewResponseDTO;
import com.lectuaria.backend.dto.book.BookReviewResponseDTO;
import com.lectuaria.backend.dto.book.BookReviewUpsertRequestDTO;
import com.lectuaria.backend.dto.common.PaginatedResponse;
import com.lectuaria.backend.exception.UnauthorizedException;
import com.lectuaria.backend.exception.ValidationException;

import com.lectuaria.backend.model.auth.User;
import com.lectuaria.backend.model.auth.UserRole;
import com.lectuaria.backend.model.book.Book;
import com.lectuaria.backend.model.book.BookRating;
import com.lectuaria.backend.model.book.BookReview;
import com.lectuaria.backend.model.book.BookRatingStats;
import com.lectuaria.backend.model.book.ReviewStatus;
import com.lectuaria.backend.model.book.ReviewSortOption;
import com.lectuaria.backend.model.friendship.Friendship;
import com.lectuaria.backend.model.notification.NotificationType;

import com.lectuaria.backend.repository.book.BookRatingRepository;
import com.lectuaria.backend.repository.book.BookReviewRepository;
import com.lectuaria.backend.repository.book.BookRatingStatsRepository;
import com.lectuaria.backend.repository.book.BookRepository;
import com.lectuaria.backend.repository.friendship.FriendshipRepository;
import com.lectuaria.backend.service.notification.INotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BookRatingServiceImpl implements IBookRatingService {

    private static final int MAX_REVIEW_LENGTH = 2000;

    private final BookRatingRepository bookRatingRepository;
    private final BookReviewRepository bookReviewRepository;
    private final BookRepository bookRepository;
    private final BookRatingStatsRepository bookRatingStatsRepository;
    private final FriendshipRepository friendshipRepository;
    private final INotificationService notificationService;

    @PersistenceContext
    private EntityManager entityManager;

    public BookRatingServiceImpl(BookRatingRepository bookRatingRepository,
            BookReviewRepository bookReviewRepository,
            BookRepository bookRepository,
            BookRatingStatsRepository bookRatingStatsRepository,
            FriendshipRepository friendshipRepository,
            INotificationService notificationService) {

        this.bookRatingRepository = bookRatingRepository;
        this.bookReviewRepository = bookReviewRepository;
        this.bookRepository = bookRepository;
        this.bookRatingStatsRepository = bookRatingStatsRepository;
        this.friendshipRepository = friendshipRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public BookRatingResponseDTO rateBook(@NonNull Long bookId, @NonNull User user, @NonNull BigDecimal rating) {
        Book book = getBookOrThrow(bookId);

        BookRating bookRating = bookRatingRepository.findByBookIdAndUserId(bookId, user.getId())
                .orElseGet(() -> {
                    BookRating newRating = new BookRating();
                    newRating.setBook(book);
                    newRating.setUser(user);
                    return newRating;
                });

        bookRating.setRating(normalizeRating(rating));
        bookRatingRepository.save(bookRating);

        refreshBookAggregates(book);

        return buildResponse("Calificación guardada correctamente.", book, user);
    }

    @Transactional(readOnly = true)
    public BookRatingResponseDTO getBookRating(@NonNull Long bookId, @NonNull User user) {
        Book book = getBookOrThrow(bookId);

        bookRatingRepository.findByBookIdAndUserId(bookId, user.getId())
                .map(BookRating::getRating)
                .orElse(null);

        return buildResponse("Calificación actual obtenida correctamente.", book, user);
    }

    @Transactional(readOnly = true)
    public List<BookRatingWithUserDTO> getAllBookRatings(@NonNull Long bookId) {
        Book book = getBookOrThrow(bookId);

        List<BookRating> ratings = bookRatingRepository.findByBookId(bookId);

        return ratings.stream()
                .map(rating -> new BookRatingWithUserDTO(
                        rating.getId().longValue(),
                        book.getId(),
                        rating.getRating(),
                        rating.getUser().getId(),
                        rating.getUser().getFullName(),
                        rating.getUser().getEmail(),
                        rating.getCreatedAt()))
                .collect(Collectors.toList());
    }

    @Transactional
    public BookRatingResponseDTO updateRating(@NonNull Long ratingId, @NonNull BigDecimal newRating,
            @NonNull User user) {
        BookRating rating = getRatingOrThrow(ratingId);
        assertOwnership(rating, user, "No puedes actualizar una calificación que no te pertenece");

        rating.setRating(normalizeRating(newRating));
        bookRatingRepository.save(rating);

        Book book = rating.getBook();
        refreshBookAggregates(book);

        return buildResponse("Calificación actualizada correctamente", book, user);
    }

    @Transactional
    public void deleteRating(@NonNull Long ratingId, @NonNull User user) {
        BookRating rating = getRatingOrThrow(ratingId);
        assertOwnership(rating, user, "No puedes eliminar una calificación que no te pertenece");
        deleteRatingInternal(rating);
    }

    @Transactional
    public void deleteRatingByBookAndUser(@NonNull Long bookId, @NonNull User user) {
        bookRatingRepository.findByBookIdAndUserId(bookId, user.getId())
                .ifPresent(this::deleteRatingInternal);
    }

    private void deleteRatingInternal(BookRating rating) {
        try {
            entityManager.createNativeQuery("DELETE FROM rating_history WHERE id_rating = :ratingId")
                    .setParameter("ratingId", rating.getId())
                    .executeUpdate();
        } catch (Exception ignored) {
            // Tabla opcional en algunos entornos.
        }

        Book book = rating.getBook();
        bookRatingRepository.delete(rating);
        refreshBookAggregates(book);

        if (bookRatingRepository.countByBookId(book.getId()) == 0) {
            bookRatingStatsRepository.deleteByBookId(book.getId());
        }
    }

    @Transactional
    public BookReviewResponseDTO saveReview(@NonNull Long bookId, @NonNull User user,
            @NonNull BookReviewUpsertRequestDTO request) {
        Book book = getBookOrThrow(bookId);
        validateReviewText(request.getReviewText());

        // 1. Manejar la Reseña (Tabla REVIEW)
        BookReview review = bookReviewRepository.findByBookIdAndUserId(bookId, user.getId())
                .orElseGet(() -> {
                    BookReview nr = new BookReview();
                    nr.setBook(book);
                    nr.setUser(user);
                    return nr;
                });

        boolean wasPreviouslyPublished = ReviewStatus.published.equals(review.getStatus());

        review.setRating(normalizeRating(request.getRating()));
        review.setReviewText(request.getReviewText().trim());
        review.setStatus(Boolean.TRUE.equals(request.getPublish()) ? ReviewStatus.published : ReviewStatus.draft);

        if (ReviewStatus.published.equals(review.getStatus()) && review.getPublishedAt() == null) {
            review.setPublishedAt(Instant.now());
        }

        BookReview saved = bookReviewRepository.save(review);

        // Send notification if review is being published for the first time
        if (!wasPreviouslyPublished && ReviewStatus.published.equals(saved.getStatus())) {
            sendReviewNotificationToFriends(user, book, saved.getId());
        }

        // 2. Sincronizar con la tabla RATING para estadísticas
        if (request.getRating() != null) {
            BookRating rating = bookRatingRepository.findByBookIdAndUserId(bookId, user.getId())
                    .orElseGet(() -> {
                        BookRating nr = new BookRating();
                        nr.setBook(book);
                        nr.setUser(user);
                        return nr;
                    });
            rating.setRating(normalizeRating(request.getRating()));
            bookRatingRepository.save(rating);
        }

        refreshBookAggregates(book);

        return mapReview(saved);
    }

    @Transactional(readOnly = true)
    public BookReviewPreviewResponseDTO previewReview(@NonNull Long bookId,
            @NonNull BookReviewPreviewRequestDTO request) {
        getBookOrThrow(bookId);
        validateReviewText(request.getReviewText());

        String normalizedText = request.getReviewText().trim();
        int remaining = MAX_REVIEW_LENGTH - normalizedText.length();

        return new BookReviewPreviewResponseDTO(bookId, normalizeRating(request.getRating()), normalizedText,
                remaining);
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<BookReviewResponseDTO> getPublishedReviews(@NonNull Long bookId, int page, int size) {
        return getPublishedReviews(bookId, page, size, ReviewSortOption.MOST_RECENT.name());
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<BookReviewResponseDTO> getPublishedReviews(@NonNull Long bookId, int page, int size, String sort) {
        getBookOrThrow(bookId);
        ReviewSortOption sortOption = ReviewSortOption.from(sort);
        Pageable pageable = PageRequest.of(Math.max(page, 0), size <= 0 ? 5 : size, toRatingSort(sortOption));

        // Obtenemos todas las calificaciones (RATING) porque siempre son públicas
        Page<BookRating> ratingsPage = bookRatingRepository.findByBookId(bookId, pageable);

        List<BookReviewResponseDTO> content = ratingsPage.getContent().stream().map(rating -> {
            // Buscamos si hay una reseña escrita asociada a esta calificación
            BookReview review = bookReviewRepository.findByBookIdAndUserId(bookId, rating.getUser().getId())
                    .orElse(null);

            // Si la reseña existe y está publicada, mostramos el texto.
            // Si no existe, o es un borrador, mostramos solo las estrellas.
            String text = (review != null && ReviewStatus.published.equals(review.getStatus()))
                    ? review.getReviewText()
                    : null;

            return new BookReviewResponseDTO(
                    review != null ? review.getId() : 0L,
                    bookId,
                    rating.getUser().getId(),
                    rating.getUser().getFullName(),
                    rating.getCreatedAt(),
                    review != null ? review.getPublishedAt() : rating.getCreatedAt(),
                    rating.getRating(),
                    text,
                    review != null ? review.getStatus().name() : ReviewStatus.published.name(),
                    0, // helpfulCount opcional
                    false // ownReview se calcula en el front
            );
        }).toList();

        return new PaginatedResponse<>(
                content,
                ratingsPage.getNumber(),
                ratingsPage.getSize(),
                ratingsPage.getTotalElements(),
                ratingsPage.getTotalPages(),
                ratingsPage.isFirst(),
                ratingsPage.isLast(),
                ratingsPage.hasNext(),
                ratingsPage.hasPrevious());
    }

    private Sort toRatingSort(ReviewSortOption sortOption) {
        return switch (sortOption) {
            case OLDEST -> Sort.by(Sort.Direction.ASC, "createdAt");
            case HIGHEST_RATING -> Sort.by(Sort.Direction.DESC, "rating").and(Sort.by(Sort.Direction.DESC, "createdAt"));
            case LOWEST_RATING -> Sort.by(Sort.Direction.ASC, "rating").and(Sort.by(Sort.Direction.DESC, "createdAt"));
            case MOST_HELPFUL, MOST_RECENT -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
    }

    @Transactional
    public BookReviewResponseDTO updateReview(@NonNull Long reviewId, @NonNull User user,
            @NonNull BookReviewUpsertRequestDTO request) {
        BookReview review = bookReviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Reseña no encontrada con ID: " + reviewId));

        if (!review.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("No tienes permiso para editar esta reseña");
        }

        validateReviewText(request.getReviewText());

        review.setRating(normalizeRating(request.getRating()));
        review.setReviewText(request.getReviewText().trim());
        review.setStatus(Boolean.TRUE.equals(request.getPublish()) ? ReviewStatus.published : ReviewStatus.draft);
        review.setEditedAt(Instant.now());

        if (ReviewStatus.published.equals(review.getStatus()) && review.getPublishedAt() == null) {
            review.setPublishedAt(Instant.now());
        }

        BookReview saved = bookReviewRepository.save(review);

        // Sincronizar RATING
        if (request.getRating() != null) {
            BookRating rating = bookRatingRepository.findByBookIdAndUserId(review.getBook().getId(), user.getId())
                    .orElseGet(() -> {
                        BookRating nr = new BookRating();
                        nr.setBook(review.getBook());
                        nr.setUser(user);
                        return nr;
                    });
            rating.setRating(normalizeRating(request.getRating()));
            bookRatingRepository.save(rating);
        }

        refreshBookAggregates(review.getBook());

        return mapReview(saved);
    }

    @Transactional
    public void deleteReview(@NonNull Long reviewId, @NonNull User user) {
        BookReview review = bookReviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Reseña no encontrada con ID: " + reviewId));

        if (!review.getUser().getId().equals(user.getId()) && !UserRole.ADMIN.name().equals(user.getRole().name())) {
            throw new UnauthorizedException("No tienes permiso para eliminar esta reseña");
        }

        Book book = review.getBook();

        // Al eliminar la reseña por decisión de negocio, también eliminamos la
        // calificación de estrellas
        bookRatingRepository.findByBookIdAndUserId(book.getId(), review.getUser().getId())
                .ifPresent(this::deleteRatingInternal);

        bookReviewRepository.delete(review);
    }

    @Transactional
    public void refreshBookAggregates(Book book) {

        BigDecimal average = bookRatingRepository.calculateAverageRatingByBookId(book.getId())
                .setScale(2, RoundingMode.HALF_UP);
        long count = bookRatingRepository.countByBookId(book.getId());

        // Actualizar la tabla Book (para compatibilidad existente)
        book.setAverageRating(average);
        book.setRatingsCount((int) count);
        bookRepository.save(book);

        updateBookRatingStats(book.getId(), average, (int) count);
    }

    /**
     * Actualiza o crea las estadísticas en BOOK_RATING_STATS
     */

    private void updateBookRatingStats(Long bookId, BigDecimal avgRating, Integer totalRatings) {
        BookRatingStats stats = bookRatingStatsRepository.findByBookId(bookId)
                .orElseGet(() -> new BookRatingStats(bookId, BigDecimal.ZERO, 0));

        stats.updateStats(avgRating, totalRatings);
        bookRatingStatsRepository.save(stats);
    }

    /**
     * Obtiene las estadísticas de calificación desde la cache BOOK_RATING_STATS
     * Si no existe, las calcula y las guarda en cache
     */

    @Transactional(readOnly = true)
    public BookRatingStats getBookRatingStats(@NonNull Long bookId) {
        return bookRatingStatsRepository.findByBookId(bookId)
                .orElseGet(() -> {
                    BigDecimal average = bookRatingRepository.calculateAverageRatingByBookId(bookId)
                            .setScale(2, RoundingMode.HALF_UP);
                    long count = bookRatingRepository.countByBookId(bookId);

                    BookRatingStats stats = new BookRatingStats(bookId, average, (int) count);
                    return bookRatingStatsRepository.save(stats);
                });
    }

    /**
     * Sincroniza las estadísticas cuando se modifica directamente Book
     * Este método se puede llamar desde otros servicios que actualicen Book
     */

    @Transactional
    public void syncBookRatingStatsFromBook(Book book) {
        if (book.getAverageRating() != null && book.getRatingsCount() != null) {
            updateBookRatingStats(book.getId(), book.getAverageRating(), book.getRatingsCount());
        }
    }

    private BookRatingResponseDTO buildResponse(String message, Book book, User user) {
        BookRatingStats stats = getBookRatingStats(book.getId());

        BookRating userRatingObj = bookRatingRepository.findByBookIdAndUserId(book.getId(), user.getId()).orElse(null);
        BookReview userReviewObj = bookReviewRepository.findByBookIdAndUserId(book.getId(), user.getId()).orElse(null);

        return new BookRatingResponseDTO(
                message,
                book.getId(),
                userRatingObj != null ? userRatingObj.getRating() : null,
                stats.getAvgRating(),
                stats.getTotalRatings().longValue(),
                userReviewObj != null ? userReviewObj.getId() : null,
                userReviewObj != null ? userReviewObj.getReviewText() : null,
                (userReviewObj != null && userReviewObj.getStatus() != null)
                        ? userReviewObj.getStatus().name()
                        : null);
    }

    private BigDecimal normalizeRating(BigDecimal rating) {
        return rating.setScale(1, RoundingMode.HALF_UP);
    }

    private void validateReviewText(String reviewText) {
        if (reviewText == null || reviewText.trim().isEmpty()) {
            throw new ValidationException(List.of("La reseña debe tener contenido para publicarse"));
        }
        if (reviewText.trim().length() > MAX_REVIEW_LENGTH) {
            throw new ValidationException(List.of("La reseña no puede superar 2000 caracteres"));
        }
    }

    private Book getBookOrThrow(Long bookId) {
        return bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("Libro no encontrado con ID: " + bookId));
    }

    private BookRating getRatingOrThrow(Long ratingId) {
        return bookRatingRepository.findById(ratingId)
                .orElseThrow(() -> new RuntimeException("Calificación no encontrada con ID: " + ratingId));
    }

    private void assertOwnership(BookRating rating, User user, String message) {
        if (!rating.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException(message);
        }
    }

    private BookReviewResponseDTO mapReview(BookReview review) {
        return new BookReviewResponseDTO(
                review.getId(),
                review.getBook().getId(),
                review.getUser().getId(),
                review.getUser().getFullName(),
                review.getCreatedAt(),
                review.getPublishedAt(),
                review.getRating(),
                review.getReviewText(),
                review.getStatus().name(),
                0, // No hay helpfulCount en la tabla Review según DDL
                false);
    }

    private void sendReviewNotificationToFriends(User user, Book book, Long reviewId) {
        List<Friendship> friendships = friendshipRepository.findFriendsByUserId(user.getId());
        for (Friendship friendship : friendships) {
            User friend = friendship.getUser1().getId().equals(user.getId()) ? friendship.getUser2() : friendship.getUser1();
            notificationService.createNotification(
                    friend.getId(),
                    NotificationType.REVIEW,
                    user.getFullName() + " ha publicado una reseña en " + book.getTitle(),
                    book.getIsbn()
            );
        }
    }
}
