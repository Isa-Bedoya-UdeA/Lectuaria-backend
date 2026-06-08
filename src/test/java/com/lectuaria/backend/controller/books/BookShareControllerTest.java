package com.lectuaria.backend.controller.books;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lectuaria.backend.dto.book.BookShareRequestDTO;
import com.lectuaria.backend.dto.book.BookShareResponseDTO;
import com.lectuaria.backend.dto.book.ShareResultDTO;
import com.lectuaria.backend.model.auth.User;
import com.lectuaria.backend.model.auth.UserRole;
import com.lectuaria.backend.repository.auth.UserRepository;
import com.lectuaria.backend.security.JwtService;
import com.lectuaria.backend.service.book.IBookShareService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookShareController.class)
@AutoConfigureMockMvc(addFilters = false)
class BookShareControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private IBookShareService bookShareService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private com.lectuaria.backend.security.AuthenticatedUserResolver authenticatedUserResolver;

    private User readerUser;

    @BeforeEach
    void setUp() {
        readerUser = new User("Reader", "reader@test.com", "hash", UserRole.READER, "reader", null, null);
        setId(readerUser, 10L);

        when(jwtService.extractEmail("valid-token")).thenReturn("reader@test.com");
        when(userRepository.findByEmail("reader@test.com")).thenReturn(java.util.Optional.of(readerUser));
        when(authenticatedUserResolver.requireCurrentUser(any(jakarta.servlet.http.HttpServletRequest.class)))
                .thenReturn(readerUser);
    }

    private void setId(Object entity, Long id) {
        try {
            var field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ========== POST /api/books/{bookId}/share ==========

    @Nested
    class ShareBook {

        @Test
        void shareBook_validRequest_returnsResult() throws Exception {
            ShareResultDTO result = new ShareResultDTO(2, 0, List.of(), "Libro compartido exitosamente con 2 amigo(s)");
            when(bookShareService.shareBookWithFriends(eq(100L), any(), any())).thenReturn(result);

            BookShareRequestDTO request = new BookShareRequestDTO(List.of(2L, 3L), "Check this!");

            mockMvc.perform(post("/api/books/100/share")
                            .header("Authorization", "Bearer valid-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.successfulShares").value(2))
                    .andExpect(jsonPath("$.failedShares").value(0))
                    .andExpect(jsonPath("$.message").value("Libro compartido exitosamente con 2 amigo(s)"));
        }

        @Test
        void shareBook_withMessage_includesInResult() throws Exception {
            ShareResultDTO result = new ShareResultDTO(1, 0, List.of(), "Libro compartido exitosamente con 1 amigo(s)");
            when(bookShareService.shareBookWithFriends(eq(100L), any(), any())).thenReturn(result);

            BookShareRequestDTO request = new BookShareRequestDTO(List.of(2L), "Must read!");

            mockMvc.perform(post("/api/books/100/share")
                            .header("Authorization", "Bearer valid-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.successfulShares").value(1));
        }
    }

    // ========== GET /api/books/shares/received ==========

    @Nested
    class GetReceivedShares {

        @Test
        void getReceivedShares_returnsShares() throws Exception {
            BookShareResponseDTO share = new BookShareResponseDTO(
                    1L, 100L, "Test Book", 1L, "Sender", 10L, "Reader", "Hello", Instant.now());
            when(bookShareService.getReceivedShares(any())).thenReturn(List.of(share));

            mockMvc.perform(get("/api/books/shares/received")
                            .header("Authorization", "Bearer valid-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.bookShareResponseDTOList.length()").value(1))
                    .andExpect(jsonPath("$._embedded.bookShareResponseDTOList[0].bookTitle").value("Test Book"))
                    .andExpect(jsonPath("$._embedded.bookShareResponseDTOList[0].senderId").value(1))
                    .andExpect(jsonPath("$._embedded.bookShareResponseDTOList[0].receiverId").value(10));
        }

        @Test
        void getReceivedShares_empty_returnsEmptyList() throws Exception {
            when(bookShareService.getReceivedShares(any())).thenReturn(List.of());

            mockMvc.perform(get("/api/books/shares/received")
                            .header("Authorization", "Bearer valid-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.bookShareResponseDTOList").doesNotExist());
        }
    }

    // ========== GET /api/books/shares/sent ==========

    @Nested
    class GetSentShares {

        @Test
        void getSentShares_returnsShares() throws Exception {
            BookShareResponseDTO share = new BookShareResponseDTO(
                    1L, 100L, "Test Book", 10L, "Reader", 2L, "Friend", "Hello", Instant.now());
            when(bookShareService.getSentShares(any())).thenReturn(List.of(share));

            mockMvc.perform(get("/api/books/shares/sent")
                            .header("Authorization", "Bearer valid-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.bookShareResponseDTOList.length()").value(1))
                    .andExpect(jsonPath("$._embedded.bookShareResponseDTOList[0].bookTitle").value("Test Book"));
        }

        @Test
        void getSentShares_empty_returnsEmptyList() throws Exception {
            when(bookShareService.getSentShares(any())).thenReturn(List.of());

            mockMvc.perform(get("/api/books/shares/sent")
                            .header("Authorization", "Bearer valid-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.bookShareResponseDTOList").doesNotExist());
        }
    }

    // ========== GET /api/books/{bookId}/shared-with/{friendId} ==========

    @Nested
    class IsBookSharedWithFriend {

        @Test
        void isBookSharedWithFriend_returnsTrue() throws Exception {
            when(bookShareService.isBookSharedWithFriend(10L, 2L, 100L)).thenReturn(true);

            mockMvc.perform(get("/api/books/100/shared-with/2")
                            .header("Authorization", "Bearer valid-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isShared").value(true));
        }

        @Test
        void isBookSharedWithFriend_returnsFalse() throws Exception {
            when(bookShareService.isBookSharedWithFriend(10L, 2L, 100L)).thenReturn(false);

            mockMvc.perform(get("/api/books/100/shared-with/2")
                            .header("Authorization", "Bearer valid-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isShared").value(false));
        }
    }
}