package com.lectuaria.backend.service.list.impl;

import com.lectuaria.backend.dto.book.BookSummaryDTO;
import com.lectuaria.backend.dto.book.GenreDTO;
import com.lectuaria.backend.dto.book.ShareResultDTO;
import com.lectuaria.backend.dto.list.UserListShareDTO;
import com.lectuaria.backend.exception.list.AlreadySharedException;
import com.lectuaria.backend.exception.list.PrivateListException;
import com.lectuaria.backend.model.auth.User;
import com.lectuaria.backend.model.book.Author;
import com.lectuaria.backend.model.book.Book;
import com.lectuaria.backend.model.list.ListVisibility;
import com.lectuaria.backend.model.list.UserList;
import com.lectuaria.backend.model.list.UserListShare;
import com.lectuaria.backend.model.notification.NotificationType;
import com.lectuaria.backend.repository.auth.UserRepository;
import com.lectuaria.backend.repository.list.UserListBookRepository;
import com.lectuaria.backend.repository.list.UserListRepository;
import com.lectuaria.backend.repository.list.UserListShareRepository;
import com.lectuaria.backend.repository.notification.NotificationRepository;
import com.lectuaria.backend.service.list.IUserListShareService;
import com.lectuaria.backend.service.notification.INotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

// DESACTIVADO: user_list_share_link fue eliminado de la BD
@Service
public class UserListShareServiceImpl implements IUserListShareService {

    private final UserListShareRepository shareRepository;
    private final UserListRepository listRepository;
    private final UserRepository userRepository;
    private final UserListBookRepository listBookRepository;
    private final NotificationRepository notificationRepository;
    private final INotificationService notificationService;

    public UserListShareServiceImpl(UserListShareRepository shareRepository,
            UserListRepository listRepository, UserRepository userRepository,
            UserListBookRepository listBookRepository, NotificationRepository notificationRepository,
            INotificationService notificationService) {
        this.shareRepository = shareRepository;
        this.listRepository = listRepository;
        this.userRepository = userRepository;
        this.listBookRepository = listBookRepository;
        this.notificationRepository = notificationRepository;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public UserListShareDTO shareListWithFriends(Long listId, List<Long> friendIds, Long ownerId) {
        UserList list = listRepository.findById(listId)
                .orElseThrow(() -> new RuntimeException("List not found"));

        if (!list.getUser().getId().equals(ownerId)) {
            throw new RuntimeException("You are not the owner of this list");
        }

        if (list.getVisibility() == ListVisibility.PRIVATE) {
            throw new PrivateListException("No se puede compartir una lista privada");
        }

        UserListShareDTO firstShare = null;
        for (Long friendId : friendIds) {
            if (friendId.equals(ownerId)) continue;

            User receiver = userRepository.findById(friendId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (shareRepository.findByListIdAndReceiverId(listId, friendId).isPresent()) {
                throw new AlreadySharedException("Esta lista ya ha sido compartida con este usuario");
            }

            UserListShare share = new UserListShare(list, list.getUser(), receiver);
            if (list.getVisibility() == ListVisibility.LISTED) {
                share.setShareToken(UUID.randomUUID().toString());
            }
            share = shareRepository.save(share);

            if (firstShare == null) firstShare = mapToDTO(share, false);
        }

        return firstShare;
    }

    @Override
    @Transactional
    public ShareResultDTO shareListWithMultipleFriends(Long listId, List<Long> friendIds, String message, Long ownerId) {
        UserList list = listRepository.findById(listId)
                .orElseThrow(() -> new RuntimeException("List not found"));

        if (!list.getUser().getId().equals(ownerId)) {
            throw new RuntimeException("You are not the owner of this list");
        }

        if (list.getVisibility() == ListVisibility.PRIVATE) {
            throw new PrivateListException("No se puede compartir una lista privada");
        }

        int successfulShares = 0;
        int failedShares = 0;
        List<String> errorMessages = new ArrayList<>();
        List<String> alreadySharedFriends = new ArrayList<>();
        List<String> otherErrors = new ArrayList<>();

        for (Long friendId : friendIds) {
            if (friendId.equals(ownerId)) continue;

            User receiver = userRepository.findById(friendId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (shareRepository.findByListIdAndReceiverId(listId, friendId).isPresent()) {
                failedShares++;
                alreadySharedFriends.add(receiver.getFullName());
                continue;
            }

            try {
                UserListShare share = new UserListShare(list, list.getUser(), receiver);
                if (list.getVisibility() == ListVisibility.LISTED) {
                    share.setShareToken(UUID.randomUUID().toString());
                }
                shareRepository.save(share);

                String notificationMessage = message != null && !message.isEmpty()
                        ? list.getUser().getFullName() + " te ha compartido esta lista: " + list.getName() + " - " + message
                        : list.getUser().getFullName() + " te ha compartido esta lista: " + list.getName();

                notificationService.createNotificationWithShareToken(
                        friendId, NotificationType.SHARED, notificationMessage, list.getId(),
                        list.getVisibility() == ListVisibility.LISTED ? share.getShareToken() : null);

                successfulShares++;
            } catch (Exception e) {
                failedShares++;
                otherErrors.add("Error al compartir con " + receiver.getFullName() + ": " + e.getMessage());
            }
        }

        if (!alreadySharedFriends.isEmpty()) {
            String friendsList = alreadySharedFriends.size() == 1
                    ? alreadySharedFriends.get(0)
                    : String.join(", ", alreadySharedFriends.subList(0, alreadySharedFriends.size() - 1))
                            + " y " + alreadySharedFriends.get(alreadySharedFriends.size() - 1);
            errorMessages.add("La lista ya ha sido compartida con " + friendsList);
        }
        errorMessages.addAll(otherErrors);

        String resultMessage;
        if (failedShares == 0) {
            resultMessage = "Lista compartida exitosamente con " + successfulShares + " amigo(s)";
        } else if (successfulShares == 0) {
            resultMessage = "No se pudo compartir la lista con ningún amigo";
        } else {
            resultMessage = "Lista compartida con " + successfulShares + " amigo(s), pero falló con " + failedShares + " amigo(s)";
        }

        return new ShareResultDTO(successfulShares, failedShares, errorMessages, resultMessage);
    }

    @Override
    @Transactional
    public void revokeShare(Long shareId, Long ownerId) {
        UserListShare share = shareRepository.findById(shareId)
                .orElseThrow(() -> new RuntimeException("Share not found"));

        if (!share.getOwner().getId().equals(ownerId)) {
            throw new RuntimeException("You are not the owner of this share");
        }

        share.setActive(false);
        shareRepository.save(share);
    }

    @Override
    public List<UserListShareDTO> getSharedLists(Long userId) {
        return shareRepository.findByReceiverIdAndIsActiveTrue(userId).stream()
                .map(share -> mapToDTO(share, false))
                .collect(Collectors.toList());
    }

    @Override
    public UserListShareDTO getListByPublicToken(String token) {
        UserListShare share = shareRepository.findByShareTokenAndIsActiveTrue(token)
                .orElseThrow(() -> new RuntimeException("Invalid or expired link"));
        return mapToDTO(share, true);
    }

    private UserListShareDTO mapToDTO(UserListShare share, boolean includeBooks) {
        List<BookSummaryDTO> books = null;
        if (includeBooks && share.getList() != null) {
            books = listBookRepository.findByUserListIdOrderByAddedAtDesc(share.getList().getId())
                    .stream()
                    .map(ulb -> mapBookToSummaryDTO(ulb.getBook()))
                    .collect(Collectors.toList());
        }

        return new UserListShareDTO(
                share.getId(), share.getList().getId(), share.getList().getName(),
                share.getList().getDescription(),
                share.getOwner().getId(), share.getOwner().getFullName(),
                share.getReceiver() != null ? share.getReceiver().getId() : null,
                share.getReceiver() != null ? share.getReceiver().getFullName() : null,
                share.getSharedAt(), share.isActive(),
                books,
                share.getList().getVisibility() == ListVisibility.LISTED ? share.getShareToken() : null
        );
    }

    private BookSummaryDTO mapBookToSummaryDTO(Book book) {
        List<String> authors = book.getAuthors() != null
                ? book.getAuthors().stream().map(Author::getName).collect(Collectors.toList())
                : List.of();
        List<GenreDTO> genres = book.getGenres() != null
                ? book.getGenres().stream()
                        .map(g -> new GenreDTO(g.getId(), g.getName(), g.getDescription()))
                        .collect(Collectors.toList())
                : List.of();

        return new BookSummaryDTO(
                book.getId(), book.getIsbn(), book.getTitle(), authors, genres,
                book.getAverageRating() != null ? book.getAverageRating() : BigDecimal.ZERO,
                book.getRatingsCount() != null ? book.getRatingsCount() : 0,
                book.getCoverUrl(), null, null,
                book.getCreatedBy() != null ? book.getCreatedBy().getId() : null,
                book.getCreatedAt()
        );
    }
}