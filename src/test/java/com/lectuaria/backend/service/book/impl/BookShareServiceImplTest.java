package com.lectuaria.backend.service.book.impl;

import com.lectuaria.backend.dto.book.BookShareRequestDTO;
import com.lectuaria.backend.dto.book.BookShareResponseDTO;
import com.lectuaria.backend.dto.book.ShareResultDTO;
import com.lectuaria.backend.exception.ResourceNotFoundException;
import com.lectuaria.backend.model.auth.User;
import com.lectuaria.backend.model.auth.UserRole;
import com.lectuaria.backend.model.book.Book;
import com.lectuaria.backend.model.book.BookShare;
import com.lectuaria.backend.repository.auth.UserRepository;
import com.lectuaria.backend.repository.book.BookRepository;
import com.lectuaria.backend.repository.book.BookShareRepository;
import com.lectuaria.backend.service.notification.INotificationService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookShareServiceImplTest {

    @Mock
    private BookShareRepository bookShareRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private INotificationService notificationService;

    @InjectMocks
    private BookShareServiceImpl service;

    private User sender;
    private User receiver1;
    private User receiver2;
    private Book book;

    @BeforeEach
    void setUp() {
        sender = new User("Sender Name", "sender@test.com", "hash", UserRole.READER, "sender", null, null);
        setId(sender, 1L);

        receiver1 = new User("Friend One", "friend1@test.com", "hash", UserRole.READER, "friend1", null, null);
        setId(receiver1, 2L);

        receiver2 = new User("Friend Two", "friend2@test.com", "hash", UserRole.READER, "friend2", null, null);
        setId(receiver2, 3L);

        book = new Book();
        book.setTitle("Test Book");
        book.setIsbn(9781234567890L);
        setId(book, 100L);
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

    // ========== shareBookWithFriends ==========

    @Nested
    class ShareBookWithFriendsTests {

        @Test
        void shareBookWithFriends_bookNotFound_throwsException() {
            when(bookRepository.findById(100L)).thenReturn(Optional.empty());

            BookShareRequestDTO request = new BookShareRequestDTO(List.of(2L), null);

            assertThatThrownBy(() -> service.shareBookWithFriends(100L, request, sender))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Libro no encontrado");
        }

        @Test
        void shareBookWithFriends_friendNotFound_throwsException() {
            when(bookRepository.findById(100L)).thenReturn(Optional.of(book));
            when(userRepository.findById(2L)).thenReturn(Optional.empty());

            BookShareRequestDTO request = new BookShareRequestDTO(List.of(2L), null);

            assertThatThrownBy(() -> service.shareBookWithFriends(100L, request, sender))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Usuario no encontrado");
        }

        @Test
        void shareBookWithFriends_allSuccessful_returnsCorrectResult() {
            when(bookRepository.findById(100L)).thenReturn(Optional.of(book));
            when(userRepository.findById(2L)).thenReturn(Optional.of(receiver1));
            when(userRepository.findById(3L)).thenReturn(Optional.of(receiver2));
            when(bookShareRepository.existsBySenderAndReceiverAndBook(1L, 2L, 100L)).thenReturn(false);
            when(bookShareRepository.existsBySenderAndReceiverAndBook(1L, 3L, 100L)).thenReturn(false);
            when(bookShareRepository.save(any())).thenAnswer(inv -> {
                BookShare bs = inv.getArgument(0);
                setId(bs, 1L);
                return bs;
            });

            BookShareRequestDTO request = new BookShareRequestDTO(List.of(2L, 3L), "Check this out!");

            ShareResultDTO result = service.shareBookWithFriends(100L, request, sender);

            assertThat(result.getSuccessfulShares()).isEqualTo(2);
            assertThat(result.getFailedShares()).isEqualTo(0);
            assertThat(result.getErrorMessages()).isEmpty();
            verify(notificationService, times(2)).createNotification(any(), any(), any(), any());
        }

        @Test
        void shareBookWithFriends_alreadyShared_reportsFailure() {
            when(bookRepository.findById(100L)).thenReturn(Optional.of(book));
            when(userRepository.findById(2L)).thenReturn(Optional.of(receiver1));
            when(bookShareRepository.existsBySenderAndReceiverAndBook(1L, 2L, 100L)).thenReturn(true);

            BookShareRequestDTO request = new BookShareRequestDTO(List.of(2L), null);

            ShareResultDTO result = service.shareBookWithFriends(100L, request, sender);

            assertThat(result.getSuccessfulShares()).isEqualTo(0);
            assertThat(result.getFailedShares()).isEqualTo(1);
            assertThat(result.getErrorMessages()).hasSize(1);
            assertThat(result.getErrorMessages().get(0)).contains("ya ha sido compartido con Friend One");
        }

        @Test
        void shareBookWithFriends_withMessage_includesInNotification() {
            when(bookRepository.findById(100L)).thenReturn(Optional.of(book));
            when(userRepository.findById(2L)).thenReturn(Optional.of(receiver1));
            when(bookShareRepository.existsBySenderAndReceiverAndBook(1L, 2L, 100L)).thenReturn(false);
            when(bookShareRepository.save(any())).thenAnswer(inv -> {
                BookShare bs = inv.getArgument(0);
                setId(bs, 1L);
                return bs;
            });

            BookShareRequestDTO request = new BookShareRequestDTO(List.of(2L), "Must read!");

            service.shareBookWithFriends(100L, request, sender);

            verify(notificationService).createNotification(
                    eq(2L), any(), contains("Must read!"), eq(9781234567890L));
        }

        @Test
        void shareBookWithFriends_emptyMessage_usesDefaultMessage() {
            when(bookRepository.findById(100L)).thenReturn(Optional.of(book));
            when(userRepository.findById(2L)).thenReturn(Optional.of(receiver1));
            when(bookShareRepository.existsBySenderAndReceiverAndBook(1L, 2L, 100L)).thenReturn(false);
            when(bookShareRepository.save(any())).thenAnswer(inv -> {
                BookShare bs = inv.getArgument(0);
                setId(bs, 1L);
                return bs;
            });

            BookShareRequestDTO request = new BookShareRequestDTO(List.of(2L), "");

            service.shareBookWithFriends(100L, request, sender);

            verify(notificationService).createNotification(
                    eq(2L), any(), contains("te ha compartido este libro: Test Book"), eq(9781234567890L));
        }
    }

    // ========== getReceivedShares ==========

    @Nested
    class GetReceivedSharesTests {

        @Test
        void getReceivedShares_returnsSharesForUser() {
            BookShare share = new BookShare(sender, receiver1, book, "Hello");
            setId(share, 10L);
            when(bookShareRepository.findByReceiverId(2L)).thenReturn(List.of(share));

            List<BookShareResponseDTO> result = service.getReceivedShares(receiver1);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getSenderId()).isEqualTo(1L);
            assertThat(result.get(0).getReceiverId()).isEqualTo(2L);
            assertThat(result.get(0).getBookTitle()).isEqualTo("Test Book");
        }

        @Test
        void getReceivedShares_emptyList_returnsEmpty() {
            when(bookShareRepository.findByReceiverId(2L)).thenReturn(List.of());

            List<BookShareResponseDTO> result = service.getReceivedShares(receiver1);

            assertThat(result).isEmpty();
        }
    }

    // ========== getSentShares ==========

    @Nested
    class GetSentSharesTests {

        @Test
        void getSentShares_returnsSharesFromUser() {
            BookShare share = new BookShare(sender, receiver1, book, "Hello");
            setId(share, 10L);
            when(bookShareRepository.findBySenderId(1L)).thenReturn(List.of(share));

            List<BookShareResponseDTO> result = service.getSentShares(sender);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getSenderId()).isEqualTo(1L);
            assertThat(result.get(0).getBookTitle()).isEqualTo("Test Book");
        }
    }

    // ========== isBookSharedWithFriend ==========

    @Nested
    class IsBookSharedWithFriendTests {

        @Test
        void isBookSharedWithFriend_exists_returnsTrue() {
            when(bookShareRepository.existsBySenderAndReceiverAndBook(1L, 2L, 100L)).thenReturn(true);

            boolean result = service.isBookSharedWithFriend(1L, 2L, 100L);

            assertThat(result).isTrue();
        }

        @Test
        void isBookSharedWithFriend_notExists_returnsFalse() {
            when(bookShareRepository.existsBySenderAndReceiverAndBook(1L, 2L, 100L)).thenReturn(false);

            boolean result = service.isBookSharedWithFriend(1L, 2L, 100L);

            assertThat(result).isFalse();
        }
    }
}