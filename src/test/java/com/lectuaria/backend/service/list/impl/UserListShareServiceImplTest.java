package com.lectuaria.backend.service.list.impl;

import com.lectuaria.backend.dto.book.ShareResultDTO;
import com.lectuaria.backend.dto.list.UserListShareDTO;
import com.lectuaria.backend.model.auth.User;
import com.lectuaria.backend.model.auth.UserRole;
import com.lectuaria.backend.model.list.ListType;
import com.lectuaria.backend.model.list.ListVisibility;
import com.lectuaria.backend.model.list.UserList;
import com.lectuaria.backend.model.list.UserListShare;
import com.lectuaria.backend.repository.auth.UserRepository;
import com.lectuaria.backend.repository.list.UserListBookRepository;
import com.lectuaria.backend.repository.list.UserListRepository;
import com.lectuaria.backend.repository.list.UserListShareRepository;
import com.lectuaria.backend.repository.notification.NotificationRepository;
import com.lectuaria.backend.service.notification.INotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserListShareServiceImplTest {

    @Mock
    private UserListShareRepository shareRepository;

    @Mock
    private UserListRepository listRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserListBookRepository listBookRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private INotificationService notificationService;

    @InjectMocks
    private UserListShareServiceImpl service;

    private long nextId = 100L;
    private User owner;
    private User friend;
    private UserList list;

    @BeforeEach
    void setUp() {
        owner = new User("Owner", "owner@test.com", "hash", UserRole.READER, "owner", null, null);
        setId(owner, 1L);

        friend = new User("Friend", "friend@test.com", "hash", UserRole.READER, "friend", null, null);
        setId(friend, 2L);

        list = createList(owner, "My List", "Description", ListVisibility.PUBLIC);
        when(listRepository.findById(list.getId())).thenReturn(Optional.of(list));
        when(userRepository.findById(2L)).thenReturn(Optional.of(friend));
    }

    private UserList createList(User owner, String name, String description, ListVisibility visibility) {
        UserList list = new UserList(owner, name, description, ListType.CUSTOM, visibility);
        setId(list, nextId++);
        return list;
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

    // ========== shareListWithFriends ==========

    @Nested
    class ShareListWithFriendsTests {

        @Test
        void shareListWithFriends_validRequest_createsShare() {
            when(shareRepository.findByListIdAndReceiverId(100L, 2L)).thenReturn(Optional.empty());
            when(shareRepository.save(any())).thenAnswer(inv -> {
                UserListShare s = inv.getArgument(0);
                setId(s, 1L);
                return s;
            });

            UserListShareDTO result = service.shareListWithFriends(100L, List.of(2L), 1L);

            assertThat(result).isNotNull();
            assertThat(result.getListId()).isEqualTo(100L);
        }

        @Test
        void shareListWithFriends_listNotFound_throws() {
            when(listRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.shareListWithFriends(999L, List.of(2L), 1L))
                    .isInstanceOf(com.lectuaria.backend.exception.ResourceNotFoundException.class)
                    .hasMessageContaining("Lista no encontrada");
        }

        @Test
        void shareListWithFriends_notOwner_throws() {
            when(listRepository.findById(100L)).thenReturn(Optional.of(list));

            assertThatThrownBy(() -> service.shareListWithFriends(100L, List.of(2L), 999L))
                    .isInstanceOf(com.lectuaria.backend.exception.ForbiddenException.class);
        }

@Test
        void shareListWithFriends_privateList_throws() {
            UserList privateList = createList(owner, "Private", "Desc", ListVisibility.PRIVATE);
            when(listRepository.findById(privateList.getId())).thenReturn(Optional.of(privateList));

            assertThatThrownBy(() -> service.shareListWithFriends(privateList.getId(), List.of(2L), 1L))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    // ========== shareListWithMultipleFriends ==========

    @Nested
    class ShareListWithMultipleFriendsTests {

        @Test
        void shareListWithMultipleFriends_allSuccessful_returnsSuccess() {
            when(shareRepository.findByListIdAndReceiverId(100L, 2L)).thenReturn(Optional.empty());
            when(shareRepository.save(any())).thenAnswer(inv -> {
                UserListShare s = inv.getArgument(0);
                setId(s, 1L);
                return s;
            });

            ShareResultDTO result = service.shareListWithMultipleFriends(100L, List.of(2L), "Check this!", 1L);

            assertThat(result.getSuccessfulShares()).isEqualTo(1);
            assertThat(result.getFailedShares()).isEqualTo(0);
        }

        @Test
        void shareListWithMultipleFriends_alreadyShared_reportsFailure() {
            when(shareRepository.findByListIdAndReceiverId(100L, 2L)).thenReturn(Optional.of(new UserListShare()));

            ShareResultDTO result = service.shareListWithMultipleFriends(100L, List.of(2L), null, 1L);

            assertThat(result.getSuccessfulShares()).isEqualTo(0);
            assertThat(result.getFailedShares()).isEqualTo(1);
            assertThat(result.getErrorMessages()).isNotEmpty();
        }

        @Test
        void shareListWithMultipleFriends_skipsOwnId() {
            ShareResultDTO result = service.shareListWithMultipleFriends(100L, List.of(1L, 2L), null, 1L);

            verify(userRepository, times(1)).findById(2L);
        }
    }

    // ========== revokeShare ==========

    @Nested
    class RevokeShareTests {

        @Test
        void revokeShare_validOwner_revokes() {
            UserListShare share = new UserListShare(list, owner, friend);
            setId(share, 1L);
            when(shareRepository.findById(1L)).thenReturn(Optional.of(share));
            when(shareRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.revokeShare(1L, 1L);

            verify(shareRepository).save(any());
        }

        @Test
        void revokeShare_notOwner_throws() {
            UserListShare share = new UserListShare(list, owner, friend);
            setId(share, 1L);
            when(shareRepository.findById(1L)).thenReturn(Optional.of(share));

            assertThatThrownBy(() -> service.revokeShare(1L, 999L))
                    .isInstanceOf(com.lectuaria.backend.exception.ForbiddenException.class);
        }

        @Test
        void revokeShare_notFound_throws() {
            when(shareRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.revokeShare(999L, 1L))
                    .isInstanceOf(com.lectuaria.backend.exception.ResourceNotFoundException.class)
                    .hasMessageContaining("Share no encontrado");
        }
    }

    // ========== getSharedLists ==========

    @Nested
    class GetSharedListsTests {

        @Test
        void getSharedLists_returnsShares() {
            UserListShare share = new UserListShare(list, owner, friend);
            setId(share, 1L);
            when(shareRepository.findByReceiverIdAndIsActiveTrue(2L)).thenReturn(List.of(share));

            List<UserListShareDTO> result = service.getSharedLists(2L);

            assertThat(result).hasSize(1);
        }

        @Test
        void getSharedLists_empty_returnsEmpty() {
            when(shareRepository.findByReceiverIdAndIsActiveTrue(2L)).thenReturn(List.of());

            List<UserListShareDTO> result = service.getSharedLists(2L);

            assertThat(result).isEmpty();
        }
    }

    // ========== getListByPublicToken ==========

    @Nested
    class GetListByPublicTokenTests {

        @Test
        void getListByPublicToken_validToken_returnsList() {
            UserListShare share = new UserListShare(list, owner, friend);
            setId(share, 1L);
            when(shareRepository.findByShareTokenAndIsActiveTrue("valid-token")).thenReturn(Optional.of(share));

            UserListShareDTO result = service.getListByPublicToken("valid-token");

            assertThat(result).isNotNull();
        }

        @Test
        void getListByPublicToken_invalidToken_throws() {
            when(shareRepository.findByShareTokenAndIsActiveTrue("invalid-token")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getListByPublicToken("invalid-token"))
                    .isInstanceOf(com.lectuaria.backend.exception.list.InvalidShareTokenException.class);
        }
    }
}