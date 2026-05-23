package com.lectuaria.backend.controller.books;

import com.lectuaria.backend.dto.book.*;
import com.lectuaria.backend.exception.UnauthorizedException;
import com.lectuaria.backend.model.auth.User;
import com.lectuaria.backend.model.auth.UserRole;
import com.lectuaria.backend.model.library.Library;
import com.lectuaria.backend.model.library.Librarian;
import com.lectuaria.backend.repository.auth.UserRepository;
import com.lectuaria.backend.security.JwtService;
import com.lectuaria.backend.service.book.IBookPublishService;
import com.lectuaria.backend.service.storage.S3StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BookPublishControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IBookPublishService bookPublishService;

    @MockBean
    private S3StorageService s3StorageService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtService jwtService;

    private User librarianUser;
    private User readerUser;
    private Library library;
    private Librarian librarianEntity;

    @BeforeEach
    void setUp() {
        librarianUser = new User("Librarian", "lib@test.com", "hash", UserRole.LIBRARIAN, "librarian", null, null);
        setId(librarianUser, 10L);

        readerUser = new User("Reader", "reader@test.com", "hash", UserRole.READER, "reader", null, null);
        setId(readerUser, 20L);

        library = new Library();
        setId(library, 1L);
        library.setName("Test Library");

        librarianEntity = new Librarian();
        setId(librarianEntity, 5L);
        librarianEntity.setUser(librarianUser);
        librarianEntity.setLibrary(library);

        SecurityContextHolder.clearContext();

        // Mock JWT service so real token parsing doesn't throw
        when(jwtService.extractEmail(anyString())).thenReturn("lib@test.com");
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

    // ========== POST /api/books/publish ==========

    @Nested
    class PublishBook {

        @Test
        void publishBook_authenticated_returnsOk() throws Exception {
            withUser("lib@test.com", librarianUser, UserRole.LIBRARIAN);
            when(userRepository.findByEmail("lib@test.com")).thenReturn(Optional.of(librarianUser));

            BookPublishResponseDTO response = new BookPublishResponseDTO(5L, "Test Book", 9781234567890L, true,
                    "Libro creado y añadido a tu biblioteca exitosamente.");
            when(bookPublishService.publishBook(any(BookPublishRequestDTO.class), eq(10L))).thenReturn(response);

            String body = """
                {
                  "isbn": 9781234567890,
                  "title": "Test Book",
                  "authors": ["Author One"],
                  "availability": {"physical": true, "digital": false, "physicalCopies": 5},
                  "libraryId": 1
                }
                """;

            mockMvc.perform(post("/api/books/publish")
                            .header("Authorization", "Bearer any.token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.bookId").value(5))
                    .andExpect(jsonPath("$.title").value("Test Book"))
                    .andExpect(jsonPath("$.isbn").value(9781234567890L))
                    .andExpect(jsonPath("$.newBook").value(true))
                    .andExpect(jsonPath("$.message").value("Libro creado y añadido a tu biblioteca exitosamente."));
        }

        @Test
        void publishBook_unauthenticated_returns401() throws Exception {
            String body = """
                {
                  "isbn": 9781234567890,
                  "title": "Test Book",
                  "authors": ["Author One"],
                  "availability": {"physical": true, "digital": false},
                  "libraryId": 1
                }
                """;

            mockMvc.perform(post("/api/books/publish")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void publishBook_readerRole_returns401() throws Exception {
            withUser("reader@test.com", readerUser, UserRole.READER);
            when(userRepository.findByEmail("reader@test.com")).thenReturn(Optional.of(readerUser));

            String body = """
                {
                  "isbn": 9781234567890,
                  "title": "Test",
                  "authors": ["Author"],
                  "availability": {"physical": true, "digital": false},
                  "libraryId": 1
                }
                """;

            mockMvc.perform(post("/api/books/publish")
                            .header("Authorization", "Bearer any.token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void publishBook_missingRequiredFields_returnsBadRequest() throws Exception {
            withUser("lib@test.com", librarianUser, UserRole.LIBRARIAN);
            when(userRepository.findByEmail("lib@test.com")).thenReturn(Optional.of(librarianUser));

            // Missing: isbn, title, authors, availability, libraryId
            String body = "{}";

            mockMvc.perform(post("/api/books/publish")
                            .header("Authorization", "Bearer any.token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void publishBook_invalidIsbn_returns500() throws Exception {
            withUser("lib@test.com", librarianUser, UserRole.LIBRARIAN);
            when(userRepository.findByEmail("lib@test.com")).thenReturn(Optional.of(librarianUser));
            when(bookPublishService.publishBook(any(), eq(10L)))
                    .thenThrow(new RuntimeException("ISBN inválido"));

            String body = """
                {
                  "isbn": 123,
                  "title": "Test",
                  "authors": ["Author"],
                  "availability": {"physical": true, "digital": false},
                  "libraryId": 1
                }
                """;

            mockMvc.perform(post("/api/books/publish")
                            .header("Authorization", "Bearer any.token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().is5xxServerError());
        }
    }

    // ========== POST /api/books/publish-with-cover ==========

    @Nested
    class PublishBookWithCover {

        @Test
        void publishWithCover_normalUrl_returnsOk() throws Exception {
            withUser("lib@test.com", librarianUser, UserRole.LIBRARIAN);
            when(userRepository.findByEmail("lib@test.com")).thenReturn(Optional.of(librarianUser));

            BookPublishResponseDTO response = new BookPublishResponseDTO(7L, "Cover Book", 9780000000001L, true,
                    "Libro creado.");
            when(bookPublishService.publishBook(any(BookPublishRequestDTO.class), eq(10L))).thenReturn(response);

            String body = """
                {
                  "isbn": 9780000000001,
                  "title": "Cover Book",
                  "authors": ["Author"],
                  "coverUrl": "http://example.com/cover.jpg",
                  "availability": {"physical": true, "digital": false},
                  "libraryId": 1
                }
                """;

            mockMvc.perform(post("/api/books/publish-with-cover")
                            .header("Authorization", "Bearer any.token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.bookId").value(7));

            verify(s3StorageService, never()).uploadCoverBytes(any(), anyLong(), any());
        }

        @Test
        void publishWithCover_withBase64Image_uploadsAndReturnsOk() throws Exception {
            withUser("lib@test.com", librarianUser, UserRole.LIBRARIAN);
            when(userRepository.findByEmail("lib@test.com")).thenReturn(Optional.of(librarianUser));
            when(s3StorageService.uploadCoverBytes(any(byte[].class), eq(9780000000002L), eq("image/png")))
                    .thenReturn("https://storage.example.com/covers/9780000000002.png");

            BookPublishResponseDTO response = new BookPublishResponseDTO(8L, "With Base64 Cover", 9780000000002L, true,
                    "Libro creado.");
            when(bookPublishService.publishBook(any(BookPublishRequestDTO.class), eq(10L))).thenReturn(response);

            String base64Image = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8DwHwAFBQIAX8jx0gAAAABJRU5ErkJggg==";

            String body = String.format("""
                {
                  "isbn": 9780000000002,
                  "title": "With Base64 Cover",
                  "authors": ["Author"],
                  "coverUrl": "%s",
                  "availability": {"physical": false, "digital": true},
                  "libraryId": 1
                }
                """, base64Image);

            mockMvc.perform(post("/api/books/publish-with-cover")
                            .header("Authorization", "Bearer any.token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.bookId").value(8));

            verify(s3StorageService).uploadCoverBytes(any(byte[].class), eq(9780000000002L), eq("image/png"));
        }

        @Test
        void publishWithCover_s3UploadFails_returns500() throws Exception {
            withUser("lib@test.com", librarianUser, UserRole.LIBRARIAN);
            when(userRepository.findByEmail("lib@test.com")).thenReturn(Optional.of(librarianUser));
            when(s3StorageService.uploadCoverBytes(any(byte[].class), anyLong(), any()))
                    .thenThrow(new java.io.IOException("S3 connection failed"));

            String base64Image = "data:image/jpeg;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8DwHwAFBQIAX8jx0gAAAABJRU5ErkJggg==";

            String body = String.format("""
                {
                  "isbn": 9780000000003,
                  "title": "Failing Cover",
                  "authors": ["Author"],
                  "coverUrl": "%s",
                  "availability": {"physical": false, "digital": true},
                  "libraryId": 1
                }
                """, base64Image);

            mockMvc.perform(post("/api/books/publish-with-cover")
                            .header("Authorization", "Bearer any.token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().is5xxServerError());
        }

        @Test
        void publishWithCover_unauthenticated_returns401() throws Exception {
            String body = """
                {
                  "isbn": 9780000000004,
                  "title": "No Auth",
                  "authors": ["Author"],
                  "availability": {"physical": true, "digital": false},
                  "libraryId": 1
                }
                """;

            mockMvc.perform(post("/api/books/publish-with-cover")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ========== GET /api/books/prefill/{isbn} ==========

    @Nested
    class PrefillFromOpenLibrary {

        @Test
        void prefill_authenticated_returnsRequest() throws Exception {
            withUser("lib@test.com", librarianUser, UserRole.LIBRARIAN);
            when(userRepository.findByEmail("lib@test.com")).thenReturn(Optional.of(librarianUser));

            BookPublishRequestDTO prefill = new BookPublishRequestDTO();
            prefill.setIsbn(9780000000005L);
            prefill.setTitle("Prefill Book Title");
            prefill.setAuthors(List.of("Known Author"));
            prefill.setDescription("A great book.");
            prefill.setCoverUrl("http://example.com/prefill-cover.jpg");
            prefill.setBookExistsInCatalog(true);
            prefill.setBookExistsInUserLibrary(false);
            prefill.setGenres(List.of("Fiction"));
            prefill.setPublishers(List.of("Publisher A"));

            when(bookPublishService.prefillFromOpenLibrary(eq(9780000000005L), eq(10L))).thenReturn(prefill);

            mockMvc.perform(get("/api/books/prefill/9780000000005")
                            .header("Authorization", "Bearer any.token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isbn").value(9780000000005L))
                    .andExpect(jsonPath("$.title").value("Prefill Book Title"))
                    .andExpect(jsonPath("$.authors[0]").value("Known Author"))
                    .andExpect(jsonPath("$.description").value("A great book."))
                    .andExpect(jsonPath("$.bookExistsInCatalog").value(true))
                    .andExpect(jsonPath("$.bookExistsInUserLibrary").value(false));
        }

        @Test
        void prefill_bookNotFound_returnsEmptyRequest() throws Exception {
            withUser("lib@test.com", librarianUser, UserRole.LIBRARIAN);
            when(userRepository.findByEmail("lib@test.com")).thenReturn(Optional.of(librarianUser));

            BookPublishRequestDTO empty = new BookPublishRequestDTO();
            empty.setIsbn(9999999999999L);
            empty.setBookExistsInCatalog(false);
            empty.setAuthors(List.of());
            empty.setPublishers(List.of());
            empty.setGenres(List.of());

            when(bookPublishService.prefillFromOpenLibrary(eq(9999999999999L), eq(10L))).thenReturn(empty);

            mockMvc.perform(get("/api/books/prefill/9999999999999")
                            .header("Authorization", "Bearer any.token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isbn").value(9999999999999L))
                    .andExpect(jsonPath("$.bookExistsInCatalog").value(false));
        }

        @Test
        void prefill_unauthenticated_returns401() throws Exception {
            mockMvc.perform(get("/api/books/prefill/9780000000005"))
                    .andExpect(status().isUnauthorized());
        }
    }
}