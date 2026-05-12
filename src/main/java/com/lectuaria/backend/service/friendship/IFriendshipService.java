package com.lectuaria.backend.service.friendship;

import com.lectuaria.backend.dto.common.UserSearchResponseDTO;
import com.lectuaria.backend.model.auth.User;

import java.util.List;

public interface IFriendshipService {
    List<UserSearchResponseDTO> searchReaders(String query, User currentUser);
    void sendFriendshipRequest(Long receiverId, User sender);
    void acceptFriendshipRequest(Long requestId, User receiver);
    void rejectFriendshipRequest(Long requestId, User receiver);
    void cancelFriendshipRequest(Long requestId, User sender);
    void removeFriendship(Long friendId, User currentUser);
    List<UserSearchResponseDTO> getFriends(User currentUser);
    List<UserSearchResponseDTO> getPendingRequests(User currentUser);
}
