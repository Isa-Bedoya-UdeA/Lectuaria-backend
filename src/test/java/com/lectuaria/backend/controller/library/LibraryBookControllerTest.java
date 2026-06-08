package com.lectuaria.backend.controller.library;

import com.lectuaria.backend.dto.book.BulkUploadResultDTO;
import com.lectuaria.backend.dto.library.LibraryBookAvailabilityDTO;
import com.lectuaria.backend.model.auth.User;
import com.lectuaria.backend.model.auth.UserRole;
import com.lectuaria.backend.model.book.LibraryBook;
import com.lectuaria.backend.model.library.Library;
import com.lectuaria.backend.model.library.Librarian;
import com.lectuaria.backend.repository.auth.UserRepository;
import com.lectuaria.backend.repository.library.LibrarianRepository;
import com.lectuaria.backend.repository.library.LibraryBookRepository;
import com.lectuaria.backend.security.JwtService;
import com.lectuaria.backend.service.book.IBulkUploadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LibraryBookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private IBulkUploadService bulkUploadService;

    @MockBean
    private LibraryBookRepository libraryBookRepository;

    @MockBean
    private LibrarianRepository librarianRepository;

    @MockBean
    private com.lectuaria.backend.security.AuthenticatedUserResolver authenticatedUserResolver;

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

    private void mockSecurityContext(User user) {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn(user.getEmail());
        SecurityContextHolder.getContext().setAuthentication(auth);
        when(authenticatedUserResolver.requireCurrentUser(any(jakarta.servlet.http.HttpServletRequest.class)))
                .thenReturn(user);
    }

    @BeforeEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    // ========== GET /api/library-books/template ==========

    @Test
    void downloadTemplate_returnsCsvFile() throws Exception {
        mockMvc.perform(get("/api/library-books/template"))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    byte[] body = result.getResponse().getContentAsByteArray();
                    String csv = new String(body);
                    org.junit.jupiter.api.Assertions.assertTrue(csv.startsWith("ISBN,Titulo,Autores"),
                            "CSV should start with header row");
                    org.junit.jupiter.api.Assertions.assertTrue(csv.contains("Los Ojos del Perro Siberiano"));
                });
    }

    // ========== POST /api/library-books/bulk-upload ==========

    @Test
    void bulkUpload_librarianRole_returns200() throws Exception {
        User librarianUser = new User("Librarian", "lib@test.com", "hash", UserRole.LIBRARIAN, "lib", null, null);
        setId(librarianUser, 1L);
        mockSecurityContext(librarianUser);

        when(userRepository.findByEmail("lib@test.com")).thenReturn(Optional.of(librarianUser));
        when(bulkUploadService.processCsv(any(), eq(1L))).thenAnswer(inv -> {
            BulkUploadResultDTO result = new BulkUploadResultDTO();
            result.setTotalProcessed(3);
            result.setSuccessCount(2);
            result.setErrorCount(1);
            return result;
        });

        MockMultipartFile file = new MockMultipartFile(
                "file", "books.csv", "text/csv",
                "ISBN,Titulo,Autores\n9781234567890,Test Book,Author".getBytes());

        mockMvc.perform(multipart("/api/library-books/bulk-upload")
                .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProcessed").value(3))
                .andExpect(jsonPath("$.successCount").value(2))
                .andExpect(jsonPath("$.errorCount").value(1));
    }

    @Test
    void bulkUpload_readerRole_returns401() throws Exception {
        String token = "valid-reader-token";
        User readerUser = new User("Reader", "reader@test.com", "hash", UserRole.READER, "reader", null, null);

        when(jwtService.isValid(token)).thenReturn(true);
        when(jwtService.extractEmail(token)).thenReturn("reader@test.com");
        when(userRepository.findByEmail("reader@test.com")).thenReturn(Optional.of(readerUser));
        mockSecurityContext(readerUser);

        MockMultipartFile file = new MockMultipartFile(
                "file", "books.csv", "text/csv",
                "ISBN,Titulo,Autores\n9781234567890,Test Book,Author".getBytes());

        mockMvc.perform(multipart("/api/library-books/bulk-upload")
                .file(file)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void bulkUpload_noAuth_returns401() throws Exception {
        SecurityContextHolder.clearContext();

        MockMultipartFile file = new MockMultipartFile(
                "file", "books.csv", "text/csv",
                "ISBN,Titulo,Autores\n9781234567890,Test Book,Author".getBytes());

        mockMvc.perform(multipart("/api/library-books/bulk-upload")
                .file(file))
                .andExpect(status().isUnauthorized());
    }

    // ========== PATCH /api/library-books/{bookId}/availability ==========

    @Test
    void updateAvailability_librarianRole_updatesPhysicalCopies() throws Exception {
        User librarianUser = new User("Librarian", "lib@test.com", "hash", UserRole.LIBRARIAN, "lib", null, null);

        Library library = new Library("Test Library", "desc", "123 Main", "lib@test.com", "555-1234", "9-5", 1L);
        setId(library, 10L);
        Librarian librarian = new Librarian(librarianUser, library, "lib@test.com");
        setId(librarian, 100L);

        LibraryBook libraryBook = new LibraryBook();
        libraryBook.setLibrary(library);
        libraryBook.setPhysicalCopies(3);
        libraryBook.setDigitalAvailable(true);
        libraryBook.setDigitalPlatform(1L);

        mockSecurityContext(librarianUser);

        when(userRepository.findByEmail("lib@test.com")).thenReturn(Optional.of(librarianUser));
        when(librarianRepository.findByUser(librarianUser)).thenReturn(Optional.of(librarian));
        when(libraryBookRepository.findByLibraryIdAndBookId(10L, 50L)).thenReturn(Optional.of(libraryBook));
        when(libraryBookRepository.save(any(LibraryBook.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(patch("/api/library-books/50/availability")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "physicalCopies": 10,
                                  "digitalAvailable": true,
                                  "digitalPlatformId": 2
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.physicalCopies").value(10))
                .andExpect(jsonPath("$.digitalAvailable").value(true))
                .andExpect(jsonPath("$.digitalPlatformId").value(2));
    }

    @Test
    void updateAvailability_readerRole_returns401() throws Exception {
        String token = "valid-reader-token";
        User readerUser = new User("Reader", "reader@test.com", "hash", UserRole.READER, "reader", null, null);

        when(jwtService.isValid(token)).thenReturn(true);
        when(jwtService.extractEmail(token)).thenReturn("reader@test.com");
        when(userRepository.findByEmail("reader@test.com")).thenReturn(Optional.of(readerUser));
        mockSecurityContext(readerUser);

        mockMvc.perform(patch("/api/library-books/50/availability")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "physicalCopies": 5
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateAvailability_bookNotInLibrary_returns500() throws Exception {
        User librarianUser = new User("Librarian", "lib@test.com", "hash", UserRole.LIBRARIAN, "lib", null, null);

        Library library = new Library("Test Library", "desc", "123 Main", "lib@test.com", "555-1234", "9-5", 1L);
        Librarian librarian = new Librarian(librarianUser, library, "lib@test.com");

        mockSecurityContext(librarianUser);

        when(userRepository.findByEmail("lib@test.com")).thenReturn(Optional.of(librarianUser));
        when(librarianRepository.findByUser(librarianUser)).thenReturn(Optional.of(librarian));
        when(libraryBookRepository.findByLibraryIdAndBookId(1L, 999L)).thenReturn(Optional.empty());

        mockMvc.perform(patch("/api/library-books/999/availability")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "physicalCopies": 5
                                }
                                """))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void updateAvailability_partialUpdate_mergesFields() throws Exception {
        User librarianUser = new User("Librarian", "lib@test.com", "hash", UserRole.LIBRARIAN, "lib", null, null);

        Library library = new Library("Test Library", "desc", "123 Main", "lib@test.com", "555-1234", "9-5", 1L);
        setId(library, 10L);
        Librarian librarian = new Librarian(librarianUser, library, "lib@test.com");

        LibraryBook libraryBook = new LibraryBook();
        libraryBook.setLibrary(library);
        libraryBook.setPhysicalCopies(5);
        libraryBook.setDigitalAvailable(false);
        libraryBook.setDigitalPlatform(1L);

        mockSecurityContext(librarianUser);

        when(userRepository.findByEmail("lib@test.com")).thenReturn(Optional.of(librarianUser));
        when(librarianRepository.findByUser(librarianUser)).thenReturn(Optional.of(librarian));
        when(libraryBookRepository.findByLibraryIdAndBookId(10L, 50L)).thenReturn(Optional.of(libraryBook));
        when(libraryBookRepository.save(any(LibraryBook.class))).thenAnswer(inv -> inv.getArgument(0));

        // Only update physicalCopies, leave digitalAvailable and digitalPlatform unchanged
        mockMvc.perform(patch("/api/library-books/50/availability")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "physicalCopies": 20
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.physicalCopies").value(20))
                .andExpect(jsonPath("$.digitalAvailable").value(false))
                .andExpect(jsonPath("$.digitalPlatformId").value(1));
    }
}