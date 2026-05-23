package com.lectuaria.backend.controller.user;

import com.lectuaria.backend.dto.statistics.GenreCountDTO;
import com.lectuaria.backend.dto.statistics.ReadingStatisticsDTO;
import com.lectuaria.backend.dto.statistics.SocialStatisticsDTO;
import com.lectuaria.backend.dto.statistics.YearComparisonDTO;
import com.lectuaria.backend.dto.user.FriendActivityDTO;
import com.lectuaria.backend.dto.user.UserProfileDTO;
import com.lectuaria.backend.dto.user.UserStatsDTO;
import com.lectuaria.backend.model.auth.User;
import com.lectuaria.backend.model.auth.UserRole;
import com.lectuaria.backend.repository.auth.UserRepository;
import com.lectuaria.backend.security.JwtService;
import com.lectuaria.backend.service.user.IUserProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IUserProfileService userProfileService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtService jwtService;

    private User readerUser;

    @BeforeEach
    void setUp() {
        readerUser = new User("Reader", "reader@test.com", "hash", UserRole.READER, "reader", null, null);
        setId(readerUser, 20L);
        SecurityContextHolder.clearContext();
        when(jwtService.extractEmail(anyString())).thenReturn("reader@test.com");
    }

    private void setId(Object entity, Long id) {
        try {
            java.lang.reflect.Field f = entity.getClass().getDeclaredField("id");
            f.setAccessible(true);
            f.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void withUser(String email, User user, UserRole role) {
        List<SimpleGrantedAuthority> auths = List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(email, null, auths);
        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(auth);
        SecurityContextHolder.setContext(ctx);
    }

    private String authHeader(String token) {
        return "Bearer " + token;
    }

    // ========== GET /api/users/{usernameSlug} ==========

    @Nested
    class GetUserProfile {

        @Test
        void getProfile_publicUser_returnsOk() throws Exception {
            UserProfileDTO profile = new UserProfileDTO(
                    5L, "publicuser", "Public User",
                    "http://avatar.url", "A passionate reader.",
                    Instant.parse("2022-05-10T00:00:00Z")
            );
            when(userProfileService.getUserProfileByUsername(eq("publicuser"), isNull())).thenReturn(profile);

            mockMvc.perform(get("/api/users/publicuser"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value("publicuser"))
                    .andExpect(jsonPath("$.fullName").value("Public User"))
                    .andExpect(jsonPath("$.biography").value("A passionate reader."));
        }

        @Test
        void getProfile_withValidToken_returnsOk() throws Exception {
            when(userRepository.findByEmail("reader@test.com")).thenReturn(Optional.of(readerUser));

            UserProfileDTO profile = new UserProfileDTO(
                    5L, "reader", "Reader",
                    "http://avatar.url", "Reading enthusiast.",
                    Instant.parse("2023-01-15T00:00:00Z")
            );
            when(userProfileService.getUserProfileByUsername(eq("reader"), eq(readerUser))).thenReturn(profile);

            mockMvc.perform(get("/api/users/reader")
                            .header("Authorization", authHeader("any.token")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value("reader"))
                    .andExpect(jsonPath("$.fullName").value("Reader"));
        }

        @Test
        void getProfile_withInvalidToken_fallsBackToNull() throws Exception {
            when(jwtService.extractEmail("bad.token")).thenThrow(new RuntimeException("Invalid token"));

            UserProfileDTO profile = new UserProfileDTO(
                    7L, "someuser", "Some User",
                    null, null,
                    Instant.parse("2023-06-01T00:00:00Z")
            );
            when(userProfileService.getUserProfileByUsername(eq("someuser"), isNull())).thenReturn(profile);

            mockMvc.perform(get("/api/users/someuser")
                            .header("Authorization", "Bearer bad.token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value("someuser"));
        }

        @Test
        void getProfile_notFound_returns404() throws Exception {
            when(userProfileService.getUserProfileByUsername(eq("nonexistent"), isNull()))
                    .thenThrow(new com.lectuaria.backend.exception.ResourceNotFoundException("Usuario no encontrado"));

            mockMvc.perform(get("/api/users/nonexistent"))
                    .andExpect(status().isNotFound());
        }
    }

    // ========== GET /api/users/{usernameSlug}/stats ==========

    @Nested
    class GetUserStats {

        @Test
        void getStats_returnsUserStats() throws Exception {
            UserStatsDTO stats = new UserStatsDTO(50, 25, 8, 3);
            when(userProfileService.getUserStats("statsuser")).thenReturn(stats);

            mockMvc.perform(get("/api/users/statsuser/stats"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.booksRead").value(50))
                    .andExpect(jsonPath("$.reviewsCount").value(25))
                    .andExpect(jsonPath("$.friendsCount").value(8))
                    .andExpect(jsonPath("$.favoritesCount").value(3));
        }

        @Test
        void getStats_userNotFound_returns404() throws Exception {
            when(userProfileService.getUserStats("ghostuser"))
                    .thenThrow(new com.lectuaria.backend.exception.ResourceNotFoundException("Usuario no encontrado"));

            mockMvc.perform(get("/api/users/ghostuser/stats"))
                    .andExpect(status().isNotFound());
        }
    }

    // ========== GET /api/users/{usernameSlug}/reading-statistics ==========

    @Nested
    class GetReadingStatistics {

        @Test
        void getReadingStats_withValidToken_returnsOk() throws Exception {
            withUser("reader@test.com", readerUser, UserRole.READER);
            when(userRepository.findByEmail("reader@test.com")).thenReturn(Optional.of(readerUser));

            ReadingStatisticsDTO stats = new ReadingStatisticsDTO(
                    25L,
                    5,
                    List.of(new GenreCountDTO(1L, "Fiction", 10L)),
                    List.of(),
                    null,
                    Instant.now()
            );
            when(userProfileService.getReadingStatistics("reader")).thenReturn(stats);

            mockMvc.perform(get("/api/users/reader/reading-statistics")
                            .header("Authorization", authHeader("any.token")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalBooksRead").value(25))
                    .andExpect(jsonPath("$.reviewsCount").value(5))
                    .andExpect(jsonPath("$.mostReadGenres[0].genreName").value("Fiction"));
        }

        @Test
        void getReadingStats_unauthenticated_returns401() throws Exception {
            // SecurityConfig requires READER or ADMIN role for this endpoint
            mockMvc.perform(get("/api/users/anyuser/reading-statistics"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void getReadingStats_userNotFound_returns404() throws Exception {
            withUser("reader@test.com", readerUser, UserRole.READER);
            when(userRepository.findByEmail("reader@test.com")).thenReturn(Optional.of(readerUser));

            when(userProfileService.getReadingStatistics("ghostreader"))
                    .thenThrow(new com.lectuaria.backend.exception.ResourceNotFoundException("Usuario no encontrado"));

            mockMvc.perform(get("/api/users/ghostreader/reading-statistics")
                            .header("Authorization", authHeader("any.token")))
                    .andExpect(status().isNotFound());
        }
    }

    // ========== GET /api/users/{usernameSlug}/social-statistics ==========

    @Nested
    class GetSocialStatistics {

        @Test
        void getSocialStats_returnsOk() throws Exception {
            SocialStatisticsDTO stats = new SocialStatisticsDTO(
                    15L, 8L, 5L, 20L, 12L, Instant.now()
            );
            when(userProfileService.getSocialStatistics("socialuser")).thenReturn(stats);

            mockMvc.perform(get("/api/users/socialuser/social-statistics"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.friendsCount").value(15))
                    .andExpect(jsonPath("$.listsSharedByFriends").value(8))
                    .andExpect(jsonPath("$.booksSharedWithFriends").value(20));
        }

        @Test
        void getSocialStats_userNotFound_returns404() throws Exception {
            when(userProfileService.getSocialStatistics("ghostsocial"))
                    .thenThrow(new com.lectuaria.backend.exception.ResourceNotFoundException("Usuario no encontrado"));

            mockMvc.perform(get("/api/users/ghostsocial/social-statistics"))
                    .andExpect(status().isNotFound());
        }
    }

    // ========== GET /api/users/{usernameSlug}/activity ==========

    @Nested
    class GetFriendActivity {

        @Test
        void getActivity_noAuth_returnsOkWithEmptyList() throws Exception {
            when(userProfileService.getFriendActivity(eq("activityuser"), isNull()))
                    .thenReturn(List.of());

            mockMvc.perform(get("/api/users/activityuser/activity"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        void getActivity_withValidToken_returnsActivities() throws Exception {
            when(userRepository.findByEmail("reader@test.com")).thenReturn(Optional.of(readerUser));

            List<FriendActivityDTO> activities = List.of(
                    new FriendActivityDTO(
                            1L, 20L, "Reader",
                            "BOOK_REVIEWED",
                            Instant.now(), null,
                            5L, "Reviewed Book", "9780000000000", "http://cover.url",
                            List.of("Author"), 4, "Great book!", "PUBLISHED", 2,
                            null, null, null, null, null
                    )
            );
            when(userProfileService.getFriendActivity(eq("reader"), eq(readerUser))).thenReturn(activities);

            mockMvc.perform(get("/api/users/reader/activity")
                            .header("Authorization", authHeader("any.token")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].userName").value("Reader"))
                    .andExpect(jsonPath("$[0].activityType").value("BOOK_REVIEWED"))
                    .andExpect(jsonPath("$[0].bookTitle").value("Reviewed Book"));
        }

        @Test
        void getActivity_withInvalidToken_fallsBackToNullUser() throws Exception {
            when(jwtService.extractEmail("bad.token")).thenThrow(new RuntimeException("bad token"));

            List<FriendActivityDTO> activities = List.of(
                    new FriendActivityDTO(
                            2L, 30L, "X",
                            "BOOK_ADDED_TO_LIST",
                            Instant.now(), null,
                            null, null, null, null, null, null, null, null, null,
                            7L, "My List", true, "token123", "PUBLIC"
                    )
            );
            when(userProfileService.getFriendActivity(eq("x"), isNull())).thenReturn(activities);

            mockMvc.perform(get("/api/users/x/activity")
                            .header("Authorization", "Bearer bad.token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].userName").value("X"))
                    .andExpect(jsonPath("$[0].listName").value("My List"));
        }

        @Test
        void getActivity_userNotFound_returns404() throws Exception {
            when(userProfileService.getFriendActivity(eq("ghostactivity"), isNull()))
                    .thenThrow(new com.lectuaria.backend.exception.ResourceNotFoundException("Usuario no encontrado"));

            mockMvc.perform(get("/api/users/ghostactivity/activity"))
                    .andExpect(status().isNotFound());
        }
    }
}