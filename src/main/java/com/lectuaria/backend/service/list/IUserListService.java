package com.lectuaria.backend.service.list;

import com.lectuaria.backend.dto.list.CreateListRequestDTO;
import com.lectuaria.backend.dto.list.MoveBookResponseDTO;
import com.lectuaria.backend.dto.list.UpdateListRequestDTO;
import com.lectuaria.backend.dto.list.UserListDTO;
import com.lectuaria.backend.model.auth.User;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface IUserListService {
    @Transactional
    void createDefaultLists(User user);

    @Transactional
    UserListDTO createCustomList(CreateListRequestDTO request, User user);

    List<UserListDTO> getUserLists(Long userId);

    UserListDTO getListDetails(Long listId, Long userId);

    @Transactional
    void addBookToList(Long listId, Long bookId, User user, boolean forceMove);

    @Transactional
    MoveBookResponseDTO moveBookBetweenLists(Long sourceListId, Long targetListId, Long bookId, User user);

    @Transactional
    void removeBookFromList(Long listId, Long bookId, User user);

    @Transactional
    void deleteList(Long listId, User user, boolean confirmDelete, boolean forceDeleteWithBooks);

    boolean toggleFavorite(Long bookId, User user);

    UserListDTO getMyFavorites(User user);

    /**
     * Edita nombre, descripcion y/o visibilidad de una lista personalizada.
     * Si la lista pasa a PRIVATE, invalida todos los shares existentes
     * (no se puede compartir algo privado). Si pasa a LISTED/PUBLIC sin
     * token, genera uno.
     */
    @Transactional
    UserListDTO updateCustomList(Long listId, UpdateListRequestDTO request, User user);
}
