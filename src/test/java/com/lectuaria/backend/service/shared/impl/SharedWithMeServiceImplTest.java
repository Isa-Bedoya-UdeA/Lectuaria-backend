package com.lectuaria.backend.service.shared.impl;

import com.lectuaria.backend.dto.list.UserListShareDTO;
import com.lectuaria.backend.dto.shared.SharedBookDTO;
import com.lectuaria.backend.model.auth.User;
import com.lectuaria.backend.model.auth.UserRole;
import com.lectuaria.backend.model.book.Book;
import com.lectuaria.backend.model.book.BookShare;
import com.lectuaria.backend.model.list.UserList;
import com.lectuaria.backend.repository.book.BookShareRepository;
import com.lectuaria.backend.service.list.IUserListShareService;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SharedWithMeServiceImplTest {

    @Mock
    private IUserListShareService userListShareService;

    @Mock
    private BookShareRepository bookShareRepository;

    @InjectMocks
    private SharedWithMeServiceImpl service;

    private User receiver;
    private User sender;
    private Book book;

    @BeforeEach
    void setUp() {
        receiver = new User("Receiver", "receiver@test.com", "hash", UserRole.READER, "receiver", null, null);
        setId(receiver, 10L);

        sender = new User("Sender", "sender@test.com", "hash", UserRole.READER, "sender", null, null);
        setId(sender, 20L);

        book = new Book();
        book.setTitle("Test Book");
        book.setIsbn(9781234567890L);
        book.setCoverUrl("http://example.com/cover.jpg");
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

    // ========== getSharedLists ==========

    @Nested
    class GetSharedListsTests {

        @Test
        void getSharedLists_returnsSharedLists() {
            UserListShareDTO mockList = new UserListShareDTO();
            when(userListShareService.getSharedLists(10L)).thenReturn(List.of(mockList));

            List<UserListShareDTO> result = service.getSharedLists(10L);

            assertThat(result).hasSize(1);
        }

        @Test
        void getSharedLists_empty_returnsEmpty() {
            when(userListShareService.getSharedLists(10L)).thenReturn(List.of());

            List<UserListShareDTO> result = service.getSharedLists(10L);

            assertThat(result).isEmpty();
        }
    }

    // ========== getSharedBooks ==========

    @Nested
    class GetSharedBooksTests {

        @Test
        void getSharedBooks_returnsBooks() {
            BookShare share = new BookShare(sender, receiver, book, "Check this!");
            setId(share, 1L);
            when(bookShareRepository.findByReceiverId(10L)).thenReturn(List.of(share));

            List<SharedBookDTO> result = service.getSharedBooks(10L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTitle()).isEqualTo("Test Book");
            assertThat(result.get(0).getIsbn()).isEqualTo("9781234567890");
            assertThat(result.get(0).getOwnerName()).isEqualTo("Sender");
            assertThat(result.get(0).getMessage()).isEqualTo("Check this!");
        }

        @Test
        void getSharedBooks_nullMessage_emptyString() {
            BookShare share = new BookShare(sender, receiver, book, null);
            setId(share, 1L);
            when(bookShareRepository.findByReceiverId(10L)).thenReturn(List.of(share));

            List<SharedBookDTO> result = service.getSharedBooks(10L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getMessage()).isEqualTo("");
        }

        @Test
        void getSharedBooks_empty_returnsEmpty() {
            when(bookShareRepository.findByReceiverId(10L)).thenReturn(List.of());

            List<SharedBookDTO> result = service.getSharedBooks(10L);

            assertThat(result).isEmpty();
        }

        @Test
        void getSharedBooks_nullBook_filtersOut() {
            BookShare share = new BookShare(sender, receiver, null, null);
            setId(share, 1L);
            when(bookShareRepository.findByReceiverId(10L)).thenReturn(List.of(share));

            List<SharedBookDTO> result = service.getSharedBooks(10L);

            assertThat(result).isEmpty();
        }
    }
}