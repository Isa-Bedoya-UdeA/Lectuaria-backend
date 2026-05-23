package com.lectuaria.backend.service.list;

import com.lectuaria.backend.dto.book.ShareResultDTO;
import com.lectuaria.backend.dto.list.UserListShareDTO;

import java.util.List;

// DESACTIVADO: user_list_share_link fue eliminado de la BD
// Los métodos relacionados con links públicos fueron removidos
public interface IUserListShareService {
    UserListShareDTO shareListWithFriends(Long listId, List<Long> friendIds, Long ownerId);
    ShareResultDTO shareListWithMultipleFriends(Long listId, List<Long> friendIds, String message, Long ownerId);
    void revokeShare(Long shareId, Long ownerId);
    List<UserListShareDTO> getSharedLists(Long userId);
    UserListShareDTO getListByPublicToken(String token);
}