package com.lectuaria.backend.service.notification.impl;

import com.lectuaria.backend.dto.notification.NotificationPreferenceDTO;
import com.lectuaria.backend.model.auth.User;
import com.lectuaria.backend.model.auth.UserRole;
import com.lectuaria.backend.model.notification.NotificationPreference;
import com.lectuaria.backend.model.notification.NotificationType;
import com.lectuaria.backend.repository.auth.UserRepository;
import com.lectuaria.backend.repository.notification.NotificationPreferenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationPreferenceServiceImplTest {

    @Mock
    private NotificationPreferenceRepository preferenceRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationPreferenceServiceImpl service;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User("Test User", "test@test.com", "hash", UserRole.READER, "testuser", null, null);
        setId(testUser, 10L);
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

    // ========== getUserPreferences ==========

    @Nested
    class GetUserPreferencesTests {

        @Test
        void getUserPreferences_whenPreferencesExist_returnsPreferences() {
            NotificationPreference pref = new NotificationPreference(testUser, NotificationType.FRIENDSHIP, true);
            setId(pref, 1L);
            when(preferenceRepository.findByUserId(10L)).thenReturn(List.of(pref));

            List<NotificationPreferenceDTO> result = service.getUserPreferences(10L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getNotificationType()).isEqualTo(NotificationType.FRIENDSHIP);
            assertThat(result.get(0).isEnabled()).isTrue();
        }

        @Test
        void getUserPreferences_whenNoPreferences_createsDefaults() {
            when(preferenceRepository.findByUserId(10L)).thenReturn(Collections.emptyList());
            when(userRepository.findById(10L)).thenReturn(Optional.of(testUser));
            when(preferenceRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

            List<NotificationPreferenceDTO> result = service.getUserPreferences(10L);

            assertThat(result).hasSize(NotificationType.values().length);
            verify(preferenceRepository).saveAll(any());
        }

        @Test
        void getUserPreferences_mapsAllFieldsCorrectly() {
            NotificationPreference pref = new NotificationPreference(testUser, NotificationType.SHARED, false);
            setId(pref, 5L);
            when(preferenceRepository.findByUserId(10L)).thenReturn(List.of(pref));

            NotificationPreferenceDTO result = service.getUserPreferences(10L).get(0);

            assertThat(result.getId()).isEqualTo(5L);
            assertThat(result.getNotificationType()).isEqualTo(NotificationType.SHARED);
            assertThat(result.isEnabled()).isFalse();
        }
    }

    // ========== updatePreference ==========

    @Nested
    class UpdatePreferenceTests {

        @Test
        void updatePreference_existingPreference_updatesAndSaves() {
            NotificationPreference pref = new NotificationPreference(testUser, NotificationType.FRIENDSHIP, true);
            setId(pref, 1L);
            when(userRepository.findById(10L)).thenReturn(Optional.of(testUser));
            when(preferenceRepository.findByUserIdAndNotificationType(10L, NotificationType.FRIENDSHIP))
                    .thenReturn(Optional.of(pref));
            when(preferenceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            NotificationPreferenceDTO result = service.updatePreference(10L, NotificationType.FRIENDSHIP, false);

            assertThat(result.isEnabled()).isFalse();
            assertThat(result.getNotificationType()).isEqualTo(NotificationType.FRIENDSHIP);
        }

        @Test
        void updatePreference_newPreference_createsAndSaves() {
            when(userRepository.findById(10L)).thenReturn(Optional.of(testUser));
            when(preferenceRepository.findByUserIdAndNotificationType(10L, NotificationType.SHARED))
                    .thenReturn(Optional.empty());
            when(preferenceRepository.save(any())).thenAnswer(inv -> {
                NotificationPreference p = inv.getArgument(0);
                setId(p, 99L);
                return p;
            });

            NotificationPreferenceDTO result = service.updatePreference(10L, NotificationType.SHARED, true);

            ArgumentCaptor<NotificationPreference> captor = ArgumentCaptor.forClass(NotificationPreference.class);
            verify(preferenceRepository).save(captor.capture());
            NotificationPreference saved = captor.getValue();
            assertThat(saved.getNotificationType()).isEqualTo(NotificationType.SHARED);
            assertThat(saved.isEnabled()).isTrue();
        }

        @Test
        void updatePreference_userNotFound_throwsException() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updatePreference(999L, NotificationType.FRIENDSHIP, true))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("User not found");
        }
    }

    // ========== resetToDefaults ==========

    @Nested
    class ResetToDefaultsTests {

        @Test
        void resetToDefaults_deletesAndRecreatesPreferences() {
            when(userRepository.findById(10L)).thenReturn(Optional.of(testUser));
            when(preferenceRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

            service.resetToDefaults(10L);

            verify(preferenceRepository).deleteByUserId(10L);
            verify(preferenceRepository).saveAll(any());
        }

        @Test
        void resetToDefaults_createsAllNotificationTypes() {
            when(userRepository.findById(10L)).thenReturn(Optional.of(testUser));
            when(preferenceRepository.saveAll(any())).thenAnswer(inv -> {
                List<NotificationPreference> list = inv.getArgument(0);
                for (int i = 0; i < list.size(); i++) {
                    setId(list.get(i), (long) (i + 1));
                }
                return list;
            });

            service.resetToDefaults(10L);

            ArgumentCaptor<List<NotificationPreference>> captor = ArgumentCaptor.forClass(List.class);
            verify(preferenceRepository).saveAll(captor.capture());
            List<NotificationPreference> saved = captor.getValue();
            assertThat(saved).hasSize(NotificationType.values().length);
            for (NotificationPreference pref : saved) {
                assertThat(pref.isEnabled()).isTrue();
            }
        }
    }
}