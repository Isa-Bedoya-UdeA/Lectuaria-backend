package com.lectuaria.backend.controller.library;

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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

    private void setId(Object entity, Long id) {
        try {
            var field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

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

    @Test
    void updateAvailability_updatesPhysicalCopies() throws Exception {
        String token = "valid-librarian-token";
        User librarianUser = new User("Librarian", "lib@test.com", "hash", UserRole.LIBRARIAN, "lib_test", null, null);

        Library library = new Library("Test Library", "desc", "123 Main", "lib@test.com", "555-1234", "9-5", 1L);
        setId(library, 1L);
        Librarian librarian = new Librarian(librarianUser, library, "lib@test.com");

        LibraryBook libraryBook = new LibraryBook();
        libraryBook.setLibrary(library);
        libraryBook.setPhysicalCopies(3);
        libraryBook.setDigitalAvailable(true);
        libraryBook.setDigitalPlatform(1L);

        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn("lib@test.com");
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(userRepository.findByEmail("lib@test.com")).thenReturn(Optional.of(librarianUser));
        when(librarianRepository.findByUser(librarianUser)).thenReturn(Optional.of(librarian));
        when(libraryBookRepository.findByLibraryIdAndBookId(1L, 50L)).thenReturn(Optional.of(libraryBook));
        when(libraryBookRepository.save(any(LibraryBook.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(patch("/api/library-books/50/availability")
                        .header("Authorization", "Bearer " + token)
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
    void updateAvailability_rejectsReaderRole() throws Exception {
        String token = "valid-reader-token";
        User readerUser = new User("Reader", "reader@test.com", "hash", UserRole.READER, "reader_test", null, null);

        when(jwtService.extractEmail("valid-reader-token")).thenReturn("reader@test.com");
        when(userRepository.findByEmail("reader@test.com")).thenReturn(Optional.of(readerUser));

        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn("reader@test.com");
        SecurityContextHolder.getContext().setAuthentication(auth);

        mockMvc.perform(patch("/api/library-books/50/availability")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "physicalCopies": 10
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateAvailability_rejectsWhenBookNotInLibrariansLibrary() throws Exception {
        String token = "valid-librarian-token";
        User librarianUser = new User("Librarian", "lib@test.com", "hash", UserRole.LIBRARIAN, "lib_test", null, null);
        Library library = new Library("Test Library", "desc", "123 Main", "lib@test.com", "555-1234", "9-5", 1L);
        Librarian librarian = new Librarian(librarianUser, library, "lib@test.com");

        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn("lib@test.com");
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(userRepository.findByEmail("lib@test.com")).thenReturn(Optional.of(librarianUser));
        when(librarianRepository.findByUser(librarianUser)).thenReturn(Optional.of(librarian));
        when(libraryBookRepository.findByLibraryIdAndBookId(1L, 999L)).thenReturn(Optional.empty());

        mockMvc.perform(patch("/api/library-books/999/availability")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "physicalCopies": 5
                                }
                                """))
                .andExpect(status().isInternalServerError());
    }
}