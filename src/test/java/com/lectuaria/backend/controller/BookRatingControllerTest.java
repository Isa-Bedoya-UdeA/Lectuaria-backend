package com.lectuaria.backend.controller;

import com.lectuaria.backend.controller.books.BookRatingController;
import com.lectuaria.backend.dto.book.BookRatingResponseDTO;
import com.lectuaria.backend.dto.book.BookReviewResponseDTO;
import com.lectuaria.backend.dto.common.PaginatedResponse;
import com.lectuaria.backend.model.auth.User;
import com.lectuaria.backend.model.auth.UserRole;
import com.lectuaria.backend.repository.auth.UserRepository;
import com.lectuaria.backend.security.JwtService;
import com.lectuaria.backend.service.book.IBookRatingService;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Objects;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.security.enabled=false",
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration,org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration,org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration"
})
@Import(com.lectuaria.backend.config.TestSecurityConfigForBookRating.class)
class BookRatingControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private IBookRatingService bookRatingService;

        @MockBean
        private UserRepository userRepository;

        @MockBean
        private JwtService jwtService;

        @Test
        void shouldSaveBookRatingSuccessfully() throws Exception {
                String token = "valid-token";
                User user = buildUser();

                when(jwtService.isValid(token)).thenReturn(true);
                when(jwtService.extractEmail(token)).thenReturn("user@example.com");
                when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
                when(bookRatingService.rateBook(eq(10L), eq(Objects.requireNonNull(user)),
                                eq(Objects.requireNonNull(new BigDecimal("4.5")))))
                                .thenReturn(new BookRatingResponseDTO(
                                                "Calificación guardada correctamente.",
                                                10L,
                                                new BigDecimal("4.5"),
                                                new BigDecimal("4.25"),
                                                8L,
                                                null,
                                                null,
                                                null));

                mockMvc.perform(put("/api/books/10/rating")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "rating": 4.5
                                                }
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("Calificación guardada correctamente."))
                                .andExpect(jsonPath("$.bookId").value(10))
                                .andExpect(jsonPath("$.userRating").value(4.5))
                                .andExpect(jsonPath("$.averageRating").value(4.25))
                                .andExpect(jsonPath("$.ratingsCount").value(8));
        }

        @Test
        void shouldRejectRatingOutsideRange() throws Exception {
                String token = "valid-token";

                when(jwtService.isValid(token)).thenReturn(true);

                mockMvc.perform(put("/api/books/10/rating")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "rating": 5.5
                                                }
                                                """))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.errors[0]").value("La calificación máxima es 5.0"));
        }

        @Test
        void shouldGetCurrentBookRating() throws Exception {
                String token = "valid-token";
                User user = buildUser();

                when(jwtService.isValid(token)).thenReturn(true);
                when(jwtService.extractEmail(token)).thenReturn("user@example.com");
                when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
                when(bookRatingService.getBookRating(eq(10L), eq(Objects.requireNonNull(user))))
                                .thenReturn(new BookRatingResponseDTO(
                                                "Calificación actual obtenida correctamente.",
                                                10L,
                                                new BigDecimal("3.5"),
                                                new BigDecimal("4.10"),
                                                12L,
                                                null,
                                                null,
                                                null));

                mockMvc.perform(get("/api/books/10/rating")
                                .header("Authorization", "Bearer " + token))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("Calificación actual obtenida correctamente."))
                                .andExpect(jsonPath("$.userRating").value(3.5))
                                .andExpect(jsonPath("$.averageRating").value(4.10))
                                .andExpect(jsonPath("$.ratingsCount").value(12));
        }

        @Test
        void shouldSavePublishedReviewSuccessfully() throws Exception {
                String token = "valid-token";
                User user = buildUser();

                when(jwtService.extractEmail(token)).thenReturn("user@example.com");
                when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
                when(bookRatingService.saveReview(eq(10L), eq(Objects.requireNonNull(user)),
                                org.mockito.ArgumentMatchers.any()))
                                .thenReturn(new BookReviewResponseDTO(
                                                77L,
                                                10L,
                                                1L,
                                                "Test User",
                                                Instant.parse("2026-03-23T12:00:00Z"),
                                                Instant.parse("2026-03-23T12:00:00Z"),
                                                new BigDecimal("4.5"),
                                                "Reseña publicada",
                                                "published",
                                                0,
                                                false));

                mockMvc.perform(post("/api/books/10/reviews")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  "rating": 4.5,
                                                  "reviewText": "Reseña publicada",
                                                  "publish": true
                                                }
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.reviewId").value(77))
                                .andExpect(jsonPath("$.bookId").value(10))
                                .andExpect(jsonPath("$.status").value("published"));
        }

        @Test
        void shouldGetPublishedReviewsPaginated() throws Exception {
                when(bookRatingService.getPublishedReviews(10L, 0, 5))
                                .thenReturn(new PaginatedResponse<>(
                                                List.of(),
                                                0,
                                                5,
                                                0,
                                                0,
                                                true,
                                                true,
                                                false,
                                                false));

                mockMvc.perform(get("/api/books/10/reviews")
                                .queryParam("page", "0")
                                .queryParam("size", "5"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.pageNumber").value(0))
                                .andExpect(jsonPath("$.pageSize").value(5))
                                .andExpect(jsonPath("$.totalElements").value(0));
        }

        private User buildUser() {
                return new User(
                                "Test User",
                                "user@example.com",
                                "hash",
                                UserRole.NORMAL,
                                "testuser",
                                null,
                                null);
        }
}
