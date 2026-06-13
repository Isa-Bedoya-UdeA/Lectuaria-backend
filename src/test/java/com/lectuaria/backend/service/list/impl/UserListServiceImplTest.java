package com.lectuaria.backend.service.list.impl;

import com.lectuaria.backend.dto.book.BookSummaryDTO;
import com.lectuaria.backend.dto.book.GenreDTO;
import com.lectuaria.backend.dto.list.CreateListRequestDTO;
import com.lectuaria.backend.dto.list.MoveBookResponseDTO;
import com.lectuaria.backend.dto.list.UpdateListRequestDTO;
import com.lectuaria.backend.dto.list.UserListDTO;
import com.lectuaria.backend.exception.ConflictException;
import com.lectuaria.backend.exception.UnauthorizedException;
import com.lectuaria.backend.model.auth.User;
import com.lectuaria.backend.model.auth.UserRole;
import com.lectuaria.backend.model.book.Author;
import com.lectuaria.backend.model.book.Book;
import com.lectuaria.backend.model.book.Genre;
import com.lectuaria.backend.model.list.ListType;
import com.lectuaria.backend.model.list.ListVisibility;
import com.lectuaria.backend.model.list.UserList;
import com.lectuaria.backend.model.list.UserListBook;
import com.lectuaria.backend.repository.book.BookRepository;
import com.lectuaria.backend.repository.list.UserListBookRepository;
import com.lectuaria.backend.repository.list.UserListRepository;
import com.lectuaria.backend.repository.list.UserListShareRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserListServiceImplTest {

    @Mock
    private UserListRepository listRepository;

    @Mock
    private UserListBookRepository listBookRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private UserListShareRepository listShareRepository;

    @InjectMocks
    private UserListServiceImpl userListService;

    private User readerUser;
    private User librarianUser;
    private UserList customList;
    private Book book;

    @BeforeEach
    void setUp() {
        readerUser = new User("Reader", "reader@test.com", "hash", UserRole.READER, "reader_test", null, null);
        setId(readerUser, 1L);

        librarianUser = new User("Librarian", "lib@test.com", "hash", UserRole.LIBRARIAN, "lib_test", null, null);
        setId(librarianUser, 2L);

        customList = new UserList(readerUser, "Mi Lista", "Una lista personalizada", ListType.CUSTOM, ListVisibility.LISTED);
        setId(customList, 10L);

        Author author = new Author();
        author.setName("Gabriel García Márquez");
        setId(author, 200L);

        Genre genre = new Genre();
        genre.setName("Ficción");
        setId(genre, 300L);

        book = new Book();
        book.setIsbn(9780307474278L);
        book.setTitle("Cien años de soledad");
        book.setAuthors(List.of(author));
        book.setGenres(List.of(genre));
        book.setAverageRating(new BigDecimal("4.5"));
        book.setRatingsCount(1200);
        setId(book, 50L);
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

    // --- createDefaultLists ---

    @Test
    void createDefaultLists_doesNothingForLibrarian() {
        userListService.createDefaultLists(librarianUser);
        verify(listRepository, never()).save(any());
    }

    @Test
    void createDefaultLists_createsFourSystemListsForReader() {
        when(listRepository.findByUserIdAndName(1L, "Por leer")).thenReturn(Optional.empty());
        when(listRepository.findByUserIdAndName(1L, "Leyendo")).thenReturn(Optional.empty());
        when(listRepository.findByUserIdAndName(1L, "Leídos")).thenReturn(Optional.empty());
        when(listRepository.findByUserIdAndName(1L, "Favoritos")).thenReturn(Optional.empty());

        userListService.createDefaultLists(readerUser);

        verify(listRepository, times(4)).save(any(UserList.class));
    }

    @Test
    void createDefaultLists_skipsExistingLists() {
        // "Por leer" exists, others don't
        when(listRepository.findByUserIdAndName(1L, "Por leer")).thenReturn(Optional.of(new UserList()));
        when(listRepository.findByUserIdAndName(1L, "Leyendo")).thenReturn(Optional.empty());
        when(listRepository.findByUserIdAndName(1L, "Leídos")).thenReturn(Optional.empty());
        when(listRepository.findByUserIdAndName(1L, "Favoritos")).thenReturn(Optional.empty());

        userListService.createDefaultLists(readerUser);

        // Only 3 saves (Leyendo, Leídos, Favoritos — Por leer already exists)
        verify(listRepository, times(3)).save(any(UserList.class));
    }

    // --- createCustomList ---

    @Test
    void createCustomList_throwsForLibrarian() {
        CreateListRequestDTO request = new CreateListRequestDTO();
        request.setName("Nueva Lista");
        request.setDescription("Descripción");
        request.setVisibility(ListVisibility.PRIVATE);

        assertThatThrownBy(() -> userListService.createCustomList(request, librarianUser))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Los bibliotecarios no pueden tener listas de lectura.");
    }

    @Test
    void createCustomList_throwsWhenListNameExists() {
        CreateListRequestDTO request = new CreateListRequestDTO();
        request.setName("Mi Lista");
        request.setDescription("Descripción");
        request.setVisibility(ListVisibility.PRIVATE);

        when(listRepository.findByUserIdAndName(1L, "Mi Lista")).thenReturn(Optional.of(customList));

        assertThatThrownBy(() -> userListService.createCustomList(request, readerUser))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Ya tienes una lista con este nombre.");
    }

    @Test
    void createCustomList_createsAndReturnsDTO() {
        CreateListRequestDTO request = new CreateListRequestDTO();
        request.setName("Mi Lista");
        request.setDescription("Descripción");
        request.setVisibility(ListVisibility.LISTED);

        when(listRepository.findByUserIdAndName(1L, "Mi Lista")).thenReturn(Optional.empty());
        when(listRepository.save(any(UserList.class))).thenAnswer(inv -> {
            UserList saved = inv.getArgument(0);
            setId(saved, 10L);
            return saved;
        });
        when(listBookRepository.countByListId(10L)).thenReturn(0L);

        UserListDTO result = userListService.createCustomList(request, readerUser);

        assertThat(result.getName()).isEqualTo("Mi Lista");
        assertThat(result.getListType()).isEqualTo(ListType.CUSTOM);
        assertThat(result.getVisibility()).isEqualTo(ListVisibility.LISTED);
        assertThat(result.getBookCount()).isEqualTo(0L);
    }

    // --- getUserLists ---

    @Test
    void getUserLists_returnsAllUserLists() {
        UserList systemList = new UserList(readerUser, "Por leer", "Por leer desc", ListType.SYSTEM, ListVisibility.LISTED);
        setId(systemList, 5L);
        when(listRepository.findByUserIdOrderByCreatedAtAsc(1L)).thenReturn(List.of(systemList, customList));
        when(listBookRepository.countByListId(5L)).thenReturn(3L);
        when(listBookRepository.countByListId(10L)).thenReturn(2L);

        List<UserListDTO> result = userListService.getUserLists(1L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Por leer");
        assertThat(result.get(0).getBookCount()).isEqualTo(3L);
        assertThat(result.get(1).getName()).isEqualTo("Mi Lista");
        assertThat(result.get(1).getBookCount()).isEqualTo(2L);
    }

    @Test
    void getUserLists_returnsEmptyForUserWithNoLists() {
        when(listRepository.findByUserIdOrderByCreatedAtAsc(1L)).thenReturn(Collections.emptyList());

        List<UserListDTO> result = userListService.getUserLists(1L);

        assertThat(result).isEmpty();
    }

    // --- getListDetails ---

    @Test
    void getListDetails_throwsForPrivateListWhenNotOwner() {
        User otherUser = new User("Other", "other@test.com", "hash", UserRole.READER, "other_test", null, null);
        setId(otherUser, 99L);

        UserList privateList = new UserList(readerUser, "Private List", "desc", ListType.CUSTOM, ListVisibility.PRIVATE);
        setId(privateList, 10L);
        when(listRepository.findById(10L)).thenReturn(Optional.of(privateList));

        assertThatThrownBy(() -> userListService.getListDetails(10L, 99L))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Esta lista es privada");
    }

    @Test
    void getListDetails_returnsListWithBooks() {
        UserListBook ulb = new UserListBook(customList, book);
        setId(ulb, 1L);
        when(listRepository.findById(10L)).thenReturn(Optional.of(customList));
        when(listBookRepository.findByUserListIdOrderByAddedAtDesc(10L)).thenReturn(List.of(ulb));
        when(listBookRepository.countByListId(10L)).thenReturn(1L);

        UserListDTO result = userListService.getListDetails(10L, 1L);

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getBooks()).hasSize(1);
        assertThat(result.getBooks().get(0).getTitle()).isEqualTo("Cien años de soledad");
    }

    // --- addBookToList ---

    @Test
    void addBookToList_throwsWhenBookAlreadyInList() {
        UserListBook existing = new UserListBook(customList, book);
        when(listRepository.findById(10L)).thenReturn(Optional.of(customList));
        when(bookRepository.findById(50L)).thenReturn(Optional.of(book));
        when(listBookRepository.findByUserListIdAndBookId(10L, 50L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> userListService.addBookToList(10L, 50L, readerUser, false))
                .isInstanceOf(ConflictException.class)
                .hasMessage("El libro ya se encuentra en esta lista");
    }

    @Test
    void addBookToList_addsNewBookToList() {
        when(listRepository.findById(10L)).thenReturn(Optional.of(customList));
        when(listBookRepository.findByUserListIdAndBookId(10L, 50L)).thenReturn(Optional.empty());
        when(bookRepository.findById(50L)).thenReturn(Optional.of(book));

        userListService.addBookToList(10L, 50L, readerUser, false);

        verify(listBookRepository).save(any(UserListBook.class));
    }

    @Test
    void addBookToList_throwsForUnauthorizedUser() {
        User otherUser = new User("Other", "other@test.com", "hash", UserRole.READER, "other_test", null, null);
        setId(otherUser, 99L);

        when(listRepository.findById(10L)).thenReturn(Optional.of(customList));

        assertThatThrownBy(() -> userListService.addBookToList(10L, 50L, otherUser, false))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("No tienes permiso para modificar esta lista");
    }

    // --- moveBookBetweenLists ---

    @Test
    void moveBookBetweenLists_throwsWhenSourceAndTargetAreSame() {
        assertThatThrownBy(() -> userListService.moveBookBetweenLists(10L, 10L, 50L, readerUser))
                .isInstanceOf(ConflictException.class)
                .hasMessage("La lista origen y destino deben ser diferentes.");
    }

    @Test
    void moveBookBetweenLists_movesBookSuccessfully() {
        UserList targetList = new UserList(readerUser, "Leídos", "Leídos desc", ListType.SYSTEM, ListVisibility.LISTED);
        setId(targetList, 20L);

        UserListBook sourceMembership = new UserListBook(customList, book);
        setId(sourceMembership, 1L);

        when(listRepository.findById(10L)).thenReturn(Optional.of(customList));
        when(listRepository.findById(20L)).thenReturn(Optional.of(targetList));
        when(listBookRepository.findByUserListIdAndBookId(10L, 50L)).thenReturn(Optional.of(sourceMembership));
        when(listBookRepository.findByUserListIdAndBookId(20L, 50L)).thenReturn(Optional.empty());
        when(listBookRepository.countByListId(10L)).thenReturn(4L);
        when(listBookRepository.countByListId(20L)).thenReturn(5L);

        MoveBookResponseDTO result = userListService.moveBookBetweenLists(10L, 20L, 50L, readerUser);

        assertThat(result.getSourceListId()).isEqualTo(10L);
        assertThat(result.getTargetListId()).isEqualTo(20L);
        assertThat(result.getSourceListBookCount()).isEqualTo(4L);
        assertThat(result.getTargetListBookCount()).isEqualTo(5L);
        verify(listBookRepository).delete(sourceMembership);
        verify(listBookRepository).save(any(UserListBook.class));
    }

    // --- removeBookFromList ---

    @Test
    void removeBookFromList_removesBookWhenFound() {
        UserListBook existing = new UserListBook(customList, book);
        when(listRepository.findById(10L)).thenReturn(Optional.of(customList));
        when(listBookRepository.findByUserListIdAndBookId(10L, 50L)).thenReturn(Optional.of(existing));

        userListService.removeBookFromList(10L, 50L, readerUser);

        verify(listBookRepository).delete(existing);
    }

    @Test
    void removeBookFromList_doesNothingWhenBookNotFound() {
        when(listRepository.findById(10L)).thenReturn(Optional.of(customList));
        when(listBookRepository.findByUserListIdAndBookId(10L, 50L)).thenReturn(Optional.empty());

        userListService.removeBookFromList(10L, 50L, readerUser);

        verify(listBookRepository, never()).delete(any());
    }

    // --- deleteList ---

    @Test
    void deleteList_throwsForSystemList() {
        UserList systemList = new UserList(readerUser, "Por leer", "desc", ListType.SYSTEM, ListVisibility.LISTED);
        setId(systemList, 5L);
        when(listRepository.findById(5L)).thenReturn(Optional.of(systemList));

        assertThatThrownBy(() -> userListService.deleteList(5L, readerUser, true, false))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Solo se pueden eliminar listas personalizadas.");
    }

    @Test
    void deleteList_throwsWithoutConfirmation() {
        when(listRepository.findById(10L)).thenReturn(Optional.of(customList));

        assertThatThrownBy(() -> userListService.deleteList(10L, readerUser, false, false))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Confirma la eliminación para continuar.");
    }

    @Test
    void deleteList_throwsWhenListHasBooksAndNotForced() {
        when(listRepository.findById(10L)).thenReturn(Optional.of(customList));
        when(listBookRepository.countByUserListId(10L)).thenReturn(5L);

        assertThatThrownBy(() -> userListService.deleteList(10L, readerUser, true, false))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("contiene libros");
    }

    @Test
    void deleteList_deletesListWithBooksWhenForced() {
        when(listRepository.findById(10L)).thenReturn(Optional.of(customList));
        when(listBookRepository.countByUserListId(10L)).thenReturn(5L);

        userListService.deleteList(10L, readerUser, true, true);

        verify(listBookRepository).deleteByUserListId(10L);
        verify(listShareRepository).deleteByListId(10L);
        verify(listRepository).delete(customList);
    }

    @Test
    void deleteList_deletesEmptyListWithoutForce() {
        when(listRepository.findById(10L)).thenReturn(Optional.of(customList));
        when(listBookRepository.countByUserListId(10L)).thenReturn(0L);

        userListService.deleteList(10L, readerUser, true, false);

        verify(listRepository).delete(customList);
    }

    // --- toggleFavorite ---

    @Test
    void toggleFavorite_addsBookWhenNotFavorited() {
        UserList favList = new UserList(readerUser, "Favoritos", "desc", ListType.SYSTEM, ListVisibility.LISTED);
        setId(favList, 30L);
        when(listRepository.findByUserIdAndNameAndListType(1L, "Favoritos", ListType.SYSTEM)).thenReturn(Optional.of(favList));
        when(listBookRepository.findByUserListIdAndBookId(30L, 50L)).thenReturn(Optional.empty());
        when(bookRepository.findById(50L)).thenReturn(Optional.of(book));

        boolean result = userListService.toggleFavorite(50L, readerUser);

        assertThat(result).isTrue();
        verify(listBookRepository).save(any(UserListBook.class));
    }

    @Test
    void toggleFavorite_removesBookWhenAlreadyFavorited() {
        UserList favList = new UserList(readerUser, "Favoritos", "desc", ListType.SYSTEM, ListVisibility.LISTED);
        setId(favList, 30L);
        UserListBook existing = new UserListBook(favList, book);
        setId(existing, 1L);

        when(listRepository.findByUserIdAndNameAndListType(1L, "Favoritos", ListType.SYSTEM)).thenReturn(Optional.of(favList));
        when(listBookRepository.findByUserListIdAndBookId(30L, 50L)).thenReturn(Optional.of(existing));
        when(bookRepository.findById(50L)).thenReturn(Optional.of(book));

        boolean result = userListService.toggleFavorite(50L, readerUser);

        assertThat(result).isFalse();
        verify(listBookRepository).delete(existing);
    }

    @Test
    void toggleFavorite_createsFavoriteListIfNotExists() {
        UserList favList = new UserList(readerUser, "Favoritos", "desc", ListType.SYSTEM, ListVisibility.LISTED);
        setId(favList, 30L);
        when(listRepository.findByUserIdAndNameAndListType(1L, "Favoritos", ListType.SYSTEM)).thenReturn(Optional.empty());
        when(listRepository.save(any())).thenAnswer(inv -> {
            UserList saved = inv.getArgument(0);
            setId(saved, 30L);
            return saved;
        });
        when(listBookRepository.findByUserListIdAndBookId(30L, 50L)).thenReturn(Optional.empty());
        when(bookRepository.findById(50L)).thenReturn(Optional.of(book));

        boolean result = userListService.toggleFavorite(50L, readerUser);

        assertThat(result).isTrue();
        verify(listRepository).save(any());
    }

    // --- updateCustomList ---

    @Nested
    class UpdateCustomListTests {

        @Test
        void updateCustomList_renamesAndChangesVisibility_returnsUpdatedDTO() {
            customList.setName("Old name");
            when(listRepository.findById(10L)).thenReturn(Optional.of(customList));
            when(listRepository.findByUserIdAndName(1L, "New name")).thenReturn(Optional.empty());
            when(listRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(listBookRepository.countByListId(10L)).thenReturn(2L);

            UpdateListRequestDTO req = new UpdateListRequestDTO();
            req.setName("New name");
            req.setVisibility(ListVisibility.PUBLIC);

            UserListDTO result = userListService.updateCustomList(10L, req, readerUser);

            assertThat(result.getName()).isEqualTo("New name");
            assertThat(result.getVisibility()).isEqualTo(ListVisibility.PUBLIC);
            // Como paso a PUBLIC sin token previo, debe generarse uno
            assertThat(customList.getPublicToken()).isNotNull().isNotBlank();
        }

        @Test
        void updateCustomList_visibilityToPrivate_invalidatesSharesAndClearsToken() {
            customList.setVisibility(ListVisibility.LISTED);
            customList.setPublicToken("old-token");
            when(listRepository.findById(10L)).thenReturn(Optional.of(customList));
            when(listRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(listBookRepository.countByListId(10L)).thenReturn(0L);

            UpdateListRequestDTO req = new UpdateListRequestDTO();
            req.setVisibility(ListVisibility.PRIVATE);

            UserListDTO result = userListService.updateCustomList(10L, req, readerUser);

            assertThat(result.getVisibility()).isEqualTo(ListVisibility.PRIVATE);
            verify(listShareRepository).deactivateAllByListId(10L);
            assertThat(customList.getPublicToken()).isNull();
        }

        @Test
        void updateCustomList_duplicateName_throws() {
            when(listRepository.findById(10L)).thenReturn(Optional.of(customList));
            UserList otra = new UserList(readerUser, "Otra", "d", ListType.CUSTOM, ListVisibility.PRIVATE);
            setId(otra, 99L);
            when(listRepository.findByUserIdAndName(1L, "Duplicada")).thenReturn(Optional.of(otra));

            UpdateListRequestDTO req = new UpdateListRequestDTO();
            req.setName("Duplicada");

            assertThatThrownBy(() -> userListService.updateCustomList(10L, req, readerUser))
                    .isInstanceOf(ConflictException.class)
                    .hasMessage("Ya tienes una lista con este nombre.");
        }

        @Test
        void updateCustomList_notOwner_throws() {
            User other = new User("Otro", "otro@test.com", "h", UserRole.READER, "otro_test", null, null);
            setId(other, 99L);
            when(listRepository.findById(10L)).thenReturn(Optional.of(customList));

            UpdateListRequestDTO req = new UpdateListRequestDTO();
            req.setName("Hack");

            assertThatThrownBy(() -> userListService.updateCustomList(10L, req, other))
                    .isInstanceOf(UnauthorizedException.class);
        }

        @Test
        void updateCustomList_systemList_throws() {
            UserList systemList = new UserList(readerUser, "Por leer", "d", ListType.SYSTEM, ListVisibility.LISTED);
            setId(systemList, 11L);
            when(listRepository.findById(11L)).thenReturn(Optional.of(systemList));

            UpdateListRequestDTO req = new UpdateListRequestDTO();
            req.setName("Hack");

            assertThatThrownBy(() -> userListService.updateCustomList(11L, req, readerUser))
                    .isInstanceOf(ConflictException.class)
                    .hasMessage("Solo se pueden editar listas personalizadas.");
        }

        @Test
        void updateCustomList_librarian_throws() {
            UpdateListRequestDTO req = new UpdateListRequestDTO();
            req.setName("X");

            assertThatThrownBy(() -> userListService.updateCustomList(10L, req, librarianUser))
                    .isInstanceOf(ConflictException.class)
                    .hasMessage("Los bibliotecarios no pueden editar listas de lectura.");
        }
    }
}