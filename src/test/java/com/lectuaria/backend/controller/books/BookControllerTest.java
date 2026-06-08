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

    @MockBean
    private com.lectuaria.backend.security.AuthenticatedUserResolver authenticatedUserResolver;

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

        when(authenticatedUserResolver.requireCurrentUser(any(jakarta.servlet.http.HttpServletRequest.class)))
                .thenReturn(librarianUser);
        when(authenticatedUserResolver.requireCurrentUserId()).thenReturn(20L);

        SecurityContextHolder.clearContext();
    }

    private void setId(Object entity, Long id) {
        try {
            var field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (NoSuchFieldException e) {
            try {
                var field = entity.getClass().getSuperclass().getDeclaredField("id");
                field.setAccessible(true);
                field.set(entity, id);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
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
                    .andExpect(jsonPath("$._embedded.bookSummaryDTOList.length()").value(2))
                    .andExpect(jsonPath("$._embedded.bookSummaryDTOList[0].title").value("Book One"))
                    .andExpect(jsonPath("$.page.number").value(0))
                    .andExpect(jsonPath("$.page.totalElements").value(2));
        }

        @Test
        void getBooksByGenre_returnsPaginatedBooks() throws Exception {
            List<BookSummaryDTO> books = List.of(summary(1L, "Genre Book"));
            when(bookService.getBooksByGenre(anyLong(), anyInt(), anyInt())).thenReturn(pageOf(books));

            mockMvc.perform(get("/api/books/genre/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.bookSummaryDTOList.length()").value(1))
                    .andExpect(jsonPath("$._embedded.bookSummaryDTOList[0].title").value("Genre Book"));
        }

        @Test
        void searchBooks_withKeywords_returnsFilteredResults() throws Exception {
            List<BookSummaryDTO> books = List.of(summary(1L, "Clean Code"));
            when(bookService.searchBooksByMultipleFilters(any(), anyInt(), anyInt(), any()))
                    .thenReturn(pageOf(books));

            mockMvc.perform(get("/api/books/search")
                            .param("keywords", "code")
                            .param("page", "0").param("size", "12"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.bookSummaryDTOList.length()").value(1))
                    .andExpect(jsonPath("$._embedded.bookSummaryDTOList[0].title").value("Clean Code"));
        }

        @Test
        void getMostPopular_returnsPaginatedBooks() throws Exception {
            List<BookSummaryDTO> books = List.of(summary(1L, "Popular Book"));
            when(bookService.getMostPopular(anyInt(), anyInt())).thenReturn(pageOf(books));

            mockMvc.perform(get("/api/books/popular"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.bookSummaryDTOList[0].title").value("Popular Book"));
        }

        @Test
        void getTopRated_returnsPaginatedBooks() throws Exception {
            List<BookSummaryDTO> books = List.of(summary(1L, "Top Book"));
            when(bookService.getTopRated(anyInt(), anyInt(), any(), any())).thenReturn(pageOf(books));

            mockMvc.perform(get("/api/books/top-rated"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.bookSummaryDTOList[0].title").value("Top Book"));
        }

        @Test
        void getNewCatalogBooks_returnsPaginatedCatalog() throws Exception {
            BookSummaryDTO summary = new BookSummaryDTO(
                    1L, 978L, "New Book", List.of("Author"),
                    List.of(new GenreDTO(1L, "Fiction", null)),
                    new java.math.BigDecimal("4.0"), 5, "http://cover.url",
                    1L, 1L, 1L, Instant.now());
            BookCatalogItemDTO item = new BookCatalogItemDTO(summary, Instant.now());
            PaginatedResponse<BookCatalogItemDTO> page =
                    new PaginatedResponse<>(List.of(item), 0, 12, 1, 1, true, true, false, false);
            when(bookService.getNewCatalogBooks(anyInt(), anyInt(), any())).thenReturn(page);

            mockMvc.perform(get("/api/books/new-catalog"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.bookCatalogItemDTOList.length()").value(1))
                    .andExpect(jsonPath("$._embedded.bookCatalogItemDTOList[0].book.title").value("New Book"));
        }

        @Test
        void getFeaturedSections_returnsOk() throws Exception {
            FeaturedSectionsDTO sections = new FeaturedSectionsDTO(
                    List.of(summary(1L, "Most Read Book")),
                    List.of(summary(2L, "Top Rated Book")),
                    java.time.Instant.now());
            when(bookService.getFeaturedSections()).thenReturn(sections);

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
                    List.of(), new java.math.BigDecimal("4.5"), 20, "http://cover.url",
                    "Test description", List.of("Publisher"), java.time.LocalDate.of(2020, 1, 1),
                    300, 9781234567890L, List.of("PDF", "EPUB"));
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
            List<BookSummaryDTO> similar = List.of(summary(30L, "Similar One"), summary(31L, "Similar Two"));
            when(bookService.getSimilarBooks(5L)).thenReturn(similar);

            mockMvc.perform(get("/api/books/5/similar"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.bookSummaryDTOList.length()").value(2))
                    .andExpect(jsonPath("$._embedded.bookSummaryDTOList[0].title").value("Similar One"));
        }

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
            when(authenticatedUserResolver.requireCurrentUser(any(jakarta.servlet.http.HttpServletRequest.class)))
                    .thenReturn(readerUser);

            mockMvc.perform(get("/api/books/library/1"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void getBooksByLibrary_librarian_wrongLibrary_returns403() throws Exception {
            withUser("lib@test.com", librarianUser, UserRole.LIBRARIAN);
            when(userRepository.findByEmail("lib@test.com")).thenReturn(Optional.of(librarianUser));
            when(authenticatedUserResolver.requireCurrentUser(any(jakarta.servlet.http.HttpServletRequest.class)))
                    .thenReturn(librarianUser);

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
            when(authenticatedUserResolver.requireCurrentUser(any(jakarta.servlet.http.HttpServletRequest.class)))
                    .thenReturn(librarianUser);
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
