package com.lectuaria.backend.controller.notification;

import com.lectuaria.backend.dto.notification.NotificationPreferenceDTO;
import com.lectuaria.backend.model.auth.User;
import com.lectuaria.backend.model.auth.UserRole;
import com.lectuaria.backend.model.notification.NotificationType;
import com.lectuaria.backend.repository.auth.UserRepository;
import com.lectuaria.backend.security.JwtService;
import com.lectuaria.backend.service.notification.INotificationPreferenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.anyBoolean;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationPreferenceController.class)
@AutoConfigureMockMvc(addFilters = false)
class NotificationPreferenceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private INotificationPreferenceService preferenceService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private com.lectuaria.backend.security.AuthenticatedUserResolver authenticatedUserResolver;

    private User readerUser;

    @BeforeEach
    void setUp() {
        readerUser = new User("Reader", "reader@test.com", "hash", UserRole.READER, "reader", null, null);
        setId(readerUser, 10L);

        when(jwtService.extractEmail("valid-token")).thenReturn("reader@test.com");
        when(userRepository.findByEmail("reader@test.com")).thenReturn(java.util.Optional.of(readerUser));
        when(authenticatedUserResolver.requireCurrentUserId()).thenReturn(10L);

        // Set up SecurityContext to simulate authenticated user
        withUser("reader@test.com", UserRole.READER);
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

    private void withUser(String email, UserRole role) {
        SecurityContextHolder.clearContext();
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                email, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role.name())));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // ========== GET /api/notification-preferences ==========

    @Nested
    class GetUserPreferences {

        @Test
        void getUserPreferences_returnsPreferences() throws Exception {
            NotificationPreferenceDTO pref = new NotificationPreferenceDTO(1L, NotificationType.FRIENDSHIP, true);
            when(preferenceService.getUserPreferences(any())).thenReturn(List.of(pref));

            mockMvc.perform(get("/api/notification-preferences")
                            .header("Authorization", "Bearer valid-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.notificationPreferenceDTOList.length()").value(1))
                    .andExpect(jsonPath("$._embedded.notificationPreferenceDTOList[0].notificationType").value("FRIENDSHIP"))
                    .andExpect(jsonPath("$._embedded.notificationPreferenceDTOList[0].isEnabled").value(true));
        }

        @Test
        void getUserPreferences_emptyList_returnsEmpty() throws Exception {
            when(preferenceService.getUserPreferences(any())).thenReturn(List.of());

            mockMvc.perform(get("/api/notification-preferences")
                            .header("Authorization", "Bearer valid-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.notificationPreferenceDTOList").doesNotExist());
        }

        @Test
        void getUserPreferences_multipleTypes_returnsAll() throws Exception {
            NotificationPreferenceDTO pref1 = new NotificationPreferenceDTO(1L, NotificationType.FRIENDSHIP, true);
            NotificationPreferenceDTO pref2 = new NotificationPreferenceDTO(2L, NotificationType.SHARED, false);
            when(preferenceService.getUserPreferences(any())).thenReturn(List.of(pref1, pref2));

            mockMvc.perform(get("/api/notification-preferences")
                            .header("Authorization", "Bearer valid-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.notificationPreferenceDTOList.length()").value(2))
                    .andExpect(jsonPath("$._embedded.notificationPreferenceDTOList[1].notificationType").value("SHARED"))
                    .andExpect(jsonPath("$._embedded.notificationPreferenceDTOList[1].isEnabled").value(false));
        }
    }

    // ========== PUT /api/notification-preferences/{type} ==========

    @Nested
    class UpdatePreference {

        @Test
        void updatePreference_enable_returnsUpdatedPreference() throws Exception {
            NotificationPreferenceDTO updated = new NotificationPreferenceDTO(1L, NotificationType.FRIENDSHIP, true);
            when(preferenceService.updatePreference(any(), any(), anyBoolean())).thenReturn(updated);

            mockMvc.perform(put("/api/notification-preferences/FRIENDSHIP")
                            .header("Authorization", "Bearer valid-token")
                            .param("enabled", "true"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.notificationType").value("FRIENDSHIP"))
                    .andExpect(jsonPath("$.isEnabled").value(true));
        }

        @Test
        void updatePreference_disable_returnsUpdatedPreference() throws Exception {
            NotificationPreferenceDTO updated = new NotificationPreferenceDTO(1L, NotificationType.FRIENDSHIP, false);
            when(preferenceService.updatePreference(any(), any(), anyBoolean())).thenReturn(updated);

            mockMvc.perform(put("/api/notification-preferences/FRIENDSHIP")
                            .header("Authorization", "Bearer valid-token")
                            .param("enabled", "false"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isEnabled").value(false));
        }
    }

    // ========== POST /api/notification-preferences/reset ==========

    @Nested
    class ResetToDefaults {

        @Test
        void resetToDefaults_returnsOk() throws Exception {
            mockMvc.perform(post("/api/notification-preferences/reset")
                            .header("Authorization", "Bearer valid-token"))
                    .andExpect(status().isOk());
        }
    }
}