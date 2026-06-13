package com.lectuaria.backend.service.user.impl;

import com.lectuaria.backend.dto.statistics.GenreCountDTO;
import com.lectuaria.backend.dto.statistics.MonthlyBooksReadDTO;
import com.lectuaria.backend.dto.statistics.ReadingStatisticsDTO;
import com.lectuaria.backend.dto.statistics.SocialStatisticsDTO;
import com.lectuaria.backend.dto.statistics.YearComparisonDTO;
import com.lectuaria.backend.dto.user.FriendActivityDTO;
import com.lectuaria.backend.dto.user.FriendshipStatus;
import com.lectuaria.backend.dto.user.UserProfileDTO;
import com.lectuaria.backend.dto.user.UserStatsDTO;
import com.lectuaria.backend.exception.ForbiddenException;
import com.lectuaria.backend.exception.ResourceNotFoundException;
import com.lectuaria.backend.model.auth.User;
import com.lectuaria.backend.model.auth.UserRole;
import com.lectuaria.backend.model.book.BookReview;
import com.lectuaria.backend.model.book.Book;
import com.lectuaria.backend.model.book.ReviewStatus;
import com.lectuaria.backend.model.friendship.Friendship;
import com.lectuaria.backend.model.friendship.FriendshipRequest;
import com.lectuaria.backend.model.list.ListVisibility;
import com.lectuaria.backend.model.list.UserList;
import com.lectuaria.backend.model.list.UserListBook;
import com.lectuaria.backend.model.list.UserListShare;
import com.lectuaria.backend.model.user.UserPrivacySettings;
import com.lectuaria.backend.model.user.Visibility;
import com.lectuaria.backend.repository.book.BookRatingRepository;
import com.lectuaria.backend.repository.book.BookReviewRepository;
import com.lectuaria.backend.repository.book.BookShareRepository;
import com.lectuaria.backend.repository.book.GenreRepository;
import com.lectuaria.backend.repository.auth.UserRepository;
import com.lectuaria.backend.repository.friendship.FriendshipRepository;
import com.lectuaria.backend.repository.friendship.FriendshipRequestRepository;
import com.lectuaria.backend.repository.list.UserListBookRepository;
import com.lectuaria.backend.repository.list.UserListRepository;
import com.lectuaria.backend.repository.list.UserListShareRepository;
import com.lectuaria.backend.repository.user.UserPrivacySettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private FriendshipRepository friendshipRepository;
    @Mock private FriendshipRequestRepository friendshipRequestRepository;
    @Mock private BookReviewRepository bookReviewRepository;
    @Mock private BookRatingRepository bookRatingRepository;
    @Mock private UserListBookRepository userListBookRepository;
    @Mock private UserListShareRepository userListShareRepository;
    @Mock private UserListRepository userListRepository;
    @Mock private BookShareRepository bookShareRepository;
    @Mock private GenreRepository genreRepository;
    @Mock private UserPrivacySettingsRepository privacyRepository;

    private UserProfileServiceImpl service;

    private User profileUser;
    private User currentUser;

    private static final Long PROFILE_ID = 1L;
    private static final Long CURRENT_ID = 2L;

    @BeforeEach
    void setUp() throws Exception {
        service = new UserProfileServiceImpl(
                userRepository, friendshipRepository, friendshipRequestRepository,
                bookReviewRepository, bookRatingRepository, userListBookRepository,
                userListShareRepository, userListRepository, bookShareRepository,
                genreRepository, privacyRepository
        );

        profileUser = new User("Perfil", "perfil@test.com", "hash", UserRole.READER, "perfil_user", null, null);
        setField(profileUser, "id", PROFILE_ID);

        currentUser = new User("Actual", "actual@test.com", "hash", UserRole.READER, "actual_user", null, null);
        setField(currentUser, "id", CURRENT_ID);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getUserStats
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getUserStats")
    class GetUserStatsTests {

        @Test
        @DisplayName("returns stats with counts")
        void returnsStatsWithCounts() throws Exception {
            when(userRepository.findByUsernameIgnoreCase("perfil_user")).thenReturn(Optional.of(profileUser));

            Friendship friendship = new Friendship(profileUser, currentUser);
            when(friendshipRepository.findFriendsByUserId(PROFILE_ID)).thenReturn(List.of(friendship));

            when(bookReviewRepository.countByUserIdAndStatus(PROFILE_ID, ReviewStatus.published)).thenReturn(5L);
            when(userListBookRepository.countDistinctByUserListUserId(PROFILE_ID)).thenReturn(3L);
            when(bookRatingRepository.findRatedBookIdsByUserId(PROFILE_ID)).thenReturn(List.of(100L, 200L));
            when(userListBookRepository.findReadBookIdsInListsByUserId(PROFILE_ID)).thenReturn(List.of(100L, 300L));

            UserStatsDTO result = service.getUserStats("perfil_user");

            assertThat(result.getBooksRead()).isEqualTo(3); // unique book IDs
            assertThat(result.getReviewsCount()).isEqualTo(5);
            assertThat(result.getFriendsCount()).isEqualTo(1);
            assertThat(result.getFavoritesCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("throws when user not found")
        void throwsWhenUserNotFound() throws Exception {
            when(userRepository.findByUsernameIgnoreCase("unknown")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getUserStats("unknown"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Usuario no encontrado");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getReadingStatistics
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getReadingStatistics")
    class GetReadingStatisticsTests {

        @Test
        @DisplayName("calculates monthly reading statistics")
        void calculatesMonthlyReadingStatistics() throws Exception {
            when(userRepository.findByUsernameIgnoreCase("perfil_user")).thenReturn(Optional.of(profileUser));

            int currentYear = LocalDate.now().getYear();

            List<Object[]> ratedBooks = new java.util.ArrayList<>();
            ratedBooks.add(new Object[]{100L, LocalDate.of(currentYear, 3, 15).atStartOfDay().toInstant(java.time.ZoneOffset.UTC)});
            ratedBooks.add(new Object[]{200L, LocalDate.of(currentYear, 5, 20).atStartOfDay().toInstant(java.time.ZoneOffset.UTC)});

            List<Object[]> listBooks = new java.util.ArrayList<>();
            listBooks.add(new Object[]{300L, LocalDate.of(currentYear, 7, 10).atStartOfDay().toInstant(java.time.ZoneOffset.UTC)});

            when(bookRatingRepository.findRatedBooksAndCreatedAtByUserId(PROFILE_ID)).thenReturn(ratedBooks);
            when(userListBookRepository.findReadBooksAndAddedAtByUserId(PROFILE_ID)).thenReturn(listBooks);

            when(bookReviewRepository.countByUserIdAndStatus(PROFILE_ID, ReviewStatus.published)).thenReturn(2L);

            when(genreRepository.findTopGenresByBookIds(anyList(), any(PageRequest.class))).thenReturn(List.of());

            ReadingStatisticsDTO result = service.getReadingStatistics("perfil_user");

            assertThat(result.getTotalBooksRead()).isEqualTo(3L);
            assertThat(result.getReviewsCount()).isEqualTo(2);
            assertThat(result.getBooksReadByMonth()).hasSize(12);
            assertThat(result.getBooksReadByMonth().get(2).getBooksRead()).isEqualTo(1L); // March = 1
            assertThat(result.getBooksReadByMonth().get(4).getBooksRead()).isEqualTo(1L); // May = 1
            assertThat(result.getBooksReadByMonth().get(6).getBooksRead()).isEqualTo(1L); // July = 1

            assertThat(result.getYearComparison().getCurrentYear()).isEqualTo(currentYear);
            assertThat(result.getYearComparison().getCurrentYearBooks()).isEqualTo(3L);
        }

        @Test
        @DisplayName("returns empty stats when no books read")
        void returnsEmptyStats() throws Exception {
            when(userRepository.findByUsernameIgnoreCase("perfil_user")).thenReturn(Optional.of(profileUser));
            when(bookRatingRepository.findRatedBooksAndCreatedAtByUserId(PROFILE_ID)).thenReturn(new java.util.ArrayList<>());
            when(userListBookRepository.findReadBooksAndAddedAtByUserId(PROFILE_ID)).thenReturn(new java.util.ArrayList<>());
            when(bookReviewRepository.countByUserIdAndStatus(PROFILE_ID, ReviewStatus.published)).thenReturn(0L);

            ReadingStatisticsDTO result = service.getReadingStatistics("perfil_user");

            assertThat(result.getTotalBooksRead()).isEqualTo(0L);
            assertThat(result.getMostReadGenres()).isEmpty();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getSocialStatistics
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getSocialStatistics")
    class GetSocialStatisticsTests {

        @Test
        @DisplayName("returns social statistics")
        void returnsSocialStatistics() throws Exception {
            when(userRepository.findByUsernameIgnoreCase("perfil_user")).thenReturn(Optional.of(profileUser));

            Friendship f1 = mock(Friendship.class);
            Friendship f2 = mock(Friendship.class);
            Friendship f3 = mock(Friendship.class);
            when(friendshipRepository.findFriendsByUserId(PROFILE_ID)).thenReturn(List.of(f1, f2, f3));

            UserListShare ls1 = mock(UserListShare.class);
            UserListShare ls2 = mock(UserListShare.class);
            UserListShare ls3 = mock(UserListShare.class);
            when(userListShareRepository.findByReceiverIdAndIsActiveTrue(PROFILE_ID)).thenReturn(List.of(ls1));
            when(userListShareRepository.findByOwnerIdAndIsActiveTrue(PROFILE_ID)).thenReturn(List.of(ls2, ls3));

            com.lectuaria.backend.model.book.BookShare bs1 = mock(com.lectuaria.backend.model.book.BookShare.class);
            com.lectuaria.backend.model.book.BookShare bs2 = mock(com.lectuaria.backend.model.book.BookShare.class);
            com.lectuaria.backend.model.book.BookShare bs3 = mock(com.lectuaria.backend.model.book.BookShare.class);
            when(bookShareRepository.findBySenderId(PROFILE_ID)).thenReturn(List.of(bs1));
            when(bookShareRepository.findByReceiverId(PROFILE_ID)).thenReturn(List.of(bs2, bs3));

            SocialStatisticsDTO result = service.getSocialStatistics("perfil_user");

            assertThat(result.getFriendsCount()).isEqualTo(3L);
            assertThat(result.getListsSharedByFriends()).isEqualTo(1L);
            assertThat(result.getListsIShared()).isEqualTo(2L);
            assertThat(result.getBooksSharedWithFriends()).isEqualTo(1L);
            assertThat(result.getBooksSharedByFriends()).isEqualTo(2L);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getUserProfileByUsername
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getUserProfileByUsername")
    class GetUserProfileByUsernameTests {

        @Test
        @DisplayName("returns profile for public user with PUBLIC visibility")
        void returnsPublicProfile() throws Exception {
            UserPrivacySettings privacy = new UserPrivacySettings(profileUser);
            privacy.setProfileVisibility(Visibility.PUBLIC);
            privacy.setReviewsVisibility(Visibility.PUBLIC);
            privacy.setReadingListsVisibility(Visibility.PUBLIC);
            privacy.setReadingListsActivityVisibility(Visibility.PUBLIC);
            privacy.setFriendsVisibility(Visibility.PUBLIC);

            when(userRepository.findByUsernameIgnoreCase("perfil_user")).thenReturn(Optional.of(profileUser));
            when(privacyRepository.findByUserId(PROFILE_ID)).thenReturn(Optional.of(privacy));
            when(friendshipRepository.findByUsers(PROFILE_ID, CURRENT_ID)).thenReturn(Optional.empty());

            when(bookReviewRepository.countByUserIdAndStatus(PROFILE_ID, ReviewStatus.published)).thenReturn(5L);
            when(userListBookRepository.countDistinctByUserListUserId(PROFILE_ID)).thenReturn(3L);
            when(bookRatingRepository.findRatedBookIdsByUserId(PROFILE_ID)).thenReturn(List.of(100L));
            when(userListBookRepository.findReadBookIdsInListsByUserId(PROFILE_ID)).thenReturn(List.of());

            when(bookReviewRepository.findRecentByUserIdsAndStatus(anyList(), eq(ReviewStatus.published), any(PageRequest.class)))
                    .thenReturn(List.of());
            when(userListRepository.findByUserIdOrderByCreatedAtAsc(PROFILE_ID)).thenReturn(List.of());
            when(friendshipRepository.findFriendsByUserId(PROFILE_ID)).thenReturn(List.of());

            UserProfileDTO result = service.getUserProfileByUsername("perfil_user", currentUser);

            assertThat(result.getId()).isEqualTo(PROFILE_ID);
            assertThat(result.getUsername()).isEqualTo("perfil_user");
            assertThat(result.getStats()).isNotNull();
            assertThat(result.getFriendshipStatus()).isEqualTo(FriendshipStatus.NONE);
            assertThat(result.getPrivacy().isProfileVisible()).isTrue();
        }

        @Test
        @DisplayName("returns own profile with all fields")
        void returnsOwnProfile() throws Exception {
            UserPrivacySettings privacy = new UserPrivacySettings(profileUser);
            // defaults to FRIENDS visibility

when(userRepository.findByUsernameIgnoreCase("perfil_user")).thenReturn(Optional.of(profileUser));
            when(privacyRepository.findByUserId(PROFILE_ID)).thenReturn(Optional.of(privacy));

            UserProfileDTO result = service.getUserProfileByUsername("perfil_user", profileUser);

            assertThat(result.getFriendshipStatus()).isEqualTo(FriendshipStatus.SELF);
        }

        @Test
        @DisplayName("throws ForbiddenException for private profile")
        void throwsForbiddenForPrivateProfile() throws Exception {
            UserPrivacySettings privacy = new UserPrivacySettings(profileUser);
            privacy.setProfileVisibility(Visibility.PRIVATE);

            when(userRepository.findByUsernameIgnoreCase("perfil_user")).thenReturn(Optional.of(profileUser));
            when(privacyRepository.findByUserId(PROFILE_ID)).thenReturn(Optional.of(privacy));

            assertThatThrownBy(() -> service.getUserProfileByUsername("perfil_user", currentUser))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("privado");
        }

        @Test
        @DisplayName("throws ForbiddenException for FRIENDS-only profile when not friends")
        void throwsForbiddenWhenNotFriends() throws Exception {
            UserPrivacySettings privacy = new UserPrivacySettings(profileUser);
            privacy.setProfileVisibility(Visibility.FRIENDS);

            when(userRepository.findByUsernameIgnoreCase("perfil_user")).thenReturn(Optional.of(profileUser));
            when(privacyRepository.findByUserId(PROFILE_ID)).thenReturn(Optional.of(privacy));
            when(friendshipRepository.findByUsers(PROFILE_ID, CURRENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getUserProfileByUsername("perfil_user", currentUser))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("returns profile when FRIENDS and users are friends")
        void returnsProfileWhenFriends() throws Exception {
            UserPrivacySettings privacy = new UserPrivacySettings(profileUser);
            privacy.setProfileVisibility(Visibility.FRIENDS);
            privacy.setReviewsVisibility(Visibility.FRIENDS);
            privacy.setReadingListsVisibility(Visibility.FRIENDS);
            privacy.setReadingListsActivityVisibility(Visibility.FRIENDS);
            privacy.setFriendsVisibility(Visibility.FRIENDS);

            Friendship friendship = new Friendship(profileUser, currentUser);

            when(userRepository.findByUsernameIgnoreCase("perfil_user")).thenReturn(Optional.of(profileUser));
            when(privacyRepository.findByUserId(PROFILE_ID)).thenReturn(Optional.of(privacy));
            when(friendshipRepository.findByUsers(PROFILE_ID, CURRENT_ID)).thenReturn(Optional.of(friendship));

            when(bookReviewRepository.countByUserIdAndStatus(PROFILE_ID, ReviewStatus.published)).thenReturn(0L);
            when(userListBookRepository.countDistinctByUserListUserId(PROFILE_ID)).thenReturn(0L);
            when(bookRatingRepository.findRatedBookIdsByUserId(PROFILE_ID)).thenReturn(List.of());
            when(userListBookRepository.findReadBookIdsInListsByUserId(PROFILE_ID)).thenReturn(List.of());

            UserProfileDTO result = service.getUserProfileByUsername("perfil_user", currentUser);

            assertThat(result.getPrivacy().isProfileVisible()).isTrue();
        }

        @Test
        @DisplayName("throws when profile user not found")
        void throwsWhenProfileNotFound() throws Exception {
            when(userRepository.findByUsernameIgnoreCase("unknown")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getUserProfileByUsername("unknown", currentUser))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getFriendActivity
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getFriendActivity")
    class GetFriendActivityTests {

        @Test
        @DisplayName("returns empty list when currentUser is null and default privacy is FRIENDS")
        void returnsEmptyWhenNoUser() throws Exception {
            // Default de UserPrivacySettings: todos los Visibility en FRIENDS.
            // Un visitante anonimo no es amigo, asi que FRIENDS => no ve nada.
            when(userRepository.findByUsernameIgnoreCase("perfil_user")).thenReturn(Optional.of(profileUser));
            when(privacyRepository.findByUserId(PROFILE_ID)).thenReturn(Optional.empty());
            when(userRepository.findById(PROFILE_ID)).thenReturn(Optional.of(profileUser));
            // getOrCreatePrivacySettings llama a save cuando no existe; mockeamos
            // para que devuelva la misma instancia creada en el lambda.
            when(privacyRepository.save(any(UserPrivacySettings.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            List<FriendActivityDTO> result = service.getFriendActivity("perfil_user", null);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("anonymous viewer sees activity when reviewsVisibility is PUBLIC")
        void anonymousSeesActivityWhenPublic() throws Exception {
            UserPrivacySettings privacy = new UserPrivacySettings(profileUser);
            privacy.setReviewsVisibility(Visibility.PUBLIC);
            privacy.setReadingListsActivityVisibility(Visibility.PUBLIC);

            when(userRepository.findByUsernameIgnoreCase("perfil_user")).thenReturn(Optional.of(profileUser));
            when(privacyRepository.findByUserId(PROFILE_ID)).thenReturn(Optional.of(privacy));

            BookReview review = mock(BookReview.class);
            when(review.getId()).thenReturn(100L);
            when(review.getUser()).thenReturn(profileUser);
            when(review.getCreatedAt()).thenReturn(Instant.now());
            when(review.getEditedAt()).thenReturn(null);
            when(review.getBook()).thenReturn(mock(Book.class));
            when(review.getRating()).thenReturn(BigDecimal.valueOf(4.0));
            when(review.getReviewText()).thenReturn("Great book");
            when(review.getStatus()).thenReturn(ReviewStatus.published);
            when(review.getBook().getId()).thenReturn(1L);
            when(review.getBook().getTitle()).thenReturn("Test Book");
            when(review.getBook().getIsbn()).thenReturn(123456L);
            when(review.getBook().getCoverUrl()).thenReturn(null);
            when(review.getBook().getAuthors()).thenReturn(java.util.Collections.emptyList());

            when(bookReviewRepository.findRecentByUserIdsAndStatus(anyList(), eq(ReviewStatus.published), any(PageRequest.class)))
                    .thenReturn(List.of(review));

            List<FriendActivityDTO> result = service.getFriendActivity("perfil_user", null);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getActivityType()).isEqualTo("BOOK_REVIEWED");
        }

        @Test
        @DisplayName("returns empty list when viewing non-friend's activity with default FRIENDS privacy")
        void returnsEmptyWhenNotFriends() throws Exception {
            when(userRepository.findByUsernameIgnoreCase("perfil_user")).thenReturn(Optional.of(profileUser));
            when(privacyRepository.findByUserId(PROFILE_ID)).thenReturn(Optional.empty());
            when(userRepository.findById(PROFILE_ID)).thenReturn(Optional.of(profileUser));
            when(privacyRepository.save(any(UserPrivacySettings.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            List<FriendActivityDTO> result = service.getFriendActivity("perfil_user", currentUser);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("returns own activity when viewing self")
        void returnsOwnActivity() throws Exception {
            UserPrivacySettings privacy = new UserPrivacySettings(profileUser);
            privacy.setReviewsVisibility(Visibility.PUBLIC);
            privacy.setReadingListsActivityVisibility(Visibility.PUBLIC);

            when(userRepository.findByUsernameIgnoreCase("perfil_user")).thenReturn(Optional.of(profileUser));
            when(privacyRepository.findByUserId(PROFILE_ID)).thenReturn(Optional.of(privacy));
            when(friendshipRepository.findByUsers(PROFILE_ID, PROFILE_ID)).thenReturn(Optional.empty());

            BookReview review = mock(BookReview.class);
            when(review.getId()).thenReturn(100L);
            when(review.getUser()).thenReturn(profileUser);
            when(review.getCreatedAt()).thenReturn(Instant.now());
            when(review.getEditedAt()).thenReturn(null);
            when(review.getBook()).thenReturn(mock(Book.class));
            when(review.getRating()).thenReturn(BigDecimal.valueOf(4.0));
            when(review.getReviewText()).thenReturn("Great book");
            when(review.getStatus()).thenReturn(ReviewStatus.published);
            when(review.getBook().getId()).thenReturn(1L);
            when(review.getBook().getTitle()).thenReturn("Test Book");
            when(review.getBook().getIsbn()).thenReturn(123456L);
            when(review.getBook().getCoverUrl()).thenReturn(null);
            when(review.getBook().getAuthors()).thenReturn(java.util.Collections.emptyList());

            when(bookReviewRepository.findRecentByUserIdsAndStatus(anyList(), eq(ReviewStatus.published), any(PageRequest.class)))
                    .thenReturn(List.of(review));

            List<FriendActivityDTO> result = service.getFriendActivity("perfil_user", profileUser);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getActivityType()).isEqualTo("BOOK_REVIEWED");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper
    // ─────────────────────────────────────────────────────────────────────────

    private void setField(Object target, String fieldName, Object value) throws Exception {
        java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}