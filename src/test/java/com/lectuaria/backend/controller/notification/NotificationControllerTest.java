package com.lectuaria.backend.controller.notification;

import com.lectuaria.backend.dto.notification.NotificationDTO;
import com.lectuaria.backend.model.auth.User;
import com.lectuaria.backend.model.auth.UserRole;
import com.lectuaria.backend.model.notification.NotificationType;
import com.lectuaria.backend.repository.auth.UserRepository;
import com.lectuaria.backend.service.notification.INotificationService;
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

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private INotificationService notificationService;

    @MockBean
    private UserRepository userRepository;

    private User readerUser;

    @BeforeEach
    void setUp() {
        readerUser = new User("Reader", "reader@test.com", "hash", UserRole.READER, "reader", null, null);
        setId(readerUser, 10L);

        SecurityContextHolder.clearContext();
        lenient().when(jwtService.extractEmail(anyString())).thenReturn("reader@test.com");
        lenient().when(jwtService.isTokenValid(anyString(), anyString())).thenReturn(true);
    }

    @MockBean
    com.lectuaria.backend.security.JwtService jwtService;

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
        lenient().when(userRepository.findByEmail(email)).thenReturn(Optional.of(switch (email) {
            case "reader@test.com" -> readerUser;
            default -> null;
        }));
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

    private NotificationDTO makeNotification(Long id, String msg, NotificationType type, boolean read) {
        return new NotificationDTO(id, type, msg, 99L, null, read, Instant.now());
    }

    // ========== GET /api/notifications ==========

    @Nested
    class GetUserNotifications {

        @Test
        void getNotifications_authenticated_returnsAll() throws Exception {
            withUser("reader@test.com", UserRole.READER);

            List<NotificationDTO> notifications = List.of(
                    makeNotification(1L, "New friend request", NotificationType.FRIENDSHIP, false),
                    makeNotification(2L, "New review", NotificationType.REVIEW, true)
            );
            when(notificationService.getUserNotifications(10L, null)).thenReturn(notifications);

            mockMvc.perform(get("/api/notifications")
                            .header("Authorization", authHeader("any-token")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.notificationDTOList.length()").value(2))
                    .andExpect(jsonPath("$._embedded.notificationDTOList[0].message").value("New friend request"))
                    .andExpect(jsonPath("$._embedded.notificationDTOList[0].read").value(false))
                    .andExpect(jsonPath("$._embedded.notificationDTOList[1].read").value(true));
        }

        @Test
        void getNotifications_unauthenticated_returns401() throws Exception {
            mockMvc.perform(get("/api/notifications"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void getNotifications_unreadOnly_returnsUnread() throws Exception {
            withUser("reader@test.com", UserRole.READER);

            List<NotificationDTO> unread = List.of(
                    makeNotification(1L, "Unread notification", NotificationType.FRIENDSHIP, false)
            );
            when(notificationService.getUserNotifications(10L, true)).thenReturn(unread);

            mockMvc.perform(get("/api/notifications")
                            .header("Authorization", authHeader("any-token"))
                            .param("unreadOnly", "true"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.notificationDTOList.length()").value(1))
                    .andExpect(jsonPath("$._embedded.notificationDTOList[0].read").value(false));
        }
    }

    // ========== GET /api/notifications/unread-count ==========

    @Nested
    class GetUnreadCount {

        @Test
        void getUnreadCount_authenticated_returnsCount() throws Exception {
            withUser("reader@test.com", UserRole.READER);

            when(notificationService.getUnreadCount(10L)).thenReturn(5L);

            mockMvc.perform(get("/api/notifications/unread-count")
                            .header("Authorization", authHeader("any-token")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.unreadCount").value(5));
        }

        @Test
        void getUnreadCount_zero_returns0() throws Exception {
            withUser("reader@test.com", UserRole.READER);

            when(notificationService.getUnreadCount(10L)).thenReturn(0L);

            mockMvc.perform(get("/api/notifications/unread-count")
                            .header("Authorization", authHeader("any-token")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.unreadCount").value(0));
        }
    }

    // ========== PUT /api/notifications/{id}/read ==========

    @Nested
    class MarkAsRead {

        @Test
        void markAsRead_authenticated_returnsUpdated() throws Exception {
            withUser("reader@test.com", UserRole.READER);

            NotificationDTO updated = makeNotification(1L, "Friend request", NotificationType.FRIENDSHIP, true);
            when(notificationService.markAsRead(1L, 10L)).thenReturn(updated);

            mockMvc.perform(put("/api/notifications/1/read")
                            .header("Authorization", authHeader("any-token")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.read").value(true));
        }
    }

    // ========== PUT /api/notifications/read-all ==========

    @Nested
    class MarkAllAsRead {

        @Test
        void markAllAsRead_authenticated_returns200() throws Exception {
            withUser("reader@test.com", UserRole.READER);

            doNothing().when(notificationService).markAllAsRead(10L);

            mockMvc.perform(put("/api/notifications/read-all")
                            .header("Authorization", authHeader("any-token")))
                    .andExpect(status().isOk());
        }
    }

    // ========== DELETE /api/notifications/{id} ==========

    @Nested
    class DeleteNotification {

        @Test
        void deleteNotification_authenticated_returns204() throws Exception {
            withUser("reader@test.com", UserRole.READER);

            doNothing().when(notificationService).deleteNotification(1L, 10L);

            mockMvc.perform(delete("/api/notifications/1")
                            .header("Authorization", authHeader("any-token")))
                    .andExpect(status().isNoContent());
        }
    }

    // ========== DELETE /api/notifications ==========

    @Nested
    class DeleteAllNotifications {

        @Test
        void deleteAllNotifications_authenticated_returns204() throws Exception {
            withUser("reader@test.com", UserRole.READER);

            doNothing().when(notificationService).deleteAllNotifications(10L);

            mockMvc.perform(delete("/api/notifications")
                            .header("Authorization", authHeader("any-token")))
                    .andExpect(status().isNoContent());
        }
    }
}