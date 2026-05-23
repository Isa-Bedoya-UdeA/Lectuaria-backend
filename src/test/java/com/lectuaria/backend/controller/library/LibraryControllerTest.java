package com.lectuaria.backend.controller.library;

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

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
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

    // Auth-dependent tests use @WithMockUser pattern — see AuthControllerTest
    // for full JWT token flow tests. LibraryController endpoints that need auth
    // are covered in integration tests with real security context.
}