package com.lectuaria.backend.service.friendship.impl;

import com.lectuaria.backend.dto.common.UserSearchResponseDTO;
import com.lectuaria.backend.model.auth.User;
import com.lectuaria.backend.model.auth.UserRole;
import com.lectuaria.backend.model.friendship.Friendship;
import com.lectuaria.backend.model.friendship.FriendshipRequest;
import com.lectuaria.backend.model.friendship.FriendshipRequestStatus;
import com.lectuaria.backend.model.notification.NotificationType;
import com.lectuaria.backend.repository.auth.UserRepository;
import com.lectuaria.backend.repository.friendship.FriendshipRepository;
import com.lectuaria.backend.repository.friendship.FriendshipRequestRepository;
import com.lectuaria.backend.service.notification.INotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FriendshipServiceImplTest {

    @Mock private FriendshipRepository friendshipRepository;
    @Mock private FriendshipRequestRepository requestRepository;
    @Mock private UserRepository userRepository;
    @Mock private INotificationService notificationService;

    private FriendshipServiceImpl service;

    private User user1;
    private User user2;
    private User user3;

    private static final Long USER1_ID = 1L;
    private static final Long USER2_ID = 2L;
    private static final Long USER3_ID = 3L;
    private static final Long REQUEST_ID = 100L;
    private static final Long FRIENDSHIP_ID = 200L;

    @BeforeEach
    void setUp() {
        service = new FriendshipServiceImpl(friendshipRepository, requestRepository, userRepository, notificationService);

        user1 = new User("Usuario Uno", "user1@test.com", "hash", UserRole.READER, "usuario1", null, null);
        setId(user1, USER1_ID);

        user2 = new User("Usuario Dos", "user2@test.com", "hash", UserRole.READER, "usuario2", null, null);
        setId(user2, USER2_ID);

        user3 = new User("Usuario Tres", "user3@test.com", "hash", UserRole.READER, "usuario3", null, null);
        setId(user3, USER3_ID);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // searchReaders
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("searchReaders")
    class SearchReadersTests {

        @Test
        @DisplayName("returns empty list for null query")
        void returnsEmptyForNullQuery() {
            List<UserSearchResponseDTO> result = service.searchReaders(null, user1);
            assertThat(result).isEmpty();
            verifyNoInteractions(userRepository);
        }

        @Test
        @DisplayName("returns empty list for blank query")
        void returnsEmptyForBlankQuery() {
            List<UserSearchResponseDTO> result = service.searchReaders("   ", user1);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("searches readers excluding current user")
        void searchesReadersExcludingSelf() {
            when(userRepository.searchReaders("gabriel", USER1_ID, UserRole.READER))
                    .thenReturn(List.of(user2));

            List<UserSearchResponseDTO> result = service.searchReaders("gabriel", user1);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(USER2_ID);
            verify(userRepository).searchReaders("gabriel", USER1_ID, UserRole.READER);
        }

        @Test
        @DisplayName("searches readers publicly when currentUser is null")
        void searchesPubliclyWithoutUser() {
            when(userRepository.searchReadersPublic("gabriel", UserRole.READER))
                    .thenReturn(List.of(user1, user2));

            List<UserSearchResponseDTO> result = service.searchReaders("gabriel", null);

            assertThat(result).hasSize(2);
            verify(userRepository).searchReadersPublic("gabriel", UserRole.READER);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // sendFriendshipRequest
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("sendFriendshipRequest")
    class SendFriendshipRequestTests {

        @Test
        @DisplayName("sends request successfully")
        void sendsRequestSuccessfully() {
            FriendshipRequest savedRequest = new FriendshipRequest(user1, user2);
            setId(savedRequest, REQUEST_ID);

            when(userRepository.findById(USER2_ID)).thenReturn(Optional.of(user2));
            when(friendshipRepository.existsByUsers(USER1_ID, USER2_ID)).thenReturn(false);
            when(requestRepository.hasPendingRequestBetween(USER1_ID, USER2_ID)).thenReturn(false);
            when(requestRepository.save(any(FriendshipRequest.class))).thenReturn(savedRequest);

            service.sendFriendshipRequest(USER2_ID, user1);

            ArgumentCaptor<FriendshipRequest> captor = ArgumentCaptor.forClass(FriendshipRequest.class);
            verify(requestRepository).save(captor.capture());
            assertThat(captor.getValue().getSender()).isEqualTo(user1);
            assertThat(captor.getValue().getReceiver()).isEqualTo(user2);

            verify(notificationService).createNotification(
                    eq(USER2_ID),
                    eq(NotificationType.FRIENDSHIP),
                    contains("solicitud de amistad"),
                    eq(REQUEST_ID)
            );
        }

        @Test
        @DisplayName("throws when sending to self")
        void throwsWhenSendingToSelf() {
            assertThatThrownBy(() -> service.sendFriendshipRequest(USER1_ID, user1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("No puedes enviarte una solicitud a ti mismo");
        }

        @Test
        @DisplayName("throws when receiver not found")
        void throwsWhenReceiverNotFound() {
            when(userRepository.findById(USER2_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.sendFriendshipRequest(USER2_ID, user1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Usuario no encontrado");
        }

        @Test
        @DisplayName("throws when already friends")
        void throwsWhenAlreadyFriends() {
            when(userRepository.findById(USER2_ID)).thenReturn(Optional.of(user2));
            when(friendshipRepository.existsByUsers(USER1_ID, USER2_ID)).thenReturn(true);

            assertThatThrownBy(() -> service.sendFriendshipRequest(USER2_ID, user1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Ya son amigos");
        }

        @Test
        @DisplayName("throws when pending request exists")
        void throwsWhenPendingRequestExists() {
            when(userRepository.findById(USER2_ID)).thenReturn(Optional.of(user2));
            when(friendshipRepository.existsByUsers(USER1_ID, USER2_ID)).thenReturn(false);
            when(requestRepository.hasPendingRequestBetween(USER1_ID, USER2_ID)).thenReturn(true);

            assertThatThrownBy(() -> service.sendFriendshipRequest(USER2_ID, user1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("solicitud de amistad pendiente");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // acceptFriendshipRequest
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("acceptFriendshipRequest")
    class AcceptFriendshipRequestTests {

        @Test
        @DisplayName("accepts request and creates friendship")
        void acceptsRequestAndCreatesFriendship() {
            FriendshipRequest request = new FriendshipRequest(user1, user2);
            setId(request, REQUEST_ID);

            Friendship savedFriendship = new Friendship(user1, user2);
            setId(savedFriendship, FRIENDSHIP_ID);

            when(requestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(request));
            when(requestRepository.save(any(FriendshipRequest.class))).thenReturn(request);
            when(friendshipRepository.save(any(Friendship.class))).thenReturn(savedFriendship);

            service.acceptFriendshipRequest(REQUEST_ID, user2);

            verify(requestRepository).save(request);
            assertThat(request.getStatus()).isEqualTo(FriendshipRequestStatus.ACCEPTED);

            verify(friendshipRepository).save(any(Friendship.class));

            verify(notificationService).createNotification(
                    eq(USER1_ID),
                    eq(NotificationType.FRIENDSHIP),
                    contains("ha aceptado tu solicitud"),
                    eq(FRIENDSHIP_ID)
            );
        }

        @Test
        @DisplayName("throws when request not found")
        void throwsWhenRequestNotFound() {
            when(requestRepository.findById(REQUEST_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.acceptFriendshipRequest(REQUEST_ID, user2))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Solicitud no encontrada");
        }

        @Test
        @DisplayName("throws when not the receiver")
        void throwsWhenNotReceiver() {
            FriendshipRequest request = new FriendshipRequest(user1, user2);
            setId(request, REQUEST_ID);

            when(requestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(request));

            assertThatThrownBy(() -> service.acceptFriendshipRequest(REQUEST_ID, user1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("No puedes aceptar esta solicitud");
        }

        @Test
        @DisplayName("throws when request is not pending")
        void throwsWhenRequestNotPending() {
            FriendshipRequest request = new FriendshipRequest(user1, user2);
            setId(request, REQUEST_ID);
            request.setStatus(FriendshipRequestStatus.ACCEPTED);

            when(requestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(request));

            assertThatThrownBy(() -> service.acceptFriendshipRequest(REQUEST_ID, user2))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("no está pendiente");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // rejectFriendshipRequest
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("rejectFriendshipRequest")
    class RejectFriendshipRequestTests {

        @Test
        @DisplayName("rejects request successfully")
        void rejectsRequestSuccessfully() {
            FriendshipRequest request = new FriendshipRequest(user1, user2);
            setId(request, REQUEST_ID);

            when(requestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(request));

            service.rejectFriendshipRequest(REQUEST_ID, user2);

            verify(requestRepository).delete(request);
        }

        @Test
        @DisplayName("throws when not the receiver")
        void throwsWhenNotReceiver() {
            FriendshipRequest request = new FriendshipRequest(user1, user2);
            setId(request, REQUEST_ID);

            when(requestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(request));

            assertThatThrownBy(() -> service.rejectFriendshipRequest(REQUEST_ID, user1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("No puedes rechazar esta solicitud");
        }

        @Test
        @DisplayName("throws when request not pending")
        void throwsWhenRequestNotPending() {
            FriendshipRequest request = new FriendshipRequest(user1, user2);
            setId(request, REQUEST_ID);
            request.setStatus(FriendshipRequestStatus.ACCEPTED);

            when(requestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(request));

            assertThatThrownBy(() -> service.rejectFriendshipRequest(REQUEST_ID, user2))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("no está pendiente");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // cancelFriendshipRequest
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("cancelFriendshipRequest")
    class CancelFriendshipRequestTests {

        @Test
        @DisplayName("cancels own request successfully")
        void cancelsOwnRequestSuccessfully() {
            FriendshipRequest request = new FriendshipRequest(user1, user2);
            setId(request, REQUEST_ID);

            when(requestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(request));

            service.cancelFriendshipRequest(REQUEST_ID, user1);

            verify(requestRepository).delete(request);
        }

        @Test
        @DisplayName("throws when not the sender")
        void throwsWhenNotSender() {
            FriendshipRequest request = new FriendshipRequest(user1, user2);
            setId(request, REQUEST_ID);

            when(requestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(request));

            assertThatThrownBy(() -> service.cancelFriendshipRequest(REQUEST_ID, user2))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("No puedes cancelar esta solicitud");
        }

        @Test
        @DisplayName("throws when request not pending")
        void throwsWhenRequestNotPending() {
            FriendshipRequest request = new FriendshipRequest(user1, user2);
            setId(request, REQUEST_ID);
            request.setStatus(FriendshipRequestStatus.REJECTED);

            when(requestRepository.findById(REQUEST_ID)).thenReturn(Optional.of(request));

            assertThatThrownBy(() -> service.cancelFriendshipRequest(REQUEST_ID, user1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("no está pendiente");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // removeFriendship
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("removeFriendship")
    class RemoveFriendshipTests {

        @Test
        @DisplayName("removes friendship and all requests between users")
        void removesFriendshipAndRequests() {
            Friendship friendship = new Friendship(user1, user2);
            setId(friendship, FRIENDSHIP_ID);

            when(friendshipRepository.findByUsers(USER1_ID, USER2_ID)).thenReturn(Optional.of(friendship));

            service.removeFriendship(USER2_ID, user1);

            verify(friendshipRepository).delete(friendship);
            verify(requestRepository).deleteAllRequestsBetween(USER1_ID, USER2_ID);
        }

        @Test
        @DisplayName("throws when friendship not found")
        void throwsWhenNotFound() {
            when(friendshipRepository.findByUsers(USER1_ID, USER2_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.removeFriendship(USER2_ID, user1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Amistad no encontrada");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getFriends
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getFriends")
    class GetFriendsTests {

        @Test
        @DisplayName("returns list of friends")
        void returnsListOfFriends() {
            Friendship friendship = new Friendship(user1, user2);
            setId(friendship, FRIENDSHIP_ID);

            when(friendshipRepository.findFriendsByUserId(USER1_ID)).thenReturn(List.of(friendship));
            when(friendshipRepository.existsByUsers(USER1_ID, USER2_ID)).thenReturn(true);

            List<UserSearchResponseDTO> result = service.getFriends(user1);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(USER2_ID);
            assertThat(result.get(0).getFriendshipStatus()).isEqualTo("friends");
        }

        @Test
        @DisplayName("returns empty list when no friends")
        void returnsEmptyList() {
            when(friendshipRepository.findFriendsByUserId(USER1_ID)).thenReturn(List.of());

            List<UserSearchResponseDTO> result = service.getFriends(user1);

            assertThat(result).isEmpty();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getPendingRequests
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getPendingRequests")
    class GetPendingRequestsTests {

        @Test
        @DisplayName("returns pending received requests")
        void returnsPendingReceivedRequests() {
            FriendshipRequest request = new FriendshipRequest(user1, user2);
            setId(request, REQUEST_ID);

            when(requestRepository.findByReceiverIdAndStatus(USER2_ID, FriendshipRequestStatus.PENDING))
                    .thenReturn(List.of(request));
            when(friendshipRepository.existsByUsers(USER2_ID, USER1_ID)).thenReturn(false);
            when(requestRepository.findPendingRequestBetween(USER2_ID, USER1_ID)).thenReturn(Optional.of(request));

            List<UserSearchResponseDTO> result = service.getPendingRequests(user2);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getFriendshipRequestId()).isEqualTo(REQUEST_ID);
            assertThat(result.get(0).getFriendshipStatus()).isEqualTo("pending_received");
        }

        @Test
        @DisplayName("returns empty list when no pending requests")
        void returnsEmptyList() {
            when(requestRepository.findByReceiverIdAndStatus(USER2_ID, FriendshipRequestStatus.PENDING))
                    .thenReturn(List.of());

            List<UserSearchResponseDTO> result = service.getPendingRequests(user2);

            assertThat(result).isEmpty();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // mapToDTO (friendship status variations)
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("mapToDTO friendship status")
    class MapToDtoTests {

        @Test
        @DisplayName("marks as friends when friendship exists")
        void marksAsFriends() {
            when(userRepository.searchReaders("dos", USER1_ID, UserRole.READER)).thenReturn(List.of(user2));
            when(friendshipRepository.existsByUsers(USER1_ID, USER2_ID)).thenReturn(true);

            List<UserSearchResponseDTO> result = service.searchReaders("dos", user1);

            assertThat(result.get(0).getFriendshipStatus()).isEqualTo("friends");
        }

        @Test
        @DisplayName("marks as pending_sent when user sent request")
        void marksAsPendingSent() {
            FriendshipRequest pendingRequest = new FriendshipRequest(user1, user2);
            setId(pendingRequest, REQUEST_ID);

            when(userRepository.searchReaders("dos", USER1_ID, UserRole.READER)).thenReturn(List.of(user2));
            when(friendshipRepository.existsByUsers(USER1_ID, USER2_ID)).thenReturn(false);
            when(requestRepository.findPendingRequestBetween(USER1_ID, USER2_ID)).thenReturn(Optional.of(pendingRequest));

            List<UserSearchResponseDTO> result = service.searchReaders("dos", user1);

            assertThat(result.get(0).getFriendshipStatus()).isEqualTo("pending_sent");
            assertThat(result.get(0).getFriendshipRequestId()).isEqualTo(REQUEST_ID);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper
    // ─────────────────────────────────────────────────────────────────────────

    private void setId(Object entity, Long id) {
        try {
            var field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}