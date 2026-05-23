package com.lectuaria.backend.controller.library;

import com.lectuaria.backend.dto.statistics.LibraryStatisticsDTO;
import com.lectuaria.backend.dto.statistics.GenreCountDTO;
import com.lectuaria.backend.dto.statistics.PopularLibraryBookDTO;
import com.lectuaria.backend.dto.book.BookSummaryDTO;
import com.lectuaria.backend.dto.book.GenreDTO;
import com.lectuaria.backend.dto.library.LibrarySummaryDTO;
import com.lectuaria.backend.model.auth.User;
import com.lectuaria.backend.model.auth.UserRole;
import com.lectuaria.backend.repository.auth.UserRepository;
import com.lectuaria.backend.security.JwtService;
import com.lectuaria.backend.service.library.ILibraryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class LibraryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ILibraryService libraryService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserRepository userRepository;

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

    // ========== GET /api/libraries ==========

    @Test
    void getAllLibraries_returnsListOfLibraries() throws Exception {
        when(libraryService.getAllLibraries()).thenReturn(List.of(
                new LibrarySummaryDTO(1L, "Central Library", "A great library", "123 Main St",
                        "central@test.com", "555-1234", "9-5", null),
                new LibrarySummaryDTO(2L, "Branch Library", "A small branch", "456 Oak Ave",
                        "branch@test.com", "555-5678", "10-6", null)
        ));

        mockMvc.perform(get("/api/libraries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Central Library"))
                .andExpect(jsonPath("$[0].address").value("123 Main St"))
                .andExpect(jsonPath("$[1].name").value("Branch Library"));
    }

    @Test
    void getAllLibraries_returnsEmptyListWhenNoLibraries() throws Exception {
        when(libraryService.getAllLibraries()).thenReturn(List.of());

        mockMvc.perform(get("/api/libraries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ========== GET /api/libraries/me/statistics ==========

    @Test
    void getMyLibraryStatistics_validLibrarian_returns200() throws Exception {
        String token = "valid-librarian-token";
        User librarianUser = new User("Librarian", "lib@test.com", "hash", UserRole.LIBRARIAN, "lib", null, null);
        setId(librarianUser, 1L);

        when(jwtService.extractEmail(token)).thenReturn("lib@test.com");
        when(userRepository.findByEmail("lib@test.com")).thenReturn(Optional.of(librarianUser));
        when(libraryService.getMyLibraryStatistics(librarianUser))
                .thenReturn(new LibraryStatisticsDTO(
                        150L, 10L, List.of(new GenreCountDTO(1L, "Fiction", 45L)),
                        8L, new BigDecimal("4.2"),
                        List.of(
                            new PopularLibraryBookDTO(
                                new BookSummaryDTO(1L, 1234567890123L, "Title",
                                    List.of("Author"),
                                    List.of(new GenreDTO(1L, "Fiction", null)),
                                    new BigDecimal("4.5"), 10,
                                    null, null, null, null, Instant.now()),
                                100L, 5L, 10)),
                        Instant.now(), Instant.now().plusSeconds(3600)));

        mockMvc.perform(get("/api/libraries/me/statistics")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalBooks").value(150))
                .andExpect(jsonPath("$.booksAddedThisMonth").value(10))
                .andExpect(jsonPath("$.averageRatingOfOwnBooks").value(4.2));
    }

    @Test
    void getMyLibraryStatistics_noToken_returns401() throws Exception {
        when(jwtService.extractEmail(any())).thenThrow(new com.lectuaria.backend.exception.UnauthorizedException("Token requerido"));

        mockMvc.perform(get("/api/libraries/me/statistics"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMyLibraryStatistics_invalidToken_returns401() throws Exception {
        when(jwtService.extractEmail("bad-token")).thenThrow(new com.lectuaria.backend.exception.UnauthorizedException("Token inválido"));

        mockMvc.perform(get("/api/libraries/me/statistics")
                .header("Authorization", "Bearer bad-token"))
                .andExpect(status().isUnauthorized());
    }
}