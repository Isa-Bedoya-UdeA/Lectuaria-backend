package com.lectuaria.backend.service.book.impl;

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
import com.lectuaria.backend.model.book.BookRatingStats;
import com.lectuaria.backend.model.book.BookReview;
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
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookRatingServiceImplTest {

    @Mock private BookRatingRepository bookRatingRepository;
    @Mock private BookReviewRepository bookReviewRepository;
    @Mock private BookRepository bookRepository;
    @Mock private BookRatingStatsRepository bookRatingStatsRepository;
    @Mock private FriendshipRepository friendshipRepository;
    @Mock private INotificationService notificationService;
    @Mock private EntityManager entityManager;

    private BookRatingServiceImpl service;

    private Book book;
    private User user;

    private static final Long BOOK_ID = 1L;
    private static final Long USER_ID = 10L;
    private static final Long RATING_ID = 100L;
    private static final Long REVIEW_ID = 200L;

    @BeforeEach
    void setUp() throws Exception {
        service = new BookRatingServiceImpl(
                bookRatingRepository,
                bookReviewRepository,
                bookRepository,
                bookRatingStatsRepository,
                friendshipRepository,
                notificationService
        );
        setField(service, "entityManager", entityManager);

        user = new User();
        setField(user, "id", USER_ID);
        setField(user, "fullName", "Juan Pérez");
        setField(user, "email", "juan@example.com");
        setField(user, "role", UserRole.READER);

        book = new Book();
        setField(book, "id", BOOK_ID);
        book.setIsbn(9780060935467L);
        book.setTitle("Cien Años de Soledad");
        book.setAverageRating(BigDecimal.valueOf(4.5));
        book.setRatingsCount(10);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // rateBook
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("rateBook")
    class RateBookTests {

        @Test
        @DisplayName("creates new rating when none exists")
        void createsNewRating() throws Exception {
            BookRatingStats stats = new BookRatingStats(BOOK_ID, BigDecimal.valueOf(4.0), 1);

            when(bookRepository.findById(BOOK_ID)).thenReturn(Optional.of(book));
            when(bookRatingRepository.findByBookIdAndUserId(BOOK_ID, USER_ID)).thenReturn(Optional.empty());
            when(bookRatingRepository.save(any(BookRating.class))).thenAnswer(inv -> {
                BookRating r = inv.getArgument(0);
                setField(r, "id", RATING_ID);
                return r;
            });
            when(bookRatingStatsRepository.findByBookId(BOOK_ID)).thenReturn(Optional.of(stats));
            when(bookRatingRepository.calculateAverageRatingByBookId(BOOK_ID)).thenReturn(BigDecimal.valueOf(4.0));

            BookRatingResponseDTO result = service.rateBook(BOOK_ID, user, BigDecimal.valueOf(4.0));

            assertNotNull(result);
            assertEquals("Calificación guardada correctamente.", result.getMessage());
            assertEquals(BOOK_ID, result.getBookId());
            verify(bookRatingRepository).save(any(BookRating.class));
        }

        @Test
        @DisplayName("updates existing rating")
        void updatesExistingRating() throws Exception {
            BookRating existing = new BookRating();
            setField(existing, "id", RATING_ID);
            existing.setBook(book);
            existing.setUser(user);
            existing.setRating(BigDecimal.valueOf(3.0));

            BookRatingStats stats = new BookRatingStats(BOOK_ID, BigDecimal.valueOf(4.0), 1);

            when(bookRepository.findById(BOOK_ID)).thenReturn(Optional.of(book));
            when(bookRatingRepository.findByBookIdAndUserId(BOOK_ID, USER_ID)).thenReturn(Optional.of(existing));
            when(bookRatingRepository.save(any(BookRating.class))).thenReturn(existing);
            when(bookRatingStatsRepository.findByBookId(BOOK_ID)).thenReturn(Optional.of(stats));
            when(bookRatingRepository.calculateAverageRatingByBookId(BOOK_ID)).thenReturn(BigDecimal.valueOf(4.0));

            BookRatingResponseDTO result = service.rateBook(BOOK_ID, user, BigDecimal.valueOf(5.0));

            assertNotNull(result);
            ArgumentCaptor<BookRating> captor = ArgumentCaptor.forClass(BookRating.class);
            verify(bookRatingRepository).save(captor.capture());
            assertEquals(BigDecimal.valueOf(5.0).setScale(1, RoundingMode.HALF_UP), captor.getValue().getRating());
        }

        @Test
        @DisplayName("throws when book not found")
        void throwsWhenBookNotFound() throws Exception {
            when(bookRepository.findById(BOOK_ID)).thenReturn(Optional.empty());

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> service.rateBook(BOOK_ID, user, BigDecimal.valueOf(4.0)));
            assertTrue(ex.getMessage().contains("Libro no encontrado"));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getBookRating
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getBookRating")
    class GetBookRatingTests {

        @Test
        @DisplayName("returns rating response")
        void returnsRatingResponse() throws Exception {
            BookRating rating = new BookRating();
            setField(rating, "id", RATING_ID);
            rating.setBook(book);
            rating.setUser(user);
            rating.setRating(BigDecimal.valueOf(4.0));

            BookReview review = new BookReview();
            setField(review, "id", REVIEW_ID);
            review.setBook(book);
            review.setUser(user);
            review.setRating(BigDecimal.valueOf(4.0));
            review.setReviewText("Gran libro");
            review.setStatus(ReviewStatus.published);

            BookRatingStats stats = new BookRatingStats(BOOK_ID, BigDecimal.valueOf(4.5), 5);

            when(bookRepository.findById(BOOK_ID)).thenReturn(Optional.of(book));
            when(bookRatingRepository.findByBookIdAndUserId(BOOK_ID, USER_ID)).thenReturn(Optional.of(rating));
            when(bookReviewRepository.findByBookIdAndUserId(BOOK_ID, USER_ID)).thenReturn(Optional.of(review));
            when(bookRatingStatsRepository.findByBookId(BOOK_ID)).thenReturn(Optional.of(stats));

            BookRatingResponseDTO result = service.getBookRating(BOOK_ID, user);

            assertNotNull(result);
            assertEquals(BOOK_ID, result.getBookId());
        }

        @Test
        @DisplayName("throws when book not found")
        void throwsWhenBookNotFound() throws Exception {
            when(bookRepository.findById(BOOK_ID)).thenReturn(Optional.empty());

            assertThrows(RuntimeException.class, () -> service.getBookRating(BOOK_ID, user));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getAllBookRatings
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getAllBookRatings")
    class GetAllBookRatingsTests {

        @Test
        @DisplayName("returns list of ratings with user info")
        void returnsRatingsList() throws Exception {
            BookRating rating1 = new BookRating();
            setField(rating1, "id", 1L);
            rating1.setBook(book);
            rating1.setUser(user);
            rating1.setRating(BigDecimal.valueOf(4.0));

            BookRating rating2 = new BookRating();
            setField(rating2, "id", 2L);
            rating2.setBook(book);
            User user2 = new User();
            setField(user2, "id", 20L);
            setField(user2, "fullName", "Ana García");
            setField(user2, "email", "ana@example.com");
            rating2.setUser(user2);
            rating2.setRating(BigDecimal.valueOf(5.0));

            when(bookRepository.findById(BOOK_ID)).thenReturn(Optional.of(book));
            when(bookRatingRepository.findByBookId(BOOK_ID)).thenReturn(List.of(rating1, rating2));

            List<BookRatingWithUserDTO> result = service.getAllBookRatings(BOOK_ID);

            assertEquals(2, result.size());
            assertEquals("Juan Pérez", result.get(0).getUserName());
            assertEquals("Ana García", result.get(1).getUserName());
        }

        @Test
        @DisplayName("returns empty list when no ratings")
        void returnsEmptyList() throws Exception {
            when(bookRepository.findById(BOOK_ID)).thenReturn(Optional.of(book));
            when(bookRatingRepository.findByBookId(BOOK_ID)).thenReturn(List.of());

            List<BookRatingWithUserDTO> result = service.getAllBookRatings(BOOK_ID);

            assertTrue(result.isEmpty());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // updateRating
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateRating")
    class UpdateRatingTests {

        @Test
        @DisplayName("updates rating successfully")
        void updatesRatingSuccessfully() throws Exception {
            BookRating rating = new BookRating();
            setField(rating, "id", RATING_ID);
            rating.setBook(book);
            rating.setUser(user);
            rating.setRating(BigDecimal.valueOf(3.0));

            BookRatingStats stats = new BookRatingStats(BOOK_ID, BigDecimal.valueOf(4.0), 1);

            when(bookRatingRepository.findById(RATING_ID)).thenReturn(Optional.of(rating));
            when(bookRatingRepository.save(any(BookRating.class))).thenReturn(rating);
            when(bookRatingStatsRepository.findByBookId(BOOK_ID)).thenReturn(Optional.of(stats));
            when(bookRatingRepository.calculateAverageRatingByBookId(BOOK_ID)).thenReturn(BigDecimal.valueOf(4.0));

            BookRatingResponseDTO result = service.updateRating(RATING_ID, BigDecimal.valueOf(5.0), user);

            assertNotNull(result);
            assertEquals("Calificación actualizada correctamente", result.getMessage());
        }

        @Test
        @DisplayName("throws when rating not found")
        void throwsWhenRatingNotFound() throws Exception {
            when(bookRatingRepository.findById(RATING_ID)).thenReturn(Optional.empty());

            assertThrows(RuntimeException.class,
                    () -> service.updateRating(RATING_ID, BigDecimal.valueOf(5.0), user));
        }

        @Test
        @DisplayName("throws when user does not own rating")
        void throwsWhenNotOwner() throws Exception {
            BookRating rating = new BookRating();
            setField(rating, "id", RATING_ID);
            rating.setBook(book);
            User otherUser = new User();
            setField(otherUser, "id", 999L);
            rating.setUser(otherUser);
            rating.setRating(BigDecimal.valueOf(3.0));

            when(bookRatingRepository.findById(RATING_ID)).thenReturn(Optional.of(rating));

            assertThrows(UnauthorizedException.class,
                    () -> service.updateRating(RATING_ID, BigDecimal.valueOf(5.0), user));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // deleteRating
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteRating")
    class DeleteRatingTests {

        @Test
        @DisplayName("deletes rating successfully")
        void deletesRatingSuccessfully() throws Exception {
            BookRating rating = new BookRating();
            setField(rating, "id", RATING_ID);
            rating.setBook(book);
            rating.setUser(user);
            rating.setRating(BigDecimal.valueOf(4.0));

            when(bookRatingRepository.findById(RATING_ID)).thenReturn(Optional.of(rating));
            when(bookRatingRepository.countByBookId(BOOK_ID)).thenReturn(0L);
            doNothing().when(bookRatingRepository).delete(rating);
            doNothing().when(bookRatingStatsRepository).deleteByBookId(BOOK_ID);
            when(bookRepository.save(any(Book.class))).thenReturn(book);
            when(bookRatingRepository.calculateAverageRatingByBookId(BOOK_ID)).thenReturn(BigDecimal.ZERO);
            when(bookRatingStatsRepository.findByBookId(BOOK_ID)).thenReturn(Optional.empty());

            service.deleteRating(RATING_ID, user);

            verify(bookRatingRepository).delete(rating);
        }

        @Test
        @DisplayName("throws when not owner")
        void throwsWhenNotOwner() throws Exception {
            BookRating rating = new BookRating();
            setField(rating, "id", RATING_ID);
            rating.setBook(book);
            User otherUser = new User();
            setField(otherUser, "id", 999L);
            rating.setUser(otherUser);

            when(bookRatingRepository.findById(RATING_ID)).thenReturn(Optional.of(rating));

            assertThrows(UnauthorizedException.class,
                    () -> service.deleteRating(RATING_ID, user));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // deleteRatingByBookAndUser
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteRatingByBookAndUser")
    class DeleteRatingByBookAndUserTests {

        @Test
        @DisplayName("deletes rating when found")
        void deletesWhenFound() throws Exception {
            BookRating rating = new BookRating();
            setField(rating, "id", RATING_ID);
            rating.setBook(book);
            rating.setUser(user);
            rating.setRating(BigDecimal.valueOf(4.0));

            when(bookRatingRepository.findByBookIdAndUserId(BOOK_ID, USER_ID)).thenReturn(Optional.of(rating));
            when(bookRatingRepository.countByBookId(BOOK_ID)).thenReturn(0L);
            doNothing().when(bookRatingRepository).delete(rating);
            doNothing().when(bookRatingStatsRepository).deleteByBookId(BOOK_ID);
            when(bookRepository.save(any(Book.class))).thenReturn(book);
            when(bookRatingRepository.calculateAverageRatingByBookId(BOOK_ID)).thenReturn(BigDecimal.ZERO);
            when(bookRatingStatsRepository.findByBookId(BOOK_ID)).thenReturn(Optional.empty());

            service.deleteRatingByBookAndUser(BOOK_ID, user);

            verify(bookRatingRepository).delete(rating);
        }

        @Test
        @DisplayName("does nothing when rating not found")
        void doesNothingWhenNotFound() throws Exception {
            when(bookRatingRepository.findByBookIdAndUserId(BOOK_ID, USER_ID)).thenReturn(Optional.empty());

            service.deleteRatingByBookAndUser(BOOK_ID, user);

            verify(bookRatingRepository, never()).delete(any());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // saveReview
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("saveReview")
    class SaveReviewTests {

        @Test
        @DisplayName("creates new review with publish flag")
        void createsNewReviewWithPublish() throws Exception {
            BookReviewUpsertRequestDTO request = new BookReviewUpsertRequestDTO();
            request.setRating(BigDecimal.valueOf(4.5));
            request.setReviewText("Una obra maestra de la literatura.");
            request.setPublish(true);

            BookReview savedReview = new BookReview();
            setField(savedReview, "id", REVIEW_ID);
            savedReview.setBook(book);
            savedReview.setUser(user);
            savedReview.setRating(BigDecimal.valueOf(4.5).setScale(1, RoundingMode.HALF_UP));
            savedReview.setReviewText("Una obra maestra de la literatura.");
            savedReview.setStatus(ReviewStatus.published);
            savedReview.setPublishedAt(Instant.now());

            BookRatingStats stats = new BookRatingStats(BOOK_ID, BigDecimal.valueOf(4.5), 1);

            when(bookRepository.findById(BOOK_ID)).thenReturn(Optional.of(book));
            when(bookReviewRepository.findByBookIdAndUserId(BOOK_ID, USER_ID)).thenReturn(Optional.empty());
            when(bookReviewRepository.save(any(BookReview.class))).thenReturn(savedReview);
            when(bookRatingRepository.findByBookIdAndUserId(BOOK_ID, USER_ID)).thenReturn(Optional.empty());
            when(bookRatingRepository.save(any(BookRating.class))).thenAnswer(i -> i.getArgument(0));
            when(bookRatingStatsRepository.findByBookId(BOOK_ID)).thenReturn(Optional.of(stats));
            when(bookRepository.save(any(Book.class))).thenReturn(book);
            when(bookRatingRepository.calculateAverageRatingByBookId(BOOK_ID)).thenReturn(BigDecimal.valueOf(4.5));
            when(bookRatingRepository.countByBookId(BOOK_ID)).thenReturn(1L);
            when(friendshipRepository.findFriendsByUserId(USER_ID)).thenReturn(List.of());

            BookReviewResponseDTO result = service.saveReview(BOOK_ID, user, request);

            assertNotNull(result);
            assertEquals(REVIEW_ID, result.getReviewId());
            verify(bookReviewRepository).save(any(BookReview.class));
            verify(bookRatingRepository).save(any(BookRating.class));
        }

        @Test
        @DisplayName("creates new review as draft")
        void createsNewReviewAsDraft() throws Exception {
            BookReviewUpsertRequestDTO request = new BookReviewUpsertRequestDTO();
            request.setRating(BigDecimal.valueOf(3.0));
            request.setReviewText("Buena historia.");
            request.setPublish(false);

            BookReview savedReview = new BookReview();
            setField(savedReview, "id", REVIEW_ID);
            savedReview.setBook(book);
            savedReview.setUser(user);
            savedReview.setRating(BigDecimal.valueOf(3.0).setScale(1, RoundingMode.HALF_UP));
            savedReview.setReviewText("Buena historia.");
            savedReview.setStatus(ReviewStatus.draft);

            BookRatingStats stats = new BookRatingStats(BOOK_ID, BigDecimal.valueOf(3.0), 1);

            when(bookRepository.findById(BOOK_ID)).thenReturn(Optional.of(book));
            when(bookReviewRepository.findByBookIdAndUserId(BOOK_ID, USER_ID)).thenReturn(Optional.empty());
            when(bookReviewRepository.save(any(BookReview.class))).thenReturn(savedReview);
            when(bookRatingRepository.findByBookIdAndUserId(BOOK_ID, USER_ID)).thenReturn(Optional.empty());
            when(bookRatingRepository.save(any(BookRating.class))).thenAnswer(i -> i.getArgument(0));
            when(bookRatingStatsRepository.findByBookId(BOOK_ID)).thenReturn(Optional.of(stats));
            when(bookRepository.save(any(Book.class))).thenReturn(book);
            when(bookRatingRepository.calculateAverageRatingByBookId(BOOK_ID)).thenReturn(BigDecimal.valueOf(3.0));
            when(bookRatingRepository.countByBookId(BOOK_ID)).thenReturn(1L);

            BookReviewResponseDTO result = service.saveReview(BOOK_ID, user, request);

            assertNotNull(result);
        }

        @Test
        @DisplayName("sends notification to friends on first publish")
        void sendsNotificationOnFirstPublish() throws Exception {
            User friend = new User();
            setField(friend, "id", 30L);
            setField(friend, "fullName", "María");

            Friendship friendship = new Friendship();
            setField(friendship, "user1", user);
            setField(friendship, "user2", friend);

            BookReviewUpsertRequestDTO request = new BookReviewUpsertRequestDTO();
            request.setRating(BigDecimal.valueOf(5.0));
            request.setReviewText("Increíble.");
            request.setPublish(true);

            BookReview savedReview = new BookReview();
            setField(savedReview, "id", REVIEW_ID);
            savedReview.setBook(book);
            savedReview.setUser(user);
            savedReview.setRating(BigDecimal.valueOf(5.0).setScale(1, RoundingMode.HALF_UP));
            savedReview.setReviewText("Increíble.");
            savedReview.setStatus(ReviewStatus.published);
            savedReview.setPublishedAt(Instant.now());

            BookRatingStats stats = new BookRatingStats(BOOK_ID, BigDecimal.valueOf(5.0), 1);

            when(bookRepository.findById(BOOK_ID)).thenReturn(Optional.of(book));
            when(bookReviewRepository.findByBookIdAndUserId(BOOK_ID, USER_ID)).thenReturn(Optional.empty());
            when(bookReviewRepository.save(any(BookReview.class))).thenReturn(savedReview);
            when(bookRatingRepository.findByBookIdAndUserId(BOOK_ID, USER_ID)).thenReturn(Optional.empty());
            when(bookRatingRepository.save(any(BookRating.class))).thenAnswer(i -> i.getArgument(0));
            when(bookRatingStatsRepository.findByBookId(BOOK_ID)).thenReturn(Optional.of(stats));
            when(bookRepository.save(any(Book.class))).thenReturn(book);
            when(bookRatingRepository.calculateAverageRatingByBookId(BOOK_ID)).thenReturn(BigDecimal.valueOf(5.0));
            when(bookRatingRepository.countByBookId(BOOK_ID)).thenReturn(1L);
            when(friendshipRepository.findFriendsByUserId(USER_ID)).thenReturn(List.of(friendship));

            service.saveReview(BOOK_ID, user, request);

            verify(notificationService).createNotification(
                    eq(30L),
                    eq(NotificationType.REVIEW),
                    contains("ha publicado una reseña"),
                    eq(9780060935467L)
            );
        }

        @Test
        @DisplayName("throws when review text is empty")
        void throwsWhenReviewTextEmpty() throws Exception {
            BookReviewUpsertRequestDTO request = new BookReviewUpsertRequestDTO();
            request.setRating(BigDecimal.valueOf(4.0));
            request.setReviewText("");
            request.setPublish(true);

            when(bookRepository.findById(BOOK_ID)).thenReturn(Optional.of(book));

            assertThrows(ValidationException.class,
                    () -> service.saveReview(BOOK_ID, user, request));
        }

        @Test
        @DisplayName("throws when review text exceeds 2000 chars")
        void throwsWhenReviewTextTooLong() throws Exception {
            BookReviewUpsertRequestDTO request = new BookReviewUpsertRequestDTO();
            request.setRating(BigDecimal.valueOf(4.0));
            request.setReviewText("x".repeat(2001));
            request.setPublish(true);

            when(bookRepository.findById(BOOK_ID)).thenReturn(Optional.of(book));

            assertThrows(ValidationException.class,
                    () -> service.saveReview(BOOK_ID, user, request));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // previewReview
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("previewReview")
    class PreviewReviewTests {

        @Test
        @DisplayName("returns preview with remaining characters")
        void returnsPreview() throws Exception {
            BookReviewPreviewRequestDTO request = new BookReviewPreviewRequestDTO();
            request.setRating(BigDecimal.valueOf(4.5));
            request.setReviewText("Gran lectura");

            when(bookRepository.findById(BOOK_ID)).thenReturn(Optional.of(book));

            BookReviewPreviewResponseDTO result = service.previewReview(BOOK_ID, request);

            assertNotNull(result);
            assertEquals(BOOK_ID, result.getBookId());
            assertEquals("Gran lectura", result.getReviewText());
            assertEquals(2000 - "Gran lectura".length(), result.getRemainingCharacters());
        }

        @Test
        @DisplayName("throws when review text is null")
        void throwsWhenReviewTextNull() throws Exception {
            BookReviewPreviewRequestDTO request = new BookReviewPreviewRequestDTO();
            request.setRating(BigDecimal.valueOf(4.0));
            request.setReviewText(null);

            when(bookRepository.findById(BOOK_ID)).thenReturn(Optional.of(book));

            assertThrows(ValidationException.class,
                    () -> service.previewReview(BOOK_ID, request));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getPublishedReviews
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getPublishedReviews")
    class GetPublishedReviewsTests {

        @Test
        @DisplayName("returns paginated reviews")
        void returnsPaginatedReviews() throws Exception {
            BookRating rating1 = new BookRating();
            setField(rating1, "id", 1L);
            rating1.setBook(book);
            rating1.setUser(user);
            rating1.setRating(BigDecimal.valueOf(5.0));

            BookReview publishedReview = new BookReview();
            setField(publishedReview, "id", REVIEW_ID);
            publishedReview.setBook(book);
            publishedReview.setUser(user);
            publishedReview.setReviewText("Excelente libro");
            publishedReview.setStatus(ReviewStatus.published);
            publishedReview.setPublishedAt(Instant.now());

            Page<BookRating> page = new PageImpl<>(List.of(rating1), PageRequest.of(0, 5), 1);

            when(bookRepository.findById(BOOK_ID)).thenReturn(Optional.of(book));
            when(bookRatingRepository.findByBookId(eq(BOOK_ID), any(Pageable.class))).thenReturn(page);
            when(bookReviewRepository.findByBookIdAndUserId(BOOK_ID, USER_ID)).thenReturn(Optional.of(publishedReview));

            PaginatedResponse<BookReviewResponseDTO> result = service.getPublishedReviews(BOOK_ID, 0, 5);

            assertNotNull(result);
            assertEquals(1, result.getContent().size());
            assertEquals("Excelente libro", result.getContent().get(0).getReviewText());
        }

        @Test
        @DisplayName("returns reviews with sort option")
        void returnsReviewsWithSort() throws Exception {
            BookRating rating1 = new BookRating();
            setField(rating1, "id", 1L);
            rating1.setBook(book);
            rating1.setUser(user);
            rating1.setRating(BigDecimal.valueOf(4.0));

            Page<BookRating> page = new PageImpl<>(List.of(rating1), PageRequest.of(0, 5), 1);

            when(bookRepository.findById(BOOK_ID)).thenReturn(Optional.of(book));
            when(bookRatingRepository.findByBookId(eq(BOOK_ID), any(Pageable.class))).thenReturn(page);
            when(bookReviewRepository.findByBookIdAndUserId(BOOK_ID, USER_ID)).thenReturn(Optional.empty());

            PaginatedResponse<BookReviewResponseDTO> result = service.getPublishedReviews(BOOK_ID, 0, 5, ReviewSortOption.HIGHEST_RATING.name());

            assertNotNull(result);
            assertEquals(1, result.getContent().size());
            assertNull(result.getContent().get(0).getReviewText());
        }

        @Test
        @DisplayName("returns empty paginated response when no ratings")
        void returnsEmptyPaginatedResponse() throws Exception {
            Page<BookRating> page = new PageImpl<>(List.of(), PageRequest.of(0, 5), 0);

            when(bookRepository.findById(BOOK_ID)).thenReturn(Optional.of(book));
            when(bookRatingRepository.findByBookId(eq(BOOK_ID), any(Pageable.class))).thenReturn(page);

            PaginatedResponse<BookReviewResponseDTO> result = service.getPublishedReviews(BOOK_ID, 0, 5);

            assertNotNull(result);
            assertTrue(result.getContent().isEmpty());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // updateReview
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateReview")
    class UpdateReviewTests {

        @Test
        @DisplayName("updates review successfully")
        void updatesReviewSuccessfully() throws Exception {
            BookReviewUpsertRequestDTO request = new BookReviewUpsertRequestDTO();
            request.setRating(BigDecimal.valueOf(5.0));
            request.setReviewText("Actualizado: mejoró con el tiempo.");
            request.setPublish(true);

            BookReview existingReview = new BookReview();
            setField(existingReview, "id", REVIEW_ID);
            existingReview.setBook(book);
            existingReview.setUser(user);
            existingReview.setRating(BigDecimal.valueOf(3.0));
            existingReview.setReviewText("Original review");
            existingReview.setStatus(ReviewStatus.draft);

            BookReview savedReview = new BookReview();
            setField(savedReview, "id", REVIEW_ID);
            savedReview.setBook(book);
            savedReview.setUser(user);
            savedReview.setRating(BigDecimal.valueOf(5.0).setScale(1, RoundingMode.HALF_UP));
            savedReview.setReviewText("Actualizado: mejoró con el tiempo.");
            savedReview.setStatus(ReviewStatus.published);
            savedReview.setPublishedAt(Instant.now());
            savedReview.setEditedAt(Instant.now());

            BookRatingStats stats = new BookRatingStats(BOOK_ID, BigDecimal.valueOf(5.0), 1);

            when(bookReviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(existingReview));
            when(bookReviewRepository.save(any(BookReview.class))).thenReturn(savedReview);
            when(bookRatingRepository.findByBookIdAndUserId(BOOK_ID, USER_ID)).thenReturn(Optional.empty());
            when(bookRatingRepository.save(any(BookRating.class))).thenAnswer(i -> i.getArgument(0));
            when(bookRatingStatsRepository.findByBookId(BOOK_ID)).thenReturn(Optional.of(stats));
            when(bookRepository.save(any(Book.class))).thenReturn(book);
            when(bookRatingRepository.calculateAverageRatingByBookId(BOOK_ID)).thenReturn(BigDecimal.valueOf(5.0));
            when(bookRatingRepository.countByBookId(BOOK_ID)).thenReturn(1L);

            BookReviewResponseDTO result = service.updateReview(REVIEW_ID, user, request);

            assertNotNull(result);
            verify(bookReviewRepository).save(any(BookReview.class));
        }

        @Test
        @DisplayName("throws when review not found")
        void throwsWhenReviewNotFound() throws Exception {
            BookReviewUpsertRequestDTO request = new BookReviewUpsertRequestDTO();
            request.setRating(BigDecimal.valueOf(5.0));
            request.setReviewText("Updated");
            request.setPublish(true);

            when(bookReviewRepository.findById(REVIEW_ID)).thenReturn(Optional.empty());

            assertThrows(RuntimeException.class,
                    () -> service.updateReview(REVIEW_ID, user, request));
        }

        @Test
        @DisplayName("throws when user does not own review")
        void throwsWhenNotOwner() throws Exception {
            BookReviewUpsertRequestDTO request = new BookReviewUpsertRequestDTO();
            request.setRating(BigDecimal.valueOf(5.0));
            request.setReviewText("Updated");
            request.setPublish(true);

            User otherUser = new User();
            setField(otherUser, "id", 999L);

            BookReview existingReview = new BookReview();
            setField(existingReview, "id", REVIEW_ID);
            existingReview.setBook(book);
            existingReview.setUser(otherUser);

            when(bookReviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(existingReview));

            assertThrows(UnauthorizedException.class,
                    () -> service.updateReview(REVIEW_ID, user, request));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // deleteReview
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteReview")
    class DeleteReviewTests {

        @Test
        @DisplayName("deletes own review successfully")
        void deletesOwnReviewSuccessfully() throws Exception {
            BookReview review = new BookReview();
            setField(review, "id", REVIEW_ID);
            review.setBook(book);
            review.setUser(user);

            when(bookReviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));
            when(bookRatingRepository.findByBookIdAndUserId(BOOK_ID, USER_ID)).thenReturn(Optional.empty());
            doNothing().when(bookReviewRepository).delete(review);

            service.deleteReview(REVIEW_ID, user);

            verify(bookReviewRepository).delete(review);
            verify(bookRatingRepository, never()).delete(any(BookRating.class));
        }

        @Test
        @DisplayName("admin can delete any review")
        void adminCanDeleteReview() throws Exception {
            User admin = new User();
            setField(admin, "id", 1L);
            setField(admin, "role", UserRole.ADMIN);

            BookReview review = new BookReview();
            setField(review, "id", REVIEW_ID);
            review.setBook(book);
            review.setUser(user);

            BookRating rating = new BookRating();
            setField(rating, "id", RATING_ID);
            rating.setBook(book);
            rating.setUser(user);

            when(bookReviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));
            when(bookRatingRepository.findByBookIdAndUserId(BOOK_ID, USER_ID)).thenReturn(Optional.of(rating));
            when(bookRatingRepository.countByBookId(BOOK_ID)).thenReturn(0L);
            doNothing().when(bookRatingRepository).delete(rating);
            doNothing().when(bookRatingStatsRepository).deleteByBookId(BOOK_ID);
            doNothing().when(bookReviewRepository).delete(review);
            when(bookRepository.save(any(Book.class))).thenReturn(book);
            when(bookRatingRepository.calculateAverageRatingByBookId(BOOK_ID)).thenReturn(BigDecimal.ZERO);
            when(bookRatingStatsRepository.findByBookId(BOOK_ID)).thenReturn(Optional.empty());

            service.deleteReview(REVIEW_ID, admin);

            verify(bookReviewRepository).delete(review);
            verify(bookRatingRepository).delete(rating);
        }

        @Test
        @DisplayName("throws when neither owner nor admin")
        void throwsWhenNeitherOwnerNorAdmin() throws Exception {
            User otherUser = new User();
            setField(otherUser, "id", 999L);
            setField(otherUser, "role", UserRole.READER);

            BookReview review = new BookReview();
            setField(review, "id", REVIEW_ID);
            review.setBook(book);
            review.setUser(user);

            when(bookReviewRepository.findById(REVIEW_ID)).thenReturn(Optional.of(review));

            assertThrows(UnauthorizedException.class,
                    () -> service.deleteReview(REVIEW_ID, otherUser));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // refreshBookAggregates
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("refreshBookAggregates")
    class RefreshBookAggregatesTests {

        @Test
        @DisplayName("calculates and saves aggregate stats")
        void calculatesAndSavesStats() throws Exception {
            when(bookRatingRepository.calculateAverageRatingByBookId(BOOK_ID))
                    .thenReturn(BigDecimal.valueOf(4.25).setScale(2, RoundingMode.HALF_UP));
            when(bookRatingRepository.countByBookId(BOOK_ID)).thenReturn(15L);
            when(bookRepository.save(any(Book.class))).thenReturn(book);
            when(bookRatingStatsRepository.findByBookId(BOOK_ID)).thenReturn(Optional.empty());
            when(bookRatingStatsRepository.save(any(BookRatingStats.class)))
                    .thenAnswer(i -> i.getArgument(0));

            service.refreshBookAggregates(book);

            ArgumentCaptor<Book> bookCaptor = ArgumentCaptor.forClass(Book.class);
            verify(bookRepository).save(bookCaptor.capture());
            assertEquals(BigDecimal.valueOf(4.25).setScale(2, RoundingMode.HALF_UP), bookCaptor.getValue().getAverageRating());
            assertEquals(15, bookCaptor.getValue().getRatingsCount());

            verify(bookRatingStatsRepository).save(any(BookRatingStats.class));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getBookRatingStats
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getBookRatingStats")
    class GetBookRatingStatsTests {

        @Test
        @DisplayName("returns cached stats when available")
        void returnsCachedStats() throws Exception {
            BookRatingStats cached = new BookRatingStats(BOOK_ID, BigDecimal.valueOf(4.0), 10);

            when(bookRatingStatsRepository.findByBookId(BOOK_ID)).thenReturn(Optional.of(cached));

            BookRatingStats result = service.getBookRatingStats(BOOK_ID);

            assertEquals(BigDecimal.valueOf(4.0), result.getAvgRating());
            assertEquals(10, result.getTotalRatings());
            verify(bookRatingRepository, never()).calculateAverageRatingByBookId(any());
        }

        @Test
        @DisplayName("calculates and saves stats when not cached")
        void calculatesWhenNotCached() throws Exception {
            when(bookRatingStatsRepository.findByBookId(BOOK_ID)).thenReturn(Optional.empty());
            when(bookRatingRepository.calculateAverageRatingByBookId(BOOK_ID))
                    .thenReturn(BigDecimal.valueOf(3.5).setScale(2, RoundingMode.HALF_UP));
            when(bookRatingRepository.countByBookId(BOOK_ID)).thenReturn(5L);
            when(bookRatingStatsRepository.save(any(BookRatingStats.class)))
                    .thenAnswer(i -> i.getArgument(0));

            BookRatingStats result = service.getBookRatingStats(BOOK_ID);

            assertEquals(BigDecimal.valueOf(3.5).setScale(2, RoundingMode.HALF_UP), result.getAvgRating());
            assertEquals(5, result.getTotalRatings());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // syncBookRatingStatsFromBook
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("syncBookRatingStatsFromBook")
    class SyncBookRatingStatsFromBookTests {

        @Test
        @DisplayName("updates stats when book has non-null values")
        void updatesStatsWhenValuesPresent() throws Exception {
            book.setAverageRating(BigDecimal.valueOf(4.0));
            book.setRatingsCount(20);

            when(bookRatingStatsRepository.findByBookId(BOOK_ID)).thenReturn(Optional.empty());
            when(bookRatingStatsRepository.save(any(BookRatingStats.class)))
                    .thenAnswer(i -> i.getArgument(0));

            service.syncBookRatingStatsFromBook(book);

            verify(bookRatingStatsRepository).save(any(BookRatingStats.class));
        }

        @Test
        @DisplayName("does nothing when book has null values")
        void doesNothingWhenNullValues() throws Exception {
            book.setAverageRating(null);
            book.setRatingsCount(null);

            service.syncBookRatingStatsFromBook(book);

            verify(bookRatingStatsRepository, never()).save(any());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper
    // ─────────────────────────────────────────────────────────────────────────

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}