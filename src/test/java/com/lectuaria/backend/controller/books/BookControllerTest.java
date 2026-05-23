package com.lectuaria.backend.controller.books;

import com.lectuaria.backend.dto.book.*;
import com.lectuaria.backend.dto.common.PaginatedResponse;
import com.lectuaria.backend.model.auth.User;
import com.lectuaria.backend.model.auth.UserRole;
import com.lectuaria.backend.model.library.Library;
import com.lectuaria.backend.model.library.Librarian;
import com.lectuaria.backend.repository.auth.UserRepository;
import com.lectuaria.backend.repository.library.LibrarianRepository;
import com.lectuaria.backend.service.book.IBookService;
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
import org.springframework.security.test.context.support.WithMockUser;
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
class BookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IBookService bookService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private LibrarianRepository librarianRepository;

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
        librarianEntity.setUser(librarianUser);
        librarianEntity.setLibrary(library);

        SecurityContextHolder.clearContext();
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

    private org.springframework.test.web.servlet.request.RequestPostProcessor postProcessor(String email, UserRole role) {
        return request -> {
            List<SimpleGrantedAuthority> auths = List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(email, null, auths);
            SecurityContext ctx = SecurityContextHolder.createEmptyContext();
            ctx.setAuthentication(auth);
            SecurityContextHolder.setContext(ctx);
            return request;
        };
    }

    private BookSummaryDTO summary(Long id, String title) {
        return new BookSummaryDTO(
                id, 978L, title,
                List.of("Author One"),
                List.of(new GenreDTO(1L, "Fiction", null)),
                new BigDecimal("4.0"),
                10,
                "http://cover.url",
                1L, 1L, 1L,
                Instant.now()
        );
    }

    private PaginatedResponse<BookSummaryDTO> emptyPage() {
        return new PaginatedResponse<>(List.of(), 0, 12, 0, 0, true, true, false, false);
    }

    private PaginatedResponse<BookSummaryDTO> pageOf(List<BookSummaryDTO> items) {
        return new PaginatedResponse<>(items, 0, 12, items.size(), 1, true, true, false, false);
    }

    // ========== PUBLIC ENDPOINTS ==========

    @Nested
    class PublicEndpoints {

        @Test
        void getAllBooks_returnsOkWithPagination() throws Exception {
            List<BookSummaryDTO> books = List.of(summary(1L, "Book One"), summary(2L, "Book Two"));
            when(bookService.getAllBooks(anyInt(), anyInt(), any(), any(), any(), any(), any()))
                    .thenReturn(pageOf(books));

            mockMvc.perform(get("/api/books"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(2))
                    .andExpect(jsonPath("$.content[0].title").value("Book One"))
                    .andExpect(jsonPath("$.pageNumber").value(0))
                    .andExpect(jsonPath("$.totalElements").value(2));
        }

        @Test
        void getAllBooks_withFilters_passesParamsToService() throws Exception {
            when(bookService.getAllBooks(eq(1), eq(24), eq(4.0f), eq(2020), eq(2024), any(), any()))
                    .thenReturn(emptyPage());

            mockMvc.perform(get("/api/books")
                            .param("page", "1")
                            .param("size", "24")
                            .param("minRating", "4.0")
                            .param("startYear", "2020")
                            .param("endYear", "2024")
                            .param("formatTypes", "pdf"))
                    .andExpect(status().isOk());
        }

        @Test
        void searchBooks_withKeywords_returnsFilteredResults() throws Exception {
            List<BookSummaryDTO> results = List.of(summary(5L, "Clean Code"));
            when(bookService.searchBooksByMultipleFilters(any(), anyInt(), anyInt(), any()))
                    .thenReturn(pageOf(results));

            mockMvc.perform(get("/api/books/search")
                            .param("keywords", "clean code"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].title").value("Clean Code"));
        }

        @Test
        void searchBooks_withGenresAndYearRange_filtersCorrectly() throws Exception {
            when(bookService.searchBooksByMultipleFilters(any(), anyInt(), anyInt(), any()))
                    .thenReturn(emptyPage());

            mockMvc.perform(get("/api/books/search")
                            .param("genreIds", "1", "2")
                            .param("minYear", "2020")
                            .param("maxYear", "2024")
                            .param("minRating", "4.5"))
                    .andExpect(status().isOk());
        }

        @Test
        void searchBooks_withLibraryIds_passesToService() throws Exception {
            when(bookService.searchBooksByMultipleFilters(any(), anyInt(), anyInt(), any()))
                    .thenReturn(emptyPage());

            mockMvc.perform(get("/api/books/search")
                            .param("libraryIds", "1", "3")
                            .param("formatTypes", "pdf", "epub"))
                    .andExpect(status().isOk());
        }

        @Test
        void getBooksByGenre_returnsPaginatedBooks() throws Exception {
            when(bookService.getBooksByGenre(eq(1L), anyInt(), anyInt()))
                    .thenReturn(pageOf(List.of(summary(7L, "Sci-Fi Book"))));

            mockMvc.perform(get("/api/books/genre/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].title").value("Sci-Fi Book"));
        }

        @Test
        void getBooksByGenre_withPagination_passesParams() throws Exception {
            when(bookService.getBooksByGenre(eq(2L), eq(2), eq(24)))
                    .thenReturn(emptyPage());

            mockMvc.perform(get("/api/books/genre/2")
                            .param("page", "2")
                            .param("size", "24"))
                    .andExpect(status().isOk());
        }

        // Note: /api/books/filter/availability matches /api/books/** in security config
        // so it requires authentication per the catch-all rule
        @Test
        void getBooksByFormatAvailability_authenticated_returnsOk() throws Exception {
            withUser("lib@test.com", librarianUser, UserRole.LIBRARIAN);
            when(userRepository.findByEmail("lib@test.com")).thenReturn(Optional.of(librarianUser));
            when(bookService.getBooksByFormatAvailability(eq("pdf"), anyInt(), anyInt(), any()))
                    .thenReturn(emptyPage());

            mockMvc.perform(get("/api/books/filter/availability")
                            .param("format", "pdf"))
                    .andExpect(status().isOk());
        }

        @Test
        void getBooksByGenres_withMultipleGenres_returnsResults() throws Exception {
            when(bookService.getBooksByGenres(eq(List.of(1L, 3L)), anyInt(), anyInt()))
                    .thenReturn(emptyPage());

            mockMvc.perform(get("/api/books/genres")
                            .param("genreIds", "1", "3"))
                    .andExpect(status().isOk());
        }

        @Test
        void getBooksByAuthor_returnsPaginatedBooks() throws Exception {
            when(bookService.getBooksByAuthor(eq(4L), anyInt(), anyInt()))
                    .thenReturn(emptyPage());

            mockMvc.perform(get("/api/books/author/4"))
                    .andExpect(status().isOk());
        }

        @Test
        void getMostPopular_returnsPaginatedBooks() throws Exception {
            when(bookService.getMostPopular(anyInt(), anyInt()))
                    .thenReturn(pageOf(List.of(summary(9L, "Popular Book"))));

            mockMvc.perform(get("/api/books/popular"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].title").value("Popular Book"));
        }

        @Test
        void getTopRated_withGenreAndYear_filtersCorrectly() throws Exception {
            when(bookService.getTopRated(anyInt(), anyInt(), eq(1L), eq(2023)))
                    .thenReturn(emptyPage());

            mockMvc.perform(get("/api/books/top-rated")
                            .param("genreId", "1")
                            .param("year", "2023"))
                    .andExpect(status().isOk());
        }

        @Test
        void getTopRated_withoutFilters_returnsAll() throws Exception {
            when(bookService.getTopRated(anyInt(), anyInt(), isNull(), isNull()))
                    .thenReturn(emptyPage());

            mockMvc.perform(get("/api/books/top-rated"))
                    .andExpect(status().isOk());
        }

        @Test
        void getNewCatalogBooks_returnsPaginatedCatalog() throws Exception {
            BookCatalogItemDTO item = new BookCatalogItemDTO(summary(11L, "New Book"), Instant.now());
            PaginatedResponse<BookCatalogItemDTO> page = new PaginatedResponse<>(
                    List.of(item), 0, 12, 1, 1, true, true, false, false);

            when(bookService.getNewCatalogBooks(anyInt(), anyInt(), any())).thenReturn(page);

            mockMvc.perform(get("/api/books/new-catalog"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].book.title").value("New Book"));
        }

        @Test
        void getNewCatalogBooks_withGenreFilter_passesFilter() throws Exception {
            PaginatedResponse<BookCatalogItemDTO> empty = new PaginatedResponse<>(
                    List.of(), 0, 12, 0, 0, true, true, false, false);
            when(bookService.getNewCatalogBooks(anyInt(), anyInt(), eq(5L))).thenReturn(empty);

            mockMvc.perform(get("/api/books/new-catalog").param("genreId", "5"))
                    .andExpect(status().isOk());
        }

        @Test
        void getFeaturedSections_returnsSections() throws Exception {
            List<BookSummaryDTO> mostRead = List.of(summary(20L, "Most Read Book"));
            List<BookSummaryDTO> topRated = List.of(summary(21L, "Top Rated Book"));
            FeaturedSectionsDTO featured = new FeaturedSectionsDTO(mostRead, topRated, Instant.now().plusSeconds(3600));
            when(bookService.getFeaturedSections()).thenReturn(featured);

            mockMvc.perform(get("/api/books/featured"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.mostReadThisMonth.length()").value(1))
                    .andExpect(jsonPath("$.topRated.length()").value(1))
                    .andExpect(jsonPath("$.mostReadThisMonth[0].title").value("Most Read Book"))
                    .andExpect(jsonPath("$.topRated[0].title").value("Top Rated Book"));
        }

        @Test
        void getBookById_returnsBookDetail() throws Exception {
            BookDetailDTO detail = new BookDetailDTO(
                    5L, "Detailed Book", List.of("Author"),
                    List.of(new GenreDTO(1L, "Fiction", null)),
                    new BigDecimal("4.5"), 20,
                    "http://cover.url", "A great book.",
                    List.of("Publisher A"),
                    LocalDate.of(2022, 5, 15),
                    300, 9781234567890L,
                    List.of("pdf", "epub")
            );
            when(bookService.getBookById(5L)).thenReturn(detail);

            mockMvc.perform(get("/api/books/5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Detailed Book"))
                    .andExpect(jsonPath("$.isbn").value(9781234567890L))
                    .andExpect(jsonPath("$.ratingsCount").value(20))
                    .andExpect(jsonPath("$.pages").value(300));
        }

        @Test
        void getSimilarBooks_returnsListOfSimilar() throws Exception {
            when(bookService.getSimilarBooks(5L))
                    .thenReturn(List.of(summary(30L, "Similar One"), summary(31L, "Similar Two")));

            mockMvc.perform(get("/api/books/5/similar"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].title").value("Similar One"));
        }

        // Note: /api/books/{id}/share-link matches /api/books/*/share-link which requires auth
        @Test
        void getBookShareLink_authenticated_returnsShareUrl() throws Exception {
            withUser("reader@test.com", readerUser, UserRole.READER);
            when(userRepository.findByEmail("reader@test.com")).thenReturn(Optional.of(readerUser));

            mockMvc.perform(get("/api/books/7/share-link"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.url").value("http://localhost:3000/books/7"))
                    .andExpect(jsonPath("$.type").value("book"));
        }

        @Test
        void getBookByIsbn_returnsBookDetail() throws Exception {
            BookDetailDTO detail = new BookDetailDTO(
                    12L, "ISBN Book", List.of("Author"),
                    List.of(), null, 0,
                    null, null, null,
                    null, null, 9781234567890L, null);
            when(bookService.getBookByIsbn(9781234567890L)).thenReturn(detail);

            mockMvc.perform(get("/api/books/isbn/9781234567890"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("ISBN Book"))
                    .andExpect(jsonPath("$.isbn").value(9781234567890L));
        }
    }

    // ========== AUTHENTICATION GATED ENDPOINTS ==========

    @Nested
    class AuthGatedEndpoints {

        @Test
        void getBooksByLibrary_unauthenticated_returns401() throws Exception {
            mockMvc.perform(get("/api/books/library/1"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(username = "reader@test.com", roles = {"READER"})
        void getBooksByLibrary_nonLibrarian_returns403() throws Exception {
            when(userRepository.findByEmail("reader@test.com")).thenReturn(Optional.of(readerUser));

            mockMvc.perform(get("/api/books/library/1"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void getBooksByLibrary_librarian_wrongLibrary_returns403() throws Exception {
            withUser("lib@test.com", librarianUser, UserRole.LIBRARIAN);
            when(userRepository.findByEmail("lib@test.com")).thenReturn(Optional.of(librarianUser));

            Library otherLib = new Library();
            setId(otherLib, 5L);
            Librarian otherLibEntity = new Librarian();
            otherLibEntity.setUser(librarianUser);
            otherLibEntity.setLibrary(otherLib);
            when(librarianRepository.findByUser(librarianUser)).thenReturn(Optional.of(otherLibEntity));

            mockMvc.perform(get("/api/books/library/1"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void removeBookFromLibrary_unauthenticated_returns401() throws Exception {
            mockMvc.perform(delete("/api/books/5/library"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void deleteBook_unauthenticated_returns401() throws Exception {
            mockMvc.perform(delete("/api/books/5"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void updateBook_unauthenticated_returns401() throws Exception {
            String body = """
                {
                  "isbn": 9781234567890,
                  "title": "Updated Title",
                  "authors": ["Author"],
                  "availability": {"isPhysical": true, "isDigital": false, "copies": 5},
                  "libraryId": 1
                }
                """;
            mockMvc.perform(put("/api/books/5")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void updateBook_librarian_returnsOk() throws Exception {
            withUser("lib@test.com", librarianUser, UserRole.LIBRARIAN);
            when(userRepository.findByEmail("lib@test.com")).thenReturn(Optional.of(librarianUser));
            when(librarianRepository.findByUser(librarianUser)).thenReturn(Optional.of(librarianEntity));
            when(bookService.updateBook(eq(5L), any(), eq(librarianUser.getId())))
                    .thenReturn(new BookDetailDTO(5L, "Updated", List.of(), List.of(), null, 0, null, null, null, null, null, 978L, null));

            String body = """
                {
                  "isbn": 9781234567890,
                  "title": "Updated Title",
                  "authors": ["Author One"],
                  "availability": {"isPhysical": true, "isDigital": false, "copies": 5},
                  "libraryId": 1
                }
                """;
            mockMvc.perform(put("/api/books/5")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Updated"));
        }
    }
}

        