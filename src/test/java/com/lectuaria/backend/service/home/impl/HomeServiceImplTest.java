package com.lectuaria.backend.service.home.impl;

import com.lectuaria.backend.dto.book.FeaturedSectionsDTO;
import com.lectuaria.backend.dto.home.FriendActivityDTO;
import com.lectuaria.backend.dto.home.HomeResponseDTO;
import com.lectuaria.backend.dto.recommendation.RecommendationDTO;
import com.lectuaria.backend.model.auth.User;
import com.lectuaria.backend.model.auth.UserRole;
import com.lectuaria.backend.model.book.Book;
import com.lectuaria.backend.model.book.BookReview;
import com.lectuaria.backend.model.book.ReviewStatus;
import com.lectuaria.backend.model.book.UserRecommendation;
import com.lectuaria.backend.model.friendship.Friendship;
import com.lectuaria.backend.model.list.UserList;
import com.lectuaria.backend.model.list.UserListBook;
import com.lectuaria.backend.repository.book.BookRatingRepository;
import com.lectuaria.backend.repository.book.BookRepository;
import com.lectuaria.backend.repository.book.BookReviewRepository;
import com.lectuaria.backend.repository.book.UserRecommendationRepository;
import com.lectuaria.backend.repository.friendship.FriendshipRepository;
import com.lectuaria.backend.repository.list.UserListBookRepository;
import com.lectuaria.backend.service.book.IBookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class HomeServiceImplTest {

    @Mock private FriendshipRepository friendshipRepository;
    @Mock private UserListBookRepository listBookRepository;
    @Mock private BookReviewRepository reviewRepository;
    @Mock private BookRatingRepository ratingRepository;
    @Mock private BookRepository bookRepository;
    @Mock private IBookService bookService;
    @Mock private UserRecommendationRepository userRecommendationRepository;

    private HomeServiceImpl homeService;

    private void setId(Object entity, Long id) throws Exception {
        Field f = entity.getClass().getDeclaredField("id");
        f.setAccessible(true);
        f.set(entity, id);
    }

    private void setField(Object entity, String fieldName, Object value) throws Exception {
        Field f = entity.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(entity, value);
    }

    @BeforeEach
    void setUp() {
        List<com.lectuaria.backend.service.home.recommendation.RecommendationStrategy> strategies = List.of(
                new com.lectuaria.backend.service.home.recommendation.PreferenceBasedRecommendationStrategy(
                        ratingRepository, listBookRepository, bookRepository),
                new com.lectuaria.backend.service.home.recommendation.HighRatedRecommendationStrategy(
                        bookRepository));
        homeService = new HomeServiceImpl(
                friendshipRepository, listBookRepository, reviewRepository,
                ratingRepository, bookRepository, bookService, userRecommendationRepository,
                strategies);
    }

    // ===== GET HOME TESTS =====

    @Nested
    @DisplayName("getHome()")
    class GetHomeTests {

        @Test
        @DisplayName("returns combined home response with all sections")
        void getHome_combinesAllSections() throws Exception {
            User user = new User("Test User", "test@test.com", "hash", UserRole.READER, "test", null, null);
            setId(user, 1L);

            when(friendshipRepository.findFriendsByUserId(1L)).thenReturn(List.of());
            when(bookService.getNewCatalogBooks(eq(0), eq(12), any()))
                    .thenReturn(new com.lectuaria.backend.dto.common.PaginatedResponse<>(
                            java.util.List.of(), 0, 12, 0, 0, true, true, false, false));
            when(bookService.getFeaturedSections())
                    .thenReturn(new FeaturedSectionsDTO(List.of(), List.of(), Instant.now()));
            when(userRecommendationRepository.findByUserIdAndHiddenFalse(1L)).thenReturn(List.of());

            HomeResponseDTO response = homeService.getHome(user, 1L);

            assertNotNull(response);
            assertNotNull(response.getFriendActivity());
            assertNotNull(response.getNewCatalogBooks());
            assertNotNull(response.getFeaturedSections());
            assertNotNull(response.getRecommendations());
            verify(bookService).getNewCatalogBooks(0, 12, 1L);
            verify(bookService).getFeaturedSections();
        }
    }

    // ===== GET FRIEND ACTIVITY TESTS =====

    @Nested
    @DisplayName("getFriendActivity()")
    class GetFriendActivityTests {

        @Test
        @DisplayName("returns empty list when user has no friends")
        void getFriendActivity_noFriends() throws Exception {
            User user = new User("Test", "test@test.com", "hash", UserRole.READER, "test", null, null);
            setId(user, 1L);

            when(friendshipRepository.findFriendsByUserId(1L)).thenReturn(List.of());

            List<FriendActivityDTO> result = homeService.getFriendActivity(user, 20);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("returns list and review activities from friends sorted by recency")
        void getFriendActivity_withFriends() throws Exception {
            User user = new User("Test", "test@test.com", "hash", UserRole.READER, "test", null, null);
            User friend = new User("Friend", "friend@test.com", "hash", UserRole.READER, "friend", null, null);
            // Set IDs BEFORE creating Friendship (constructor uses getId())
            setId(user, 1L);
            setId(friend, 2L);
            Friendship friendship = new Friendship(user, friend);
            setId(friendship, 10L);

            UserList list = new UserList(friend, "Por leer", "desc",
                    com.lectuaria.backend.model.list.ListType.CUSTOM,
                    com.lectuaria.backend.model.list.ListVisibility.LISTED);
            setId(list, 5L);

            Book book = new Book();
            setId(book, 100L);
            book.setTitle("Test Book");
            book.setIsbn(123456L);

            UserListBook listBook = new UserListBook(list, book);
            setId(listBook, 50L);
            setField(listBook, "addedAt", Instant.now().minus(1, ChronoUnit.HOURS));

            BookReview review = new BookReview();
            setId(review, 60L);
            setField(review, "user", friend);
            setField(review, "book", book);
            setField(review, "status", ReviewStatus.PUBLISHED);
            setField(review, "createdAt", Instant.now().minus(1, ChronoUnit.HOURS));
            setField(review, "publishedAt", Instant.now().plus(1, ChronoUnit.HOURS)); // newer than listBook

            when(friendshipRepository.findFriendsByUserId(1L)).thenReturn(List.of(friendship));
            when(listBookRepository.findRecentByUserIds(anyList(), any(Pageable.class))).thenReturn(List.of(listBook));
            when(reviewRepository.findRecentByUserIdsAndStatus(anyList(), eq(ReviewStatus.PUBLISHED), any(Pageable.class)))
                    .thenReturn(List.of(review));

            List<FriendActivityDTO> result = homeService.getFriendActivity(user, 20);

            assertEquals(2, result.size());
            assertTrue(result.get(0).getId().startsWith("review-"));
        }

        @Test
        @DisplayName("returns limited results by size parameter")
        void getFriendActivity_respectsSizeLimit() throws Exception {
            User user = new User("Test", "test@test.com", "hash", UserRole.READER, "test", null, null);
            User friend = new User("Friend", "friend@test.com", "hash", UserRole.READER, "friend", null, null);
            setId(user, 1L);
            setId(friend, 2L);
            Friendship friendship = new Friendship(user, friend);
            setId(friendship, 10L);

            Book book = new Book();
            setId(book, 100L);
            book.setTitle("Test Book");
            book.setIsbn(123456L);

            UserList list = new UserList(friend, "Por leer", "desc",
                    com.lectuaria.backend.model.list.ListType.CUSTOM,
                    com.lectuaria.backend.model.list.ListVisibility.LISTED);
            setId(list, 5L);

            UserListBook listBook = new UserListBook(list, book);
            setId(listBook, 50L);
            setField(listBook, "addedAt", Instant.now());

            BookReview review = new BookReview();
            setId(review, 60L);
            setField(review, "user", friend);
            setField(review, "book", book);
            setField(review, "status", ReviewStatus.PUBLISHED);
            setField(review, "createdAt", Instant.now());

            when(friendshipRepository.findFriendsByUserId(1L)).thenReturn(List.of(friendship));
            when(listBookRepository.findRecentByUserIds(anyList(), any(Pageable.class)))
                    .thenReturn(List.of(listBook, listBook));
            when(reviewRepository.findRecentByUserIdsAndStatus(anyList(), eq(ReviewStatus.PUBLISHED), any(Pageable.class)))
                    .thenReturn(List.of(review, review));

            List<FriendActivityDTO> result = homeService.getFriendActivity(user, 1);

            assertTrue(result.size() <= 1);
        }
    }

    // ===== GET RECOMMENDATIONS TESTS =====

    @Nested
    @DisplayName("getRecommendations()")
    class GetRecommendationsTests {

        @Test
        @DisplayName("uses cached recommendations when available and fresh")
        void getRecommendations_usesCached() throws Exception {
            User user = new User("Test", "test@test.com", "hash", UserRole.READER, "test", null, null);
            setId(user, 1L);

            Book book = new Book();
            setId(book, 10L);
            book.setTitle("Cached Book");
            book.setIsbn(123L);

            UserRecommendation rec = new UserRecommendation(user, book, "Cached reason", BigDecimal.valueOf(4.5));
            setId(rec, 5L);
            setField(rec, "calculatedAt", Instant.now());

            when(userRecommendationRepository.findByUserIdAndHiddenFalse(1L)).thenReturn(List.of(rec));

            List<RecommendationDTO> result = homeService.getRecommendations(user, 10);

            assertEquals(1, result.size());
            assertEquals("Cached Book", result.get(0).getBook().getTitle());
            verify(userRecommendationRepository, never()).deleteActiveByUserId(any());
        }

        @Test
        @DisplayName("computes new recommendations when cache is stale")
        void getRecommendations_computesWhenStale() throws Exception {
            User user = new User("Test", "test@test.com", "hash", UserRole.READER, "test", null, null);
            setId(user, 1L);

            Book book = new Book();
            setId(book, 10L);
            book.setTitle("Old Book");
            book.setIsbn(123L);

            UserRecommendation oldRec = new UserRecommendation(user, book, "Old", BigDecimal.valueOf(3.0));
            setId(oldRec, 5L);
            setField(oldRec, "calculatedAt", Instant.now().minus(10, ChronoUnit.DAYS));

            when(userRecommendationRepository.findByUserIdAndHiddenFalse(1L)).thenReturn(List.of(oldRec));
            when(ratingRepository.findRatedBookIdsByUserId(1L)).thenReturn(List.of());
            when(listBookRepository.findBookIdsByUserId(1L)).thenReturn(List.of());
            when(userRecommendationRepository.findHiddenBookIdsByUserId(1L)).thenReturn(List.of());
            lenient().when(ratingRepository.findTopGenresByUserRatings(eq(1L), any(Pageable.class))).thenReturn(List.of());
            lenient().when(listBookRepository.findTopGenresByUserLists(eq(1L), any(Pageable.class))).thenReturn(List.of());
            lenient().when(bookRepository.findRecommendationsByGenreIds(anyList(), any(Pageable.class))).thenReturn(List.of());
            lenient().when(bookRepository.findFallbackRecommendations(any(Pageable.class))).thenReturn(List.of());
            lenient().when(userRecommendationRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

            List<RecommendationDTO> result = homeService.getRecommendations(user, 5);

            // No candidates, no fallback → result empty but computation happened
            assertTrue(result.isEmpty());
            verify(userRecommendationRepository).deleteActiveByUserId(1L);
        }

        @Test
        @DisplayName("returns fallback recommendations when no genre preferences")
        void getRecommendations_fallbackWhenNoGenres() throws Exception {
            User user = new User("Test", "test@test.com", "hash", UserRole.READER, "test", null, null);
            setId(user, 1L);

            Book fallback = new Book();
            setId(fallback, 30L);
            fallback.setTitle("Fallback Book");
            fallback.setIsbn(789L);

            when(userRecommendationRepository.findByUserIdAndHiddenFalse(1L)).thenReturn(List.of());
            when(ratingRepository.findRatedBookIdsByUserId(1L)).thenReturn(List.of());
            when(listBookRepository.findBookIdsByUserId(1L)).thenReturn(List.of());
            when(userRecommendationRepository.findHiddenBookIdsByUserId(1L)).thenReturn(List.of());
            lenient().when(ratingRepository.findTopGenresByUserRatings(eq(1L), any(Pageable.class))).thenReturn(
                    java.util.List.<Object[]>of(new Object[]{1L, "Ficci\u00f3n"}));
            lenient().when(listBookRepository.findTopGenresByUserLists(eq(1L), any(Pageable.class))).thenReturn(List.of());
            lenient().when(bookRepository.findRecommendationsByGenreIds(anyList(), any(Pageable.class))).thenReturn(List.of());
            when(bookRepository.findFallbackRecommendations(any(Pageable.class))).thenReturn(java.util.List.of(fallback));
            lenient().when(userRecommendationRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

            List<RecommendationDTO> result = homeService.getRecommendations(user, 5);

            assertEquals(1, result.size());
            assertEquals("Fallback Book", result.get(0).getBook().getTitle());
            verify(bookRepository).findFallbackRecommendations(any(Pageable.class));
        }
    }

    // ===== HIDE RECOMMENDATION TESTS =====

    @Nested
    @DisplayName("hideRecommendation()")
    class HideRecommendationTests {

        @Test
        @DisplayName("hides existing recommendation by setting hidden=true")
        void hideRecommendation_existing() throws Exception {
            User user = new User("Test", "test@test.com", "hash", UserRole.READER, "test", null, null);
            setId(user, 1L);

            Book book = new Book();
            setId(book, 10L);
            book.setTitle("Book to Hide");
            book.setIsbn(123L);

            UserRecommendation rec = new UserRecommendation(user, book, "Some reason", BigDecimal.valueOf(3.0));
            setId(rec, 5L);
            rec.setHidden(false);

            when(userRecommendationRepository.findByUserIdAndBookId(1L, 10L)).thenReturn(Optional.of(rec));
            when(userRecommendationRepository.save(any(UserRecommendation.class))).thenAnswer(inv -> inv.getArgument(0));

            homeService.hideRecommendation(user, 10L);

            ArgumentCaptor<UserRecommendation> captor = ArgumentCaptor.forClass(UserRecommendation.class);
            verify(userRecommendationRepository).save(captor.capture());
            assertTrue(captor.getValue().getHidden());
        }

        @Test
        @DisplayName("creates new hidden recommendation when not found in DB")
        void hideRecommendation_createsNewHidden() throws Exception {
            User user = new User("Test", "test@test.com", "hash", UserRole.READER, "test", null, null);
            setId(user, 1L);

            Book book = new Book();
            setId(book, 20L);
            book.setTitle("New Hidden Book");
            book.setIsbn(456L);

            when(userRecommendationRepository.findByUserIdAndBookId(1L, 20L)).thenReturn(Optional.empty());
            when(bookRepository.findById(20L)).thenReturn(Optional.of(book));
            when(userRecommendationRepository.save(any(UserRecommendation.class))).thenAnswer(inv -> {
                UserRecommendation r = inv.getArgument(0);
                setId(r, 99L);
                return r;
            });

            homeService.hideRecommendation(user, 20L);

            ArgumentCaptor<UserRecommendation> captor = ArgumentCaptor.forClass(UserRecommendation.class);
            verify(userRecommendationRepository).save(captor.capture());
            UserRecommendation saved = captor.getValue();
            assertTrue(saved.getHidden());
            assertEquals("Ocultado por el usuario", saved.getReason());
            assertEquals(BigDecimal.ZERO, saved.getScore());
        }

        @Test
        @DisplayName("throws IllegalArgumentException when book not found for new hidden recommendation")
        void hideRecommendation_bookNotFound() throws Exception {
            User user = new User("Test", "test@test.com", "hash", UserRole.READER, "test", null, null);
            setId(user, 1L);

            when(userRecommendationRepository.findByUserIdAndBookId(1L, 999L)).thenReturn(Optional.empty());
            when(bookRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class, () -> homeService.hideRecommendation(user, 999L));
        }
    }
}