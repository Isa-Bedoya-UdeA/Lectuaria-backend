package com.lectuaria.backend.controller.list;

import com.lectuaria.backend.dto.book.BookSummaryDTO;
import com.lectuaria.backend.dto.list.CreateListRequestDTO;
import com.lectuaria.backend.dto.list.FavoriteToggleResponseDTO;
import com.lectuaria.backend.dto.list.MoveBookResponseDTO;
import com.lectuaria.backend.dto.list.UpdateListRequestDTO;
import com.lectuaria.backend.dto.list.UserListDTO;
import com.lectuaria.backend.model.auth.User;
import com.lectuaria.backend.model.auth.UserRole;
import com.lectuaria.backend.model.list.ListType;
import com.lectuaria.backend.model.list.ListVisibility;
import com.lectuaria.backend.repository.auth.UserRepository;
import com.lectuaria.backend.security.JwtService;
import com.lectuaria.backend.service.list.IUserListService;
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
class UserListControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IUserListService listService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtService jwtService;

    private User readerUser;
    private User librarianUser;

    @BeforeEach
    void setUp() {
        readerUser = new User("Reader", "reader@test.com", "hash", UserRole.READER, "reader", null, null);
        setId(readerUser, 10L);

        librarianUser = new User("Librarian", "lib@test.com", "hash", UserRole.LIBRARIAN, "librarian", null, null);
        setId(librarianUser, 30L);

        SecurityContextHolder.clearContext();
        lenient().when(jwtService.extractEmail(anyString())).thenReturn("reader@test.com");
        lenient().when(jwtService.isTokenValid(anyString(), anyString())).thenReturn(true);
    }

    private void setId(Object entity, Long id) {
        try {
            var f = entity.getClass().getDeclaredField("id");
            f.setAccessible(true);
            f.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void withUser(String email, UserRole role) {
        SecurityContextHolder.clearContext();
        switch (email) {
            case "reader@test.com" -> lenient().when(userRepository.findByEmail("reader@test.com")).thenReturn(Optional.of(readerUser));
            case "lib@test.com" -> lenient().when(userRepository.findByEmail("lib@test.com")).thenReturn(Optional.of(librarianUser));
        }
        lenient().when(jwtService.extractEmail(anyString())).thenReturn(email);
        List<SimpleGrantedAuthority> auths = List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(email, null, auths);
        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(auth);
        SecurityContextHolder.setContext(ctx);
    }

    private String authHeader(String token) {
        return "Bearer " + token;
    }

    private UserListDTO makeList(Long id, String name, ListType type, ListVisibility vis) {
        UserListDTO dto = new UserListDTO(id, name, "Description for " + name, type, vis, 5L, Instant.now());
        dto.setUserId(10L);
        return dto;
    }

    // ========== GET /api/lists ==========

    @Nested
    class GetMyLists {

        @Test
        void getMyLists_authenticated_returnsLists() throws Exception {
            withUser("reader@test.com", UserRole.READER);

            List<UserListDTO> lists = List.of(
                    makeList(1L, "Favorites", ListType.CUSTOM, ListVisibility.PRIVATE),
                    makeList(2L, "Reading Now", ListType.CUSTOM, ListVisibility.PUBLIC)
            );
            when(listService.getUserLists(10L)).thenReturn(lists);

            mockMvc.perform(get("/api/lists")
                            .header("Authorization", authHeader("any-token")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.userListDTOList.length()").value(2))
                    .andExpect(jsonPath("$._embedded.userListDTOList[0].name").value("Favorites"))
                    .andExpect(jsonPath("$._embedded.userListDTOList[1].name").value("Reading Now"));
        }

        @Test
        void getMyLists_unauthenticated_returns401() throws Exception {
            mockMvc.perform(get("/api/lists"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ========== GET /api/lists/{listId} ==========

    @Nested
    class GetListDetails {

        @Test
        void getListDetails_authenticated_returnsList() throws Exception {
            withUser("reader@test.com", UserRole.READER);

            UserListDTO list = makeList(1L, "My Favorites", ListType.SYSTEM, ListVisibility.PRIVATE);
            when(listService.getListDetails(1L, 10L)).thenReturn(list);

            mockMvc.perform(get("/api/lists/1")
                            .header("Authorization", authHeader("any-token")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.name").value("My Favorites"));
        }

        @Test
        void getListDetails_librarian_returns401() throws Exception {
            withUser("lib@test.com", UserRole.LIBRARIAN);

            mockMvc.perform(get("/api/lists/1")
                            .header("Authorization", authHeader("any-token")))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Los bibliotecarios no pueden tener listas de lectura."));
        }
    }

    // ========== POST /api/lists ==========

    @Nested
    class CreateList {

        @Test
        void createList_authenticated_returnsCreatedList() throws Exception {
            withUser("reader@test.com", UserRole.READER);

            UserListDTO created = makeList(5L, "New List", ListType.CUSTOM, ListVisibility.PUBLIC);
            when(listService.createCustomList(any(CreateListRequestDTO.class), eq(readerUser))).thenReturn(created);

            String body = """
                {
                  "name": "New List",
                  "description": "A new custom list",
                  "visibility": "PUBLIC"
                }
                """;

            mockMvc.perform(post("/api/lists")
                            .header("Authorization", authHeader("any-token"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(5))
                    .andExpect(jsonPath("$.name").value("New List"));
        }

        @Test
        void createList_librarian_returns401() throws Exception {
            withUser("lib@test.com", UserRole.LIBRARIAN);

            String body = """
                {
                  "name": "Librarian List",
                  "visibility": "PUBLIC"
                }
                """;

            mockMvc.perform(post("/api/lists")
                            .header("Authorization", authHeader("any-token"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ========== POST /api/lists/{listId}/books/{bookId} ==========

    @Nested
    class AddBook {

        @Test
        void addBook_authenticated_returns200() throws Exception {
            withUser("reader@test.com", UserRole.READER);

            doNothing().when(listService).addBookToList(1L, 100L, readerUser, false);

            mockMvc.perform(post("/api/lists/1/books/100")
                            .header("Authorization", authHeader("any-token")))
                    .andExpect(status().isOk());
        }

        @Test
        void addBook_withForce_returns200() throws Exception {
            withUser("reader@test.com", UserRole.READER);

            doNothing().when(listService).addBookToList(1L, 100L, readerUser, true);

            mockMvc.perform(post("/api/lists/1/books/100")
                            .header("Authorization", authHeader("any-token"))
                            .param("force", "true"))
                    .andExpect(status().isOk());
        }
    }

    // ========== DELETE /api/lists/{listId}/books/{bookId} ==========

    @Nested
    class RemoveBook {

        @Test
        void removeBook_authenticated_returns200() throws Exception {
            withUser("reader@test.com", UserRole.READER);

            doNothing().when(listService).removeBookFromList(1L, 100L, readerUser);

            mockMvc.perform(delete("/api/lists/1/books/100")
                            .header("Authorization", authHeader("any-token")))
                    .andExpect(status().isOk());
        }
    }

    // ========== DELETE /api/lists/{listId} ==========

    @Nested
    class DeleteList {

        @Test
        void deleteList_withConfirm_returns204() throws Exception {
            withUser("reader@test.com", UserRole.READER);

            doNothing().when(listService).deleteList(1L, readerUser, true, false);

            mockMvc.perform(delete("/api/lists/1")
                            .header("Authorization", authHeader("any-token"))
                            .param("confirm", "true"))
                    .andExpect(status().isNoContent());
        }
    }

    // ========== PATCH /api/lists/{listId} ==========

    @Nested
    class UpdateList {

        @Test
        void updateList_authenticated_returnsUpdatedList() throws Exception {
            withUser("reader@test.com", UserRole.READER);

            UserListDTO updated = makeList(1L, "Renombrada", ListType.CUSTOM, ListVisibility.PRIVATE);
            when(listService.updateCustomList(eq(1L), any(UpdateListRequestDTO.class), eq(readerUser)))
                    .thenReturn(updated);

            String body = """
                {
                  "name": "Renombrada",
                  "visibility": "PRIVATE"
                }
                """;

            mockMvc.perform(patch("/api/lists/1")
                            .header("Authorization", authHeader("any-token"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Renombrada"))
                    .andExpect(jsonPath("$.visibility").value("PRIVATE"));
        }

        @Test
        void updateList_librarian_returns401() throws Exception {
            withUser("lib@test.com", UserRole.LIBRARIAN);

            String body = """
                {
                  "name": "Hack"
                }
                """;

            mockMvc.perform(patch("/api/lists/1")
                            .header("Authorization", authHeader("any-token"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ========== POST /api/lists/favorites/books/{bookId}/toggle ==========

    @Nested
    class ToggleFavorite {

        @Test
        void toggleFavorite_authenticated_returnsToggleResponse() throws Exception {
            withUser("reader@test.com", UserRole.READER);

            when(listService.toggleFavorite(100L, readerUser)).thenReturn(true);

            mockMvc.perform(post("/api/lists/favorites/books/100/toggle")
                            .header("Authorization", authHeader("any-token")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.bookId").value(100))
                    .andExpect(jsonPath("$.favorite").value(true));
        }

        @Test
        void toggleFavorite_librarian_returns401() throws Exception {
            withUser("lib@test.com", UserRole.LIBRARIAN);

            mockMvc.perform(post("/api/lists/favorites/books/100/toggle")
                            .header("Authorization", authHeader("any-token")))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ========== GET /api/lists/favorites ==========

    @Nested
    class GetFavorites {

        @Test
        void getFavorites_authenticated_returnsFavoritesList() throws Exception {
            withUser("reader@test.com", UserRole.READER);

            UserListDTO favList = makeList(0L, "Favorites", ListType.CUSTOM, ListVisibility.PRIVATE);
            when(listService.getMyFavorites(readerUser)).thenReturn(favList);

            mockMvc.perform(get("/api/lists/favorites")
                            .header("Authorization", authHeader("any-token")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Favorites"));
        }
    }
}