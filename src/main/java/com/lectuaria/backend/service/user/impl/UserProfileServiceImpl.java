package com.lectuaria.backend.service.user.impl;

import com.lectuaria.backend.dto.user.FriendshipStatus;
import com.lectuaria.backend.dto.user.UserProfileDTO;
import com.lectuaria.backend.dto.user.UserStatsDTO;
import com.lectuaria.backend.exception.ResourceNotFoundException;
import com.lectuaria.backend.model.auth.User;
import com.lectuaria.backend.model.friendship.Friendship;
import com.lectuaria.backend.model.friendship.FriendshipRequest;
import com.lectuaria.backend.repository.auth.UserRepository;
import com.lectuaria.backend.repository.friendship.FriendshipRepository;
import com.lectuaria.backend.repository.friendship.FriendshipRequestRepository;
import com.lectuaria.backend.service.user.IUserProfileService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserProfileServiceImpl implements IUserProfileService {

    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;
    private final FriendshipRequestRepository friendshipRequestRepository;

    public UserProfileServiceImpl(UserRepository userRepository,
                                   FriendshipRepository friendshipRepository,
                                   FriendshipRequestRepository friendshipRequestRepository) {
        this.userRepository = userRepository;
        this.friendshipRepository = friendshipRepository;
        this.friendshipRequestRepository = friendshipRequestRepository;
    }

    @Transactional(readOnly = true)
    public UserProfileDTO getUserProfileByUsername(String usernameSlug, User currentUser) {
        User profileUser = userRepository.findByUsernameIgnoreCase(usernameSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        UserProfileDTO dto = new UserProfileDTO(
                profileUser.getId(),
                profileUser.getUsername(),
                profileUser.getFullName(),
                profileUser.getPhotoUrl(),
                profileUser.getBiography(),
                profileUser.getCreatedAt()
        );

        dto.setStats(getUserStats(usernameSlug));
        dto.setFriendshipStatus(determineFriendshipStatus(profileUser, currentUser));

        return dto;
    }

    @Transactional(readOnly = true)
    public UserStatsDTO getUserStats(String usernameSlug) {
        User user = userRepository.findByUsernameIgnoreCase(usernameSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        Integer friendsCount = friendshipRepository.findFriendsByUserId(user.getId()).size();
        Integer reviewsCount = 0;
        Integer favoritesCount = 0;
        Integer booksRead = 0;

        return new UserStatsDTO(booksRead, reviewsCount, friendsCount, favoritesCount);
    }

    private FriendshipStatus determineFriendshipStatus(User profileUser, User currentUser) {
        if (currentUser == null) {
            return FriendshipStatus.NONE;
        }

        if (profileUser.getId().equals(currentUser.getId())) {
            return FriendshipStatus.SELF;
        }

        Optional<Friendship> friendship = friendshipRepository.findByUsers(
                profileUser.getId(),
                currentUser.getId()
        );

        if (friendship.isPresent()) {
            return FriendshipStatus.ACCEPTED;
        }

        Optional<FriendshipRequest> request = friendshipRequestRepository
                .findPendingRequestBetween(
                        currentUser.getId(),
                        profileUser.getId()
                );

        if (request.isPresent()) {
            return FriendshipStatus.PENDING;
        }

        return FriendshipStatus.NONE;
    }
}
