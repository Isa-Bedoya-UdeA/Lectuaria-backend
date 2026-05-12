package com.lectuaria.backend.service.list;

import com.lectuaria.backend.dto.book.BookSummaryDTO;
import com.lectuaria.backend.dto.list.CreateListRequestDTO;
import com.lectuaria.backend.dto.list.UserListDTO;
import com.lectuaria.backend.dto.list.MoveBookResponseDTO;
import com.lectuaria.backend.model.auth.User;

import java.util.List;

public interface IUserListService {
    void createDefaultLists(User user);
    UserListDTO createCustomList(CreateListRequestDTO request, User user);
    List<UserListDTO> getUserLists(Long userId);
    UserListDTO getListDetails(Long listId, Long userId);
    void addBookToList(Long listId, Long bookId, User user, boolean forceMove);
    MoveBookResponseDTO moveBookBetweenLists(Long sourceListId, Long targetListId, Long bookId, User user);
    void removeBookFromList(Long listId, Long bookId, User user);
    void deleteList(Long listId, User user, boolean confirmDelete, boolean forceDeleteWithBooks);
    boolean toggleFavorite(Long bookId, User user);
    UserListDTO getMyFavorites(User user);
}
