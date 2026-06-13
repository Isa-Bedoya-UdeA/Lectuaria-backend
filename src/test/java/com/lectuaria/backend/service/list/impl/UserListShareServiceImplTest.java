package com.lectuaria.backend.service.list.impl;

import com.lectuaria.backend.dto.book.ShareResultDTO;
import com.lectuaria.backend.dto.list.UserListShareDTO;
import com.lectuaria.backend.model.auth.User;
import com.lectuaria.backend.model.auth.UserRole;
import com.lectuaria.backend.model.list.ListType;
import com.lectuaria.backend.model.list.ListVisibility;
import com.lectuaria.backend.model.list.UserList;
import com.lectuaria.backend.model.list.UserListShare;
import com.lectuaria.backend.model.notification.NotificationType;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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

        @Test
        void shareListWithMultipleFriends_publicList_persistsPublicTokenAndPassesItToNotification() {
            // Lista PUBLIC sin token previo: el service debe generarlo
            // y persistirlo en la lista antes de crear la notificacion.
            UserList publicList = createList(owner, "Publica", "Desc", ListVisibility.PUBLIC);
            when(listRepository.findById(publicList.getId())).thenReturn(Optional.of(publicList));
            when(shareRepository.findByListIdAndReceiverId(publicList.getId(), 2L)).thenReturn(Optional.empty());
            when(shareRepository.save(any())).thenAnswer(inv -> {
                UserListShare s = inv.getArgument(0);
                setId(s, 1L);
                return s;
            });

            ShareResultDTO result = service.shareListWithMultipleFriends(publicList.getId(), List.of(2L), null, 1L);

            assertThat(result.getSuccessfulShares()).isEqualTo(1);
            // La lista debe haberse persistido con un nuevo publicToken
            assertThat(publicList.getPublicToken()).isNotNull().isNotBlank();
            // La notificacion recibe ese mismo token (NO el id interno)
            verify(notificationService).createNotificationWithShareToken(
                    eq(2L), eq(NotificationType.SHARED), anyString(), eq(publicList.getId()),
                    eq(publicList.getPublicToken()));
        }

        @Test
        void shareListWithMultipleFriends_listedList_passesIndividualShareToken() {
            // Para LISTED cada share individual tambien debe quedar con
            // su propio token (ademas del publicToken de la lista).
            when(shareRepository.findByListIdAndReceiverId(100L, 2L)).thenReturn(Optional.empty());
            when(shareRepository.save(any())).thenAnswer(inv -> {
                UserListShare s = inv.getArgument(0);
                setId(s, 1L);
                return s;
            });

            service.shareListWithMultipleFriends(100L, List.of(2L), null, 1L);

            // Capturamos el share que se guardó y validamos su token
            org.mockito.ArgumentCaptor<UserListShare> captor =
                    org.mockito.ArgumentCaptor.forClass(UserListShare.class);
            verify(shareRepository).save(captor.capture());
            assertThat(captor.getValue().getShareToken()).isNotNull().isNotBlank();
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

        @Test
        void getSharedLists_publicList_returnsListPublicToken() {
            // Caso reportado por QA: lista PUBLIC, sin shareToken individual.
            // El DTO debe llevar el publicToken de user_list para que el
            // front navegue a /shared/{token} y no al fallback /lists/{id}.
            UserList publicList = createList(owner, "Publica", "Desc", ListVisibility.PUBLIC);
            publicList.setPublicToken("list-public-token");
            UserListShare share = new UserListShare(publicList, owner, friend);
            setId(share, 1L);
            when(shareRepository.findByReceiverIdAndIsActiveTrue(2L)).thenReturn(List.of(share));

            List<UserListShareDTO> result = service.getSharedLists(2L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getPublicToken()).isEqualTo("list-public-token");
        }

        @Test
        void getSharedLists_listedList_prefersShareToken() {
            // Lista LISTED: si el share tiene shareToken (compatibilidad
            // hacia atras), ese gana; sino, cae al publicToken de la lista.
            UserList listedList = createList(owner, "Listada", "Desc", ListVisibility.LISTED);
            listedList.setPublicToken("list-public-token");
            UserListShare share = new UserListShare(listedList, owner, friend);
            share.setShareToken("share-token-individual");
            setId(share, 1L);
            when(shareRepository.findByReceiverIdAndIsActiveTrue(2L)).thenReturn(List.of(share));

            List<UserListShareDTO> result = service.getSharedLists(2L);

            assertThat(result.get(0).getPublicToken()).isEqualTo("share-token-individual");
        }

        @Test
        void getSharedLists_listedListWithoutShareToken_fallsBackToListPublicToken() {
            UserList listedList = createList(owner, "Listada", "Desc", ListVisibility.LISTED);
            listedList.setPublicToken("list-public-token");
            UserListShare share = new UserListShare(listedList, owner, friend);
            // share.getShareToken() queda null
            setId(share, 1L);
            when(shareRepository.findByReceiverIdAndIsActiveTrue(2L)).thenReturn(List.of(share));

            List<UserListShareDTO> result = service.getSharedLists(2L);

            assertThat(result.get(0).getPublicToken()).isEqualTo("list-public-token");
        }

        @Test
        void getSharedLists_privateList_returnsNullToken() {
            // Defensa: si por algun motivo una lista PRIVATE llegara a estar
            // compartida (no deberia), el DTO no debe filtrar su token.
            UserList privateList = createList(owner, "Privada", "Desc", ListVisibility.PRIVATE);
            privateList.setPublicToken("stale-private-token");
            UserListShare share = new UserListShare(privateList, owner, friend);
            setId(share, 1L);
            when(shareRepository.findByReceiverIdAndIsActiveTrue(2L)).thenReturn(List.of(share));

            List<UserListShareDTO> result = service.getSharedLists(2L);

            assertThat(result.get(0).getPublicToken()).isNull();
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

        @Test
        void getListByPublicToken_publicListToken_returnsListFromPublicToken() {
            // Caso real: la lista es PUBLIC, el token que llega es el
            // publicToken de user_list (no el shareToken de un share).
            // No hay share individual (no se compartio con nadie especifico).
            UserList publicList = createList(owner, "Publica", "Desc", ListVisibility.PUBLIC);
            publicList.setPublicToken("list-public-token");
            when(shareRepository.findByShareTokenAndIsActiveTrue("list-public-token")).thenReturn(Optional.empty());
            when(listRepository.findByPublicToken("list-public-token")).thenReturn(Optional.of(publicList));
            when(listBookRepository.findByUserListIdOrderByAddedAtDesc(publicList.getId())).thenReturn(List.of());

            UserListShareDTO result = service.getListByPublicToken("list-public-token");

            assertThat(result).isNotNull();
            assertThat(result.getListName()).isEqualTo("Publica");
            assertThat(result.getOwnerId()).isEqualTo(owner.getId());
            assertThat(result.getPublicToken()).isEqualTo("list-public-token");
            // No hay share, asi que estos campos son null
            assertThat(result.getId()).isNull();
            assertThat(result.getReceiverId()).isNull();
        }

        @Test
        void getListByPublicToken_listedListToken_returnsListFromPublicToken() {
            // Caso LISTED con publicToken en la lista (mismo flujo que PUBLIC)
            UserList listedList = createList(owner, "Listada", "Desc", ListVisibility.LISTED);
            listedList.setPublicToken("listed-public-token");
            when(shareRepository.findByShareTokenAndIsActiveTrue("listed-public-token")).thenReturn(Optional.empty());
            when(listRepository.findByPublicToken("listed-public-token")).thenReturn(Optional.of(listedList));
            when(listBookRepository.findByUserListIdOrderByAddedAtDesc(listedList.getId())).thenReturn(List.of());

            UserListShareDTO result = service.getListByPublicToken("listed-public-token");

            assertThat(result).isNotNull();
            assertThat(result.getListName()).isEqualTo("Listada");
        }

        @Test
        void getListByPublicToken_privateListToken_throws() {
            // Defensa: si por algun motivo llega un token que pertenece a
            // una lista privada (no deberia existir en BD), lo rechazamos.
            UserList privateList = createList(owner, "Privada", "Desc", ListVisibility.PRIVATE);
            privateList.setPublicToken("stale-private-token");
            when(shareRepository.findByShareTokenAndIsActiveTrue("stale-private-token")).thenReturn(Optional.empty());
            when(listRepository.findByPublicToken("stale-private-token")).thenReturn(Optional.of(privateList));

            assertThatThrownBy(() -> service.getListByPublicToken("stale-private-token"))
                    .isInstanceOf(com.lectuaria.backend.exception.list.InvalidShareTokenException.class);
        }
    }
}