package com.lectuaria.backend.controller.friendship;

import com.lectuaria.backend.dto.common.UserSearchResponseDTO;
import com.lectuaria.backend.model.auth.User;
import com.lectuaria.backend.model.auth.UserRole;
import com.lectuaria.backend.repository.auth.UserRepository;
import com.lectuaria.backend.security.JwtService;
import com.lectuaria.backend.service.friendship.IFriendshipService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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
class FriendshipControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IFriendshipService friendshipService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtService jwtService;

    private User readerUser;
    private User friendUser;
    private User librarianUser;

    @BeforeEach
    void setUp() {
        readerUser = new User("Reader", "reader@test.com", "hash", UserRole.READER, "reader", null, null);
        setId(readerUser, 10L);

        friendUser = new User("Friend", "friend@test.com", "hash", UserRole.READER, "friend", null, null);
        setId(friendUser, 20L);

        librarianUser = new User("Librarian", "lib@test.com", "hash", UserRole.LIBRARIAN, "librarian", null, null);
        setId(librarianUser, 30L);

        SecurityContextHolder.clearContext();
        // Default: return reader for any token email
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
        // Configure userRepository based on which email we're testing
        switch (email) {
            case "reader@test.com" -> lenient().when(userRepository.findByEmail("reader@test.com")).thenReturn(Optional.of(readerUser));
            case "lib@test.com" -> lenient().when(userRepository.findByEmail("lib@test.com")).thenReturn(Optional.of(librarianUser));
            default -> { /* nothing */ }
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

    // ========== GET /api/friendships/search ==========

    @Nested
    class SearchReaders {

        @Test
        void searchReaders_publicNoToken_returnsResults() throws Exception {
            List<UserSearchResponseDTO> results = List.of(
                    new UserSearchResponseDTO(2L, "Alice", "alice", null, "Medellín", "none", null)
            );
            when(friendshipService.searchReaders(eq("alice"), isNull())).thenReturn(results);

            mockMvc.perform(get("/api/friendships/search")
                            .param("query", "alice"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].fullName").value("Alice"))
                    .andExpect(jsonPath("$[0].friendshipStatus").value("none"));
        }

        @Test
        void searchReaders_authenticatedWithToken_returnsResultsWithStatus() throws Exception {
            withUser("reader@test.com", UserRole.READER);

            List<UserSearchResponseDTO> results = List.of(
                    new UserSearchResponseDTO(2L, "Alice", "alice", null, "Medellín", "friends", null)
            );
            when(friendshipService.searchReaders(eq("alice"), eq(readerUser))).thenReturn(results);

            mockMvc.perform(get("/api/friendships/search")
                            .header("Authorization", authHeader("any-token"))
                            .param("query", "alice"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].friendshipStatus").value("friends"));
        }

        @Test
        void searchReaders_librarianRole_returns401() throws Exception {
            withUser("lib@test.com", UserRole.LIBRARIAN);

            mockMvc.perform(get("/api/friendships/search")
                            .header("Authorization", authHeader("any-token"))
                            .param("query", "alice"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Los bibliotecarios no tienen acceso a opciones de amistad"));
        }

        @Test
        void searchReaders_invalidToken_returnsEmptyOrFallsBack() throws Exception {
            when(jwtService.extractEmail("bad-token")).thenThrow(new RuntimeException("bad token"));

            List<UserSearchResponseDTO> results = List.of(
                    new UserSearchResponseDTO(3L, "Bob", "bob", null, "Medellín", "none", null)
            );
            when(friendshipService.searchReaders(eq("bob"), isNull())).thenReturn(results);

            mockMvc.perform(get("/api/friendships/search")
                            .header("Authorization", "Bearer bad-token")
                            .param("query", "bob"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }
    }

    // ========== GET /api/friendships ==========

    @Nested
    class GetFriends {

        @Test
        void getFriends_authenticated_returnsFriends() throws Exception {
            withUser("reader@test.com", UserRole.READER);

            List<UserSearchResponseDTO> friends = List.of(
                    new UserSearchResponseDTO(20L, "Friend", "friend", null, "Medellín", "friends", null)
            );
            when(friendshipService.getFriends(readerUser)).thenReturn(friends);

            mockMvc.perform(get("/api/friendships")
                            .header("Authorization", authHeader("any-token")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].fullName").value("Friend"));
        }

        @Test
        void getFriends_unauthenticated_returns401() throws Exception {
            mockMvc.perform(get("/api/friendships"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void getFriends_librarian_returns401() throws Exception {
            withUser("lib@test.com", UserRole.LIBRARIAN);

            mockMvc.perform(get("/api/friendships")
                            .header("Authorization", authHeader("any-token")))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Los bibliotecarios no tienen acceso a opciones de amistad"));
        }
    }

    // ========== GET /api/friendships/requests/pending ==========

    @Nested
    class GetPendingRequests {

        @Test
        void getPendingRequests_authenticated_returnsRequests() throws Exception {
            withUser("reader@test.com", UserRole.READER);

            List<UserSearchResponseDTO> requests = List.of(
                    new UserSearchResponseDTO(3L, "Charlie", "charlie", null, "Medellín", "pending_received", 99L)
            );
            when(friendshipService.getPendingRequests(readerUser)).thenReturn(requests);

            mockMvc.perform(get("/api/friendships/requests/pending")
                            .header("Authorization", authHeader("any-token")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].friendshipStatus").value("pending_received"))
                    .andExpect(jsonPath("$[0].friendshipRequestId").value(99));
        }

        @Test
        void getPendingRequests_unauthenticated_returns401() throws Exception {
            mockMvc.perform(get("/api/friendships/requests/pending"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ========== POST /api/friendships/requests/{receiverId} ==========

    @Nested
    class SendFriendshipRequest {

        @Test
        void sendRequest_authenticated_returns200() throws Exception {
            withUser("reader@test.com", UserRole.READER);

            doNothing().when(friendshipService).sendFriendshipRequest(20L, readerUser);

            mockMvc.perform(post("/api/friendships/requests/20")
                            .header("Authorization", authHeader("any-token")))
                    .andExpect(status().isOk());
        }

        @Test
        void sendRequest_unauthenticated_returns401() throws Exception {
            mockMvc.perform(post("/api/friendships/requests/20"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void sendRequest_librarian_returns401() throws Exception {
            withUser("lib@test.com", UserRole.LIBRARIAN);

            mockMvc.perform(post("/api/friendships/requests/20")
                            .header("Authorization", authHeader("any-token")))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Los bibliotecarios no tienen acceso a opciones de amistad"));
        }
    }

    // ========== POST /api/friendships/requests/{requestId}/accept ==========

    @Nested
    class AcceptFriendshipRequest {

        @Test
        void acceptRequest_authenticated_returns200() throws Exception {
            withUser("reader@test.com", UserRole.READER);

            doNothing().when(friendshipService).acceptFriendshipRequest(99L, readerUser);

            mockMvc.perform(post("/api/friendships/requests/99/accept")
                            .header("Authorization", authHeader("any-token")))
                    .andExpect(status().isOk());
        }

        @Test
        void acceptRequest_unauthenticated_returns401() throws Exception {
            mockMvc.perform(post("/api/friendships/requests/99/accept"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ========== POST /api/friendships/requests/{requestId}/reject ==========

    @Nested
    class RejectFriendshipRequest {

        @Test
        void rejectRequest_authenticated_returns200() throws Exception {
            withUser("reader@test.com", UserRole.READER);

            doNothing().when(friendshipService).rejectFriendshipRequest(99L, readerUser);

            mockMvc.perform(post("/api/friendships/requests/99/reject")
                            .header("Authorization", authHeader("any-token")))
                    .andExpect(status().isOk());
        }

        @Test
        void rejectRequest_unauthenticated_returns401() throws Exception {
            mockMvc.perform(post("/api/friendships/requests/99/reject"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ========== DELETE /api/friendships/requests/{requestId} ==========

    @Nested
    class CancelFriendshipRequest {

        @Test
        void cancelRequest_authenticated_returns200() throws Exception {
            withUser("reader@test.com", UserRole.READER);

            doNothing().when(friendshipService).cancelFriendshipRequest(99L, readerUser);

            mockMvc.perform(delete("/api/friendships/requests/99")
                            .header("Authorization", authHeader("any-token")))
                    .andExpect(status().isOk());
        }

        @Test
        void cancelRequest_unauthenticated_returns401() throws Exception {
            mockMvc.perform(delete("/api/friendships/requests/99"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ========== DELETE /api/friendships/{friendId} ==========

    @Nested
    class RemoveFriendship {

        @Test
        void removeFriendship_authenticated_returns200() throws Exception {
            withUser("reader@test.com", UserRole.READER);

            doNothing().when(friendshipService).removeFriendship(20L, readerUser);

            mockMvc.perform(delete("/api/friendships/20")
                            .header("Authorization", authHeader("any-token")))
                    .andExpect(status().isOk());
        }

        @Test
        void removeFriendship_unauthenticated_returns401() throws Exception {
            mockMvc.perform(delete("/api/friendships/20"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void removeFriendship_librarian_returns401() throws Exception {
            withUser("lib@test.com", UserRole.LIBRARIAN);

            mockMvc.perform(delete("/api/friendships/20")
                            .header("Authorization", authHeader("any-token")))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Los bibliotecarios no tienen acceso a opciones de amistad"));
        }
    }
}