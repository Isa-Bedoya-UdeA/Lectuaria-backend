package com.lectuaria.backend.controller;

import com.lectuaria.backend.dto.book.*;
import com.lectuaria.backend.dto.common.PaginatedResponse;
import com.lectuaria.backend.model.auth.User;
import com.lectuaria.backend.model.auth.UserRole;
import com.lectuaria.backend.repository.auth.UserRepository;
import com.lectuaria.backend.security.JwtService;
import com.lectuaria.backend.service.book.IBookRatingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class BookRatingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IBookRatingService bookRatingService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtService jwtService;

    private void setId(Object entity, Long id) {
        Class<?> clazz = entity.getClass();
        while (clazz != null) {
            try {
                var field = clazz.getDeclaredField("id");
                field.setAccessible(true);
                field.set(entity, id);
                return;
            } catch (NoSuchFieldException | IllegalAccessException e) {
                clazz = clazz.getSuperclass();
            }
        }
        throw new RuntimeException("id field not found in class hierarchy");
    }

    private void mockReaderAuth(String token) {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn("reader@test.com");
        SecurityContextHolder.getContext().setAuthentication(auth);
        when(jwtService.extractEmail(token)).thenReturn("reader@test.com");
    }

    private void mockLibrarianAuth(String token) {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn("lib@test.com");
        SecurityContextHolder.getContext().setAuthentication(auth);
        when(jwtService.extractEmail(token)).thenReturn("lib@test.com");
    }

    // convenience overloads with fixed token
    private void mockReaderAuth() { mockReaderAuth("mock-reader-token"); }
    private void mockLibrarianAuth() { mockLibrarianAuth("mock-lib-token"); }

    // ========== Public endpoints (no auth required) ==========

    @Test
    void getAllBookRatings_returns200() throws Exception {
        when(bookRatingService.getAllBookRatings(10L)).thenReturn(List.of(
            new BookRatingWithUserDTO(1L, 10L, new BigDecimal("5.0"), 1L, "User One", "one@test.com", Instant.now()),
            new BookRatingWithUserDTO(2L, 10L, new BigDecimal("4.0"), 2L, "User Two", "two@test.com", Instant.now())
        ));

        mockMvc.perform(get("/api/books/10/ratings"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].rating").value(5.0))
            .andExpect(jsonPath("$[0].userName").value("User One"));
    }

    @Test
    void getAllBookRatings_empty_returnsEmptyList() throws Exception {
        when(bookRatingService.getAllBookRatings(99L)).thenReturn(List.of());

        mockMvc.perform(get("/api/books/99/ratings"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getPublishedReviews_returns200() throws Exception {
        when(bookRatingService.getPublishedReviews(10L, 0, 5, "MOST_RECENT"))
            .thenReturn(new PaginatedResponse<>(
                List.of(new BookReviewResponseDTO(
                    1L, 10L, 1L, "Reader", Instant.now(), Instant.now(),
                    new BigDecimal("4.5"), "Great book!", "published", 0, false)),
                0, 5, 1, 1, true, true, false, false));

        mockMvc.perform(get("/api/books/10/reviews")
                .queryParam("page", "0")
                .queryParam("size", "5")
                .queryParam("sort", "MOST_RECENT"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pageNumber").value(0))
            .andExpect(jsonPath("$.pageSize").value(5))
            .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void getPublishedReviews_noParams_usesDefaults() throws Exception {
        when(bookRatingService.getPublishedReviews(10L, 0, 5, "MOST_RECENT"))
            .thenReturn(new PaginatedResponse<>(List.of(), 0, 5, 0, 0, true, true, false, false));

        mockMvc.perform(get("/api/books/10/reviews"))
            .andExpect(status().isOk());
    }

    @Test
    void rateBook_noAuth_returns401() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(put("/api/books/10/rating")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "rating": 4.0
                    }
                    """))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void saveReview_noAuth_returns401() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(post("/api/books/10/reviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "rating": 4.5,
                      "reviewText": "Test",
                      "publish": true
                    }
                    """))
            .andExpect(status().isUnauthorized());
    }

    // ========== Authenticated endpoints (mock SecurityContext) ==========

    @Test
    void rateBook_asReader_returns200() throws Exception {
        User readerUser = new User("Reader", "reader@test.com", "hash", UserRole.READER, "reader", null, null);
        setId(readerUser, 1L);
        mockReaderAuth();
        when(userRepository.findByEmail("reader@test.com")).thenReturn(Optional.of(readerUser));
        when(bookRatingService.rateBook(eq(10L), eq(readerUser), any(BigDecimal.class)))
            .thenReturn(new BookRatingResponseDTO(
                "Calificación guardada.", 10L, new BigDecimal("4.5"),
                new BigDecimal("4.25"), 8L, null, null, null));

        mockMvc.perform(put("/api/books/10/rating")
                .header("Authorization", "Bearer mock-reader-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "rating": 4.5
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userRating").value(4.5));
    }

    @Test
    void rateBook_asLibrarian_returns403() throws Exception {
        User librarianUser = new User("Librarian", "lib@test.com", "hash", UserRole.LIBRARIAN, "lib", null, null);
        setId(librarianUser, 2L);
        mockLibrarianAuth();
        when(userRepository.findByEmail("lib@test.com")).thenReturn(Optional.of(librarianUser));

        mockMvc.perform(put("/api/books/10/rating")
                .header("Authorization", "Bearer mock-lib-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "rating": 4.0
                    }
                    """))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void saveReview_asReader_returns200() throws Exception {
        User readerUser = new User("Reader", "reader@test.com", "hash", UserRole.READER, "reader", null, null);
        setId(readerUser, 1L);
        mockReaderAuth();
        when(userRepository.findByEmail("reader@test.com")).thenReturn(Optional.of(readerUser));
        when(bookRatingService.saveReview(eq(10L), eq(readerUser), any(BookReviewUpsertRequestDTO.class)))
            .thenReturn(new BookReviewResponseDTO(
                77L, 10L, 1L, "Reader", Instant.parse("2026-03-23T12:00:00Z"),
                Instant.parse("2026-03-23T12:00:00Z"), new BigDecimal("4.5"),
                "Excelente libro", "published", 0, false));

        mockMvc.perform(post("/api/books/10/reviews")
                .header("Authorization", "Bearer mock-reader-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "rating": 4.5,
                      "reviewText": "Excelente libro",
                      "publish": true
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.reviewId").value(77))
            .andExpect(jsonPath("$.status").value("published"));
    }

    @Test
    void saveReview_asLibrarian_returns403() throws Exception {
        User librarianUser = new User("Librarian", "lib@test.com", "hash", UserRole.LIBRARIAN, "lib", null, null);
        setId(librarianUser, 2L);
        mockLibrarianAuth();
        when(userRepository.findByEmail("lib@test.com")).thenReturn(Optional.of(librarianUser));

        mockMvc.perform(post("/api/books/10/reviews")
                .header("Authorization", "Bearer mock-lib-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "rating": 4.0,
                      "reviewText": "Good book",
                      "publish": true
                    }
                    """))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteReview_asReader_returns204() throws Exception {
        User readerUser = new User("Reader", "reader@test.com", "hash", UserRole.READER, "reader", null, null);
        setId(readerUser, 1L);
        mockReaderAuth();
        when(userRepository.findByEmail("reader@test.com")).thenReturn(Optional.of(readerUser));
        doNothing().when(bookRatingService).deleteReview(77L, readerUser);

        mockMvc.perform(delete("/api/books/reviews/77")
                .header("Authorization", "Bearer mock-reader-token"))
            .andExpect(status().isNoContent());
    }

    @Test
    void deleteRating_asReader_returns204() throws Exception {
        User readerUser = new User("Reader", "reader@test.com", "hash", UserRole.READER, "reader", null, null);
        setId(readerUser, 1L);
        mockReaderAuth();
        when(userRepository.findByEmail("reader@test.com")).thenReturn(Optional.of(readerUser));
        doNothing().when(bookRatingService).deleteRating(55L, readerUser);

        mockMvc.perform(delete("/api/books/ratings/55")
                .header("Authorization", "Bearer mock-reader-token"))
            .andExpect(status().isNoContent());
    }

    @Test
    void updateRating_asReader_returns200() throws Exception {
        User readerUser = new User("Reader", "reader@test.com", "hash", UserRole.READER, "reader", null, null);
        setId(readerUser, 1L);
        mockReaderAuth();
        when(userRepository.findByEmail("reader@test.com")).thenReturn(Optional.of(readerUser));
        when(bookRatingService.updateRating(eq(55L), any(BigDecimal.class), eq(readerUser)))
            .thenReturn(new BookRatingResponseDTO(
                "Calificación actualizada.", 10L, new BigDecimal("4.0"),
                new BigDecimal("4.20"), 10L, null, null, null));

        mockMvc.perform(put("/api/books/ratings/55")
                .header("Authorization", "Bearer mock-reader-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "rating": 4.0
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userRating").value(4.0));
    }

    @Test
    void updateRating_asLibrarian_returns403() throws Exception {
        User librarianUser = new User("Librarian", "lib@test.com", "hash", UserRole.LIBRARIAN, "lib", null, null);
        setId(librarianUser, 2L);
        mockLibrarianAuth();
        when(userRepository.findByEmail("lib@test.com")).thenReturn(Optional.of(librarianUser));

        mockMvc.perform(put("/api/books/ratings/55")
                .header("Authorization", "Bearer mock-lib-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "rating": 3.5
                    }
                    """))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void updateReview_asReader_returns200() throws Exception {
        User readerUser = new User("Reader", "reader@test.com", "hash", UserRole.READER, "reader", null, null);
        setId(readerUser, 1L);
        mockReaderAuth();
        when(userRepository.findByEmail("reader@test.com")).thenReturn(Optional.of(readerUser));
        when(bookRatingService.updateReview(eq(77L), eq(readerUser), any(BookReviewUpsertRequestDTO.class)))
            .thenReturn(new BookReviewResponseDTO(
                77L, 10L, 1L, "Reader", Instant.now(), Instant.now(),
                new BigDecimal("5.0"), "Updated review", "published", 0, false));

        mockMvc.perform(put("/api/books/reviews/77")
                .header("Authorization", "Bearer mock-reader-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "rating": 5.0,
                      "reviewText": "Updated review",
                      "publish": true
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.reviewText").value("Updated review"));
    }

    @Test
    void updateReview_asLibrarian_returns403() throws Exception {
        User librarianUser = new User("Librarian", "lib@test.com", "hash", UserRole.LIBRARIAN, "lib", null, null);
        setId(librarianUser, 2L);
        mockLibrarianAuth();
        when(userRepository.findByEmail("lib@test.com")).thenReturn(Optional.of(librarianUser));

        mockMvc.perform(put("/api/books/reviews/77")
                .header("Authorization", "Bearer mock-lib-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "rating": 4.0,
                      "reviewText": "Updated",
                      "publish": true
                    }
                    """))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void getUserRating_asReader_returns200() throws Exception {
        User readerUser = new User("Reader", "reader@test.com", "hash", UserRole.READER, "reader", null, null);
        setId(readerUser, 1L);
        mockReaderAuth();
        when(userRepository.findByEmail("reader@test.com")).thenReturn(Optional.of(readerUser));
        when(bookRatingService.getBookRating(eq(10L), eq(readerUser)))
            .thenReturn(new BookRatingResponseDTO(
                null, 10L, new BigDecimal("4.0"),
                new BigDecimal("4.25"), 8L, null, null, null));

        mockMvc.perform(get("/api/books/10/rating")
                .header("Authorization", "Bearer mock-reader-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userRating").value(4.0));
    }

    @Test
    void previewReview_asReader_returns200() throws Exception {
        User readerUser = new User("Reader", "reader@test.com", "hash", UserRole.READER, "reader", null, null);
        setId(readerUser, 1L);
        mockReaderAuth();
        when(userRepository.findByEmail("reader@test.com")).thenReturn(Optional.of(readerUser));
        when(bookRatingService.previewReview(eq(10L), any(BookReviewPreviewRequestDTO.class)))
            .thenReturn(new BookReviewPreviewResponseDTO(10L, new BigDecimal("4.5"), "Test review content", 480));

        mockMvc.perform(post("/api/books/10/reviews/preview")
                .header("Authorization", "Bearer mock-reader-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "rating": 4.5,
                      "reviewText": "Test review content",
                      "publish": true
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.reviewText").value("Test review content"));
    }
}