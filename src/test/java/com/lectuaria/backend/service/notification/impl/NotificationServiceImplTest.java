package com.lectuaria.backend.service.notification.impl;

import com.lectuaria.backend.dto.notification.NotificationDTO;
import com.lectuaria.backend.model.auth.User;
import com.lectuaria.backend.model.auth.UserRole;
import com.lectuaria.backend.model.notification.Notification;
import com.lectuaria.backend.model.notification.NotificationPreference;
import com.lectuaria.backend.model.notification.NotificationType;
import com.lectuaria.backend.repository.auth.UserRepository;
import com.lectuaria.backend.repository.notification.NotificationPreferenceRepository;
import com.lectuaria.backend.repository.notification.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationPreferenceRepository preferenceRepository;

    @InjectMocks
    private NotificationServiceImpl service;

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

    // ========== createNotification ==========

    @Nested
    class CreateNotificationTests {

        @Test
        void createNotification_userEnabled_createsNotification() {
            when(userRepository.findById(10L)).thenReturn(Optional.of(testUser));
            NotificationPreference pref = new NotificationPreference(testUser, NotificationType.FRIENDSHIP, true);
            when(preferenceRepository.findByUserIdAndNotificationType(10L, NotificationType.FRIENDSHIP))
                    .thenReturn(Optional.of(pref));
            when(notificationRepository.save(any())).thenAnswer(inv -> {
                Notification n = inv.getArgument(0);
                setId(n, 1L);
                return n;
            });

            NotificationDTO result = service.createNotification(10L, NotificationType.FRIENDSHIP, "Test message", 100L);

            assertThat(result).isNotNull();
            assertThat(result.getNotificationType()).isEqualTo(NotificationType.FRIENDSHIP);
            assertThat(result.getMessage()).isEqualTo("Test message");
            assertThat(result.getReferenceId()).isEqualTo(100L);
            verify(notificationRepository).save(any());
        }

        @Test
        void createNotification_userDisabled_returnsNull() {
            when(userRepository.findById(10L)).thenReturn(Optional.of(testUser));
            NotificationPreference pref = new NotificationPreference(testUser, NotificationType.FRIENDSHIP, false);
            when(preferenceRepository.findByUserIdAndNotificationType(10L, NotificationType.FRIENDSHIP))
                    .thenReturn(Optional.of(pref));

            NotificationDTO result = service.createNotification(10L, NotificationType.FRIENDSHIP, "Test message", null);

            assertThat(result).isNull();
            verify(notificationRepository, never()).save(any());
        }

        @Test
        void createNotification_noPreference_returnsNull() {
            when(userRepository.findById(10L)).thenReturn(Optional.of(testUser));
            when(preferenceRepository.findByUserIdAndNotificationType(10L, NotificationType.FRIENDSHIP))
                    .thenReturn(Optional.empty());

            NotificationDTO result = service.createNotification(10L, NotificationType.FRIENDSHIP, "Test message", null);

            assertThat(result).isNull();
            verify(notificationRepository, never()).save(any());
        }

        @Test
        void createNotification_userNotFound_throwsException() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.createNotification(999L, NotificationType.FRIENDSHIP, "Test", null))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("User not found");
        }
    }

    // ========== createNotificationWithShareToken ==========

    @Nested
    class CreateNotificationWithShareTokenTests {

        @Test
        void createNotificationWithShareToken_createsWithToken() {
            when(userRepository.findById(10L)).thenReturn(Optional.of(testUser));
            NotificationPreference pref = new NotificationPreference(testUser, NotificationType.SHARED, true);
            when(preferenceRepository.findByUserIdAndNotificationType(10L, NotificationType.SHARED))
                    .thenReturn(Optional.of(pref));
            when(notificationRepository.save(any())).thenAnswer(inv -> {
                Notification n = inv.getArgument(0);
                setId(n, 1L);
                return n;
            });

            NotificationDTO result = service.createNotificationWithShareToken(
                    10L, NotificationType.SHARED, "Check this!", 100L, "share-token-123");

            assertThat(result).isNotNull();
            assertThat(result.getShareToken()).isEqualTo("share-token-123");
        }

        @Test
        void createNotificationWithShareToken_nullToken_works() {
            when(userRepository.findById(10L)).thenReturn(Optional.of(testUser));
            NotificationPreference pref = new NotificationPreference(testUser, NotificationType.SHARED, true);
            when(preferenceRepository.findByUserIdAndNotificationType(10L, NotificationType.SHARED))
                    .thenReturn(Optional.of(pref));
            when(notificationRepository.save(any())).thenAnswer(inv -> {
                Notification n = inv.getArgument(0);
                setId(n, 1L);
                return n;
            });

            NotificationDTO result = service.createNotificationWithShareToken(
                    10L, NotificationType.SHARED, "Check this!", 100L, null);

            assertThat(result).isNotNull();
            assertThat(result.getShareToken()).isNull();
        }
    }

    // ========== getUserNotifications ==========

    @Nested
    class GetUserNotificationsTests {

        @Test
        void getUserNotifications_all_returnsAll() {
            Notification n1 = new Notification(testUser, NotificationType.FRIENDSHIP, "Msg1", 1L);
            setId(n1, 1L);
            when(notificationRepository.findByUserIdOrderByCreatedAtDesc(10L)).thenReturn(List.of(n1));

            List<NotificationDTO> result = service.getUserNotifications(10L, false);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getNotificationType()).isEqualTo(NotificationType.FRIENDSHIP);
        }

        @Test
        void getUserNotifications_unreadOnly_returnsUnread() {
            Notification n1 = new Notification(testUser, NotificationType.FRIENDSHIP, "Msg1", 1L);
            setId(n1, 1L);
            when(notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(10L)).thenReturn(List.of(n1));

            List<NotificationDTO> result = service.getUserNotifications(10L, true);

            assertThat(result).hasSize(1);
            verify(notificationRepository).findByUserIdAndIsReadFalseOrderByCreatedAtDesc(10L);
        }

        @Test
        void getUserNotifications_empty_returnsEmpty() {
            when(notificationRepository.findByUserIdOrderByCreatedAtDesc(10L)).thenReturn(List.of());

            List<NotificationDTO> result = service.getUserNotifications(10L, null);

            assertThat(result).isEmpty();
        }
    }

    // ========== markAsRead ==========

    @Nested
    class MarkAsReadTests {

        @Test
        void markAsRead_existingNotification_marksAsRead() {
            Notification notification = new Notification(testUser, NotificationType.FRIENDSHIP, "Test", null);
            setId(notification, 1L);
            when(notificationRepository.findByIdAndUserId(1L, 10L)).thenReturn(Optional.of(notification));
            when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            NotificationDTO result = service.markAsRead(1L, 10L);

            assertThat(result.isRead()).isTrue();
        }

        @Test
        void markAsRead_notFound_throwsException() {
            when(notificationRepository.findByIdAndUserId(999L, 10L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.markAsRead(999L, 10L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Notification not found");
        }
    }

    // ========== markAllAsRead ==========

    @Nested
    class MarkAllAsReadTests {

        @Test
        void markAllAsRead_withUnread_marksAll() {
            Notification n1 = new Notification(testUser, NotificationType.FRIENDSHIP, "Msg1", null);
            setId(n1, 1L);
            Notification n2 = new Notification(testUser, NotificationType.SHARED, "Msg2", null);
            setId(n2, 2L);
            when(notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(10L)).thenReturn(List.of(n1, n2));

            service.markAllAsRead(10L);

            verify(notificationRepository).saveAll(any());
        }

        @Test
        void markAllAsRead_noUnread_doesNothing() {
            when(notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(10L)).thenReturn(List.of());

            service.markAllAsRead(10L);

            verify(notificationRepository, never()).saveAll(any());
        }
    }

    // ========== deleteNotification ==========

    @Nested
    class DeleteNotificationTests {

        @Test
        void deleteNotification_existing_deletes() {
            Notification notification = new Notification(testUser, NotificationType.FRIENDSHIP, "Test", null);
            setId(notification, 1L);
            when(notificationRepository.findByIdAndUserId(1L, 10L)).thenReturn(Optional.of(notification));

            service.deleteNotification(1L, 10L);

            verify(notificationRepository).delete(notification);
        }

        @Test
        void deleteNotification_notFound_throwsException() {
            when(notificationRepository.findByIdAndUserId(999L, 10L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteNotification(999L, 10L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Notification not found");
        }
    }

    // ========== deleteAllNotifications ==========

    @Nested
    class DeleteAllNotificationsTests {

        @Test
        void deleteAllNotifications_deletesAllForUser() {
            Notification n1 = new Notification(testUser, NotificationType.FRIENDSHIP, "Msg1", null);
            setId(n1, 1L);
            when(notificationRepository.findByUserIdOrderByCreatedAtDesc(10L)).thenReturn(List.of(n1));

            service.deleteAllNotifications(10L);

            verify(notificationRepository).deleteAll(List.of(n1));
        }
    }

    // ========== getUnreadCount ==========

    @Nested
    class GetUnreadCountTests {

        @Test
        void getUnreadCount_returnsCount() {
            when(notificationRepository.countByUserIdAndIsReadFalse(10L)).thenReturn(5L);

            Long result = service.getUnreadCount(10L);

            assertThat(result).isEqualTo(5L);
        }
    }
}