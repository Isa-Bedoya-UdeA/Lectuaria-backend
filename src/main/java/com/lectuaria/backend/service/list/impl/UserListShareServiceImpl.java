package com.lectuaria.backend.service.list.impl;

import com.lectuaria.backend.dto.book.BookSummaryDTO;
import com.lectuaria.backend.dto.book.GenreDTO;
import com.lectuaria.backend.dto.book.ShareResultDTO;
import com.lectuaria.backend.dto.list.UserListShareDTO;
import com.lectuaria.backend.dto.list.UserListShareLinkDTO;
import com.lectuaria.backend.exception.list.AlreadySharedException;
import com.lectuaria.backend.exception.list.PrivateListException;
import com.lectuaria.backend.model.auth.User;
import com.lectuaria.backend.model.book.Author;
import com.lectuaria.backend.model.book.Book;
import com.lectuaria.backend.model.list.ListVisibility;
import com.lectuaria.backend.model.list.UserList;
import com.lectuaria.backend.model.list.UserListShare;
import com.lectuaria.backend.model.list.UserListShareLink;
import com.lectuaria.backend.model.notification.NotificationType;
import com.lectuaria.backend.repository.auth.UserRepository;
import com.lectuaria.backend.repository.list.UserListBookRepository;
import com.lectuaria.backend.repository.list.UserListRepository;
import com.lectuaria.backend.repository.list.UserListShareLinkRepository;
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

@Service
public class UserListShareServiceImpl implements IUserListShareService {

    private final UserListShareRepository shareRepository;
    private final UserListShareLinkRepository linkRepository;
    private final UserListRepository listRepository;
    private final UserRepository userRepository;
    private final UserListBookRepository listBookRepository;
    private final NotificationRepository notificationRepository;
    private final INotificationService notificationService;

    public UserListShareServiceImpl(UserListShareRepository shareRepository, UserListShareLinkRepository linkRepository, UserListRepository listRepository, UserRepository userRepository, UserListBookRepository listBookRepository, NotificationRepository notificationRepository, INotificationService notificationService) {
        this.shareRepository = shareRepository;
        this.linkRepository = linkRepository;
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
            if (friendId.equals(ownerId)) {
                continue;
            }

            User receiver = userRepository.findById(friendId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (shareRepository.findByListIdAndReceiverId(listId, friendId).isPresent()) {
                throw new AlreadySharedException("Esta lista ya ha sido compartida con este usuario");
            }

            UserListShare share = new UserListShare(list, list.getUser(), receiver);
            // Generate unique token for LISTED lists
            if (list.getVisibility() == ListVisibility.LISTED) {
                String shareToken = generateUniqueToken();
                share.setShareToken(shareToken);
            }
            share = shareRepository.save(share);

            if (firstShare == null) {
                firstShare = mapToDTO(share, false);
            }
        }

        return firstShare;
    }

    private String generateUniqueToken() {
        return UUID.randomUUID().toString();
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
            if (friendId.equals(ownerId)) {
                continue;
            }

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
                    share.setShareToken(generateUniqueToken());
                }
                share = shareRepository.save(share);

                String notificationMessage = message != null && !message.isEmpty()
                        ? list.getUser().getFullName() + " te ha compartido esta lista: " + list.getName() + " - " + message
                        : list.getUser().getFullName() + " te ha compartido esta lista: " + list.getName();

                // Create notification with shareToken for LISTED lists (to redirect to /shared/token)
                // For PUBLIC lists, also set shareToken so frontend knows to redirect to shared page
                if (list.getVisibility() == ListVisibility.LISTED) {
                    notificationService.createNotificationWithShareToken(
                        friendId,
                        NotificationType.SHARED,
                        notificationMessage,
                        list.getId(),
                        share.getShareToken()
                    );
                } else {
                    // For PUBLIC visibility, create a public token and use it
                    String publicToken = UUID.randomUUID().toString();
                    notificationService.createNotificationWithShareToken(
                        friendId,
                        NotificationType.SHARED,
                        notificationMessage,
                        list.getId(),
                        publicToken
                    );

                    // Also save the public link if it doesn't exist
                    if (linkRepository.findByListId(listId).isEmpty()) {
                        UserListShareLink newLink = new UserListShareLink(list, publicToken);
                        linkRepository.save(newLink);
                    }
                }
                successfulShares++;
            } catch (Exception e) {
                failedShares++;
                otherErrors.add("Error al compartir con " + receiver.getFullName() + ": " + e.getMessage());
            }
        }

        // Construir mensajes de error más inteligentes
        if (!alreadySharedFriends.isEmpty()) {
            if (alreadySharedFriends.size() == 1) {
                errorMessages.add("La lista ya ha sido compartida con " + alreadySharedFriends.get(0));
            } else if (alreadySharedFriends.size() == 2) {
                errorMessages.add("La lista ya ha sido compartida con " + alreadySharedFriends.get(0) + " y " + alreadySharedFriends.get(1));
            } else {
                // 3 o más amigos
                String friendsList = String.join(", ", alreadySharedFriends.subList(0, alreadySharedFriends.size() - 1)) 
                    + " y " + alreadySharedFriends.get(alreadySharedFriends.size() - 1);
                errorMessages.add("La lista ya ha sido compartida con " + friendsList);
            }
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
    public UserListShareLinkDTO generatePublicLink(Long listId, Long ownerId) {
        UserList list = listRepository.findById(listId)
                .orElseThrow(() -> new RuntimeException("List not found"));

        if (!list.getUser().getId().equals(ownerId)) {
            throw new RuntimeException("You are not the owner of this list");
        }

        if (list.getVisibility() == ListVisibility.PRIVATE) {
            throw new PrivateListException("No se puede compartir una lista privada");
        }

        UserListShareLink existingLink = linkRepository.findByListId(listId).orElse(null);
        if (existingLink != null && existingLink.isActive()) {
            return mapToLinkDTO(existingLink);
        }

        String publicToken = UUID.randomUUID().toString();
        UserListShareLink link = new UserListShareLink(list, publicToken);
        link = linkRepository.save(link);

        return mapToLinkDTO(link);
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
    @Transactional
    public void revokePublicLink(Long linkId, Long ownerId) {
        UserListShareLink link = linkRepository.findById(linkId)
                .orElseThrow(() -> new RuntimeException("Link not found"));

        if (!link.getList().getUser().getId().equals(ownerId)) {
            throw new RuntimeException("You are not the owner of this list");
        }

        link.setActive(false);
        linkRepository.save(link);
    }

    @Override
    public List<UserListShareDTO> getSharedLists(Long userId) {
        List<UserListShare> shares = shareRepository.findByReceiverIdAndIsActiveTrue(userId);
        return shares.stream()
                .map(share -> mapToDTO(share, false))
                .collect(Collectors.toList());
    }

    @Override
    public UserListShareDTO getListByPublicToken(String token) {
        UserListShareLink link = linkRepository.findByPublicTokenAndIsActiveTrue(token).orElse(null);
        
        if (link != null) {
            UserListShare share = new UserListShare();
            share.setList(link.getList());
            share.setOwner(link.getList().getUser());
            share.setReceiver(null);
            share.setActive(true);
            return mapToDTO(share, true);
        }

        UserListShare share = shareRepository.findByShareTokenAndIsActiveTrue(token)
                .orElseThrow(() -> new RuntimeException("Invalid or expired link"));

        return mapToDTO(share, true);
    }

    @Override
    public List<UserListShareLinkDTO> getPublicLinks(Long listId, Long ownerId) {
        UserList list = listRepository.findById(listId)
                .orElseThrow(() -> new RuntimeException("List not found"));

        if (!list.getUser().getId().equals(ownerId)) {
            throw new RuntimeException("You are not the owner of this list");
        }

        if (list.getVisibility() == ListVisibility.PRIVATE) {
            throw new PrivateListException("No se puede compartir una lista privada");
        }

        List<UserListShareLink> links = linkRepository.findAllByListId(listId);
        return links.stream()
                .map(this::mapToLinkDTO)
                .collect(Collectors.toList());
    }

    private UserListShareDTO mapToDTO(UserListShare share, boolean includeBooks) {
        List<BookSummaryDTO> books = null;
        if (includeBooks && share.getList() != null) {
            books = listBookRepository.findByUserListIdOrderByAddedAtDesc(share.getList().getId())
                    .stream()
                    .map(ulb -> mapBookToSummaryDTO(ulb.getBook()))
                    .collect(Collectors.toList());
        }

        String publicToken = null;
        if (share.getList() != null) {
            if (share.getList().getVisibility() == ListVisibility.LISTED && share.getShareToken() != null) {
                publicToken = share.getShareToken();
            } else {
                UserListShareLink link = linkRepository.findByListId(share.getList().getId()).orElse(null);
                if (link != null && link.isActive()) {
                    publicToken = link.getPublicToken();
                }
            }
        }

        return new UserListShareDTO(
                share.getId(),
                share.getList().getId(),
                share.getList().getName(),
                share.getList().getDescription(),
                share.getOwner().getId(),
                share.getOwner().getFullName(),
                share.getReceiver() != null ? share.getReceiver().getId() : null,
                share.getReceiver() != null ? share.getReceiver().getFullName() : null,
                share.getSharedAt(),
                share.isActive(),
                books,
                publicToken
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
                book.getId(),
                book.getIsbn(),
                book.getTitle(),
                authors,
                genres,
                book.getAverageRating() != null ? book.getAverageRating() : BigDecimal.ZERO,
                book.getRatingsCount() != null ? book.getRatingsCount() : 0,
                book.getCoverUrl(),
                null, // libraryId
                null, // userAddedId
                book.getCreatedBy() != null ? book.getCreatedBy().getId() : null
        );
    }

    private UserListShareLinkDTO mapToLinkDTO(UserListShareLink link) {
        return new UserListShareLinkDTO(
                link.getId(),
                link.getList().getId(),
                link.getList().getName(),
                link.getPublicToken(),
                link.getCreatedAt(),
                link.isActive()
        );
    }
}
