package com.lectuaria.backend.service.list;

import com.lectuaria.backend.dto.book.ShareResultDTO;
import com.lectuaria.backend.dto.list.UserListShareDTO;
import com.lectuaria.backend.dto.list.UserListShareLinkDTO;

import java.util.List;

public interface IUserListShareService {
    UserListShareDTO shareListWithFriends(Long listId, List<Long> friendIds, Long ownerId);
    ShareResultDTO shareListWithMultipleFriends(Long listId, List<Long> friendIds, String message, Long ownerId);
    UserListShareLinkDTO generatePublicLink(Long listId, Long ownerId);
    void revokeShare(Long shareId, Long ownerId);
    void revokePublicLink(Long linkId, Long ownerId);
    List<UserListShareDTO> getSharedLists(Long userId);
    UserListShareDTO getListByPublicToken(String token);
    List<UserListShareLinkDTO> getPublicLinks(Long listId, Long ownerId);
}
