package com.lectuaria.backend.service.friendship.impl;

import com.lectuaria.backend.dto.common.UserSearchResponseDTO;
import com.lectuaria.backend.model.auth.User;
import com.lectuaria.backend.model.auth.UserRole;
import com.lectuaria.backend.model.friendship.Friendship;
import com.lectuaria.backend.model.friendship.FriendshipRequest;
import com.lectuaria.backend.model.friendship.FriendshipRequestStatus;
import com.lectuaria.backend.model.notification.NotificationType;
import com.lectuaria.backend.repository.auth.UserRepository;
import com.lectuaria.backend.repository.friendship.FriendshipRepository;
import com.lectuaria.backend.repository.friendship.FriendshipRequestRepository;
import com.lectuaria.backend.service.friendship.IFriendshipService;
import com.lectuaria.backend.service.notification.INotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class FriendshipServiceImpl implements IFriendshipService {

    private final FriendshipRepository friendshipRepository;
    private final FriendshipRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final INotificationService notificationService;

    public FriendshipServiceImpl(FriendshipRepository friendshipRepository, FriendshipRequestRepository requestRepository,
            UserRepository userRepository, INotificationService notificationService) {
        this.friendshipRepository = friendshipRepository;
        this.requestRepository = requestRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<UserSearchResponseDTO> searchReaders(String query, User currentUser) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }

        List<User> users;
        if (currentUser != null) {
            // Authenticated: exclude self from results
            users = userRepository.searchReaders(query.trim(), currentUser.getId(), UserRole.NORMAL);
        } else {
            // Public: no userId exclusion
            users = userRepository.searchReadersPublic(query.trim(), UserRole.NORMAL);
        }

        return users.stream().map(u -> mapToDTO(u, currentUser)).collect(Collectors.toList());
    }

    @Transactional
    public void sendFriendshipRequest(Long receiverId, User sender) {
        if (sender.getId().equals(receiverId)) {
            throw new IllegalArgumentException("No puedes enviarte una solicitud a ti mismo");
        }

        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        if (friendshipRepository.existsByUsers(sender.getId(), receiverId)) {
            throw new IllegalArgumentException("Ya son amigos");
        }

        if (requestRepository.hasPendingRequestBetween(sender.getId(), receiverId)) {
            throw new IllegalArgumentException("Ya hay una solicitud de amistad pendiente");
        }

        FriendshipRequest request = new FriendshipRequest(sender, receiver);
        request = requestRepository.save(request);

        notificationService.createNotification(
                receiverId,
                NotificationType.FRIENDSHIP,
                "Tienes una solicitud de amistad de " + sender.getFullName(),
                request.getId()
        );
    }

    @Transactional
    public void acceptFriendshipRequest(Long requestId, User receiver) {
        FriendshipRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada"));

        if (!request.getReceiver().getId().equals(receiver.getId())) {
            throw new IllegalArgumentException("No puedes aceptar esta solicitud");
        }

        if (request.getStatus() != FriendshipRequestStatus.pending) {
            throw new IllegalArgumentException("La solicitud no está pendiente");
        }

        request.setStatus(FriendshipRequestStatus.accepted);
        requestRepository.save(request);

        Friendship friendship = new Friendship(request.getSender(), request.getReceiver());
        friendship = friendshipRepository.save(friendship);

        notificationService.createNotification(
                request.getSender().getId(),
                NotificationType.FRIENDSHIP,
                receiver.getFullName() + " ha aceptado tu solicitud de amistad",
                friendship.getId()
        );
    }

    @Transactional
    public void rejectFriendshipRequest(Long requestId, User receiver) {
        FriendshipRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada"));

        if (!request.getReceiver().getId().equals(receiver.getId())) {
            throw new IllegalArgumentException("No puedes rechazar esta solicitud");
        }

        if (request.getStatus() != FriendshipRequestStatus.pending) {
            throw new IllegalArgumentException("La solicitud no está pendiente");
        }

        request.setStatus(FriendshipRequestStatus.rejected);
        requestRepository.save(request); // Optionally just delete it according to DDL/Logic. We update to rejected or
                                         // delete.
        // Actually, "Al rechazar, la solicitud desaparece" implies we might delete it.
        // Let's delete it so they can resend later if needed.
        requestRepository.delete(request);
    }

    @Transactional
    public void cancelFriendshipRequest(Long requestId, User sender) {
        FriendshipRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada"));

        if (!request.getSender().getId().equals(sender.getId())) {
            throw new IllegalArgumentException("No puedes cancelar esta solicitud");
        }

        if (request.getStatus() != FriendshipRequestStatus.pending) {
            throw new IllegalArgumentException("La solicitud no está pendiente");
        }

        requestRepository.delete(request);
    }

    @Transactional
    public void removeFriendship(Long friendId, User currentUser) {
        Friendship friendship = friendshipRepository.findByUsers(currentUser.getId(), friendId)
                .orElseThrow(() -> new IllegalArgumentException("Amistad no encontrada"));

        friendshipRepository.delete(friendship);

        // Delete ALL requests between the two users (pending OR accepted) so they can re-send later
        requestRepository.deleteAllRequestsBetween(currentUser.getId(), friendId);
    }

    @Transactional(readOnly = true)
    public List<UserSearchResponseDTO> getFriends(User currentUser) {
        List<Friendship> friendships = friendshipRepository.findFriendsByUserId(currentUser.getId());
        return friendships.stream().map(f -> {
            User friend = f.getUser1().getId().equals(currentUser.getId()) ? f.getUser2() : f.getUser1();
            return mapToDTO(friend, currentUser);
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UserSearchResponseDTO> getPendingRequests(User currentUser) {
        List<FriendshipRequest> requests = requestRepository.findByReceiverIdAndStatus(currentUser.getId(),
                FriendshipRequestStatus.pending);
        return requests.stream().map(req -> {
            UserSearchResponseDTO dto = mapToDTO(req.getSender(), currentUser);
            dto.setFriendshipRequestId(req.getId());
            return dto;
        }).collect(Collectors.toList());
    }

    private UserSearchResponseDTO mapToDTO(User user, User currentUser) {
        String status = "none";
        Long reqId = null;

        if (currentUser != null && !user.getId().equals(currentUser.getId())) {
            if (friendshipRepository.existsByUsers(currentUser.getId(), user.getId())) {
                status = "friends";
            } else {
                Optional<FriendshipRequest> optReq = requestRepository.findPendingRequestBetween(currentUser.getId(),
                        user.getId());
                if (optReq.isPresent()) {
                    FriendshipRequest req = optReq.get();
                    if (req.getSender().getId().equals(currentUser.getId())) {
                        status = "pending_sent";
                    } else {
                        status = "pending_received";
                    }
                    reqId = req.getId();
                }
            }
        } else if (currentUser != null && user.getId().equals(currentUser.getId())) {
            status = "self";
        }

        // We can define city as "Desconocido" for now since living zone is not mapped
        // in User entity
        return new UserSearchResponseDTO(
                user.getId(),
                user.getFullName(),
                user.getUsername(),
                user.getPhotoUrl(),
                "Medellín", // Hardcoded or extracted if mapped
                status,
                reqId);
    }
}
