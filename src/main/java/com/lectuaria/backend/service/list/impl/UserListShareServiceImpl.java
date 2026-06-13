package com.lectuaria.backend.service.list.impl;

import com.lectuaria.backend.dto.book.BookSummaryDTO;
import com.lectuaria.backend.dto.book.GenreDTO;
import com.lectuaria.backend.dto.book.ShareResultDTO;
import com.lectuaria.backend.dto.list.UserListShareDTO;
import com.lectuaria.backend.exception.ForbiddenException;
import com.lectuaria.backend.exception.ResourceNotFoundException;
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
                .orElseThrow(() -> new ResourceNotFoundException("Lista no encontrada con id: " + listId));

        if (!list.getUser().getId().equals(ownerId)) {
            throw new ForbiddenException("No eres el dueño de esta lista");
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
                .orElseThrow(() -> new ResourceNotFoundException("Lista no encontrada con id: " + listId));

        if (!list.getUser().getId().equals(ownerId)) {
            throw new ForbiddenException("No eres el dueño de esta lista");
        }

        if (list.getVisibility() == ListVisibility.PRIVATE) {
            throw new PrivateListException("No se puede compartir una lista privada");
        }

        // El token de enlace publico es propio de la lista, no del share individual.
        // Se asegura de que exista (y se persiste) para visibilidades LISTED y PUBLIC,
        // independientemente de cuantos amigos se seleccionen. Asi el enlace publico
        // sigue funcionando aunque el dueno nunca comparta con un amigo especifico.
        String listPublicToken = ensurePublicToken(list);

        int successfulShares = 0;
        int failedShares = 0;
        List<String> errorMessages = new ArrayList<>();
        List<String> alreadySharedFriends = new ArrayList<>();
        List<String> otherErrors = new ArrayList<>();

        for (Long friendId : friendIds) {
            if (friendId.equals(ownerId)) continue;

            User receiver = userRepository.findById(friendId)
                    .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + friendId));

            if (shareRepository.findByListIdAndReceiverId(listId, friendId).isPresent()) {
                failedShares++;
                alreadySharedFriends.add(receiver.getFullName());
                continue;
            }

            try {
                UserListShare share = new UserListShare(list, list.getUser(), receiver);
                // Cada share individual tambien lleva su propio token (compatibilidad
                // con getListByPublicToken y futuras urls por share).
                share.setShareToken(UUID.randomUUID().toString());
                shareRepository.save(share);

                String notificationMessage = message != null && !message.isEmpty()
                        ? list.getUser().getFullName() + " te ha compartido esta lista: " + list.getName() + " - " + message
                        : list.getUser().getFullName() + " te ha compartido esta lista: " + list.getName();

                // La notificacion lleva el token publico de la lista (NO el id
                // interno) para que el link del cliente apunte a /shared/{token}.
                notificationService.createNotificationWithShareToken(
                        friendId, NotificationType.SHARED, notificationMessage, list.getId(), listPublicToken);

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

    /**
     * Asegura que la lista tenga un token publico persistido cuando su
     * visibilidad es LISTED o PUBLIC. Si ya tiene uno, lo reutiliza.
     * Esto es lo que usa la URL /shared/{token} y la notificacion que
     * recibe el destinatario.
     */
    private String ensurePublicToken(UserList list) {
        if (list.getVisibility() == ListVisibility.PRIVATE) {
            return null;
        }
        String current = list.getPublicToken();
        if (current == null || current.isBlank()) {
            String generated = UUID.randomUUID().toString();
            list.setPublicToken(generated);
            listRepository.save(list);
            return generated;
        }
        return current;
    }

    @Override
    @Transactional
    public void revokeShare(Long shareId, Long ownerId) {
        UserListShare share = shareRepository.findById(shareId)
                .orElseThrow(() -> new ResourceNotFoundException("Share no encontrado con id: " + shareId));

        if (!share.getOwner().getId().equals(ownerId)) {
            throw new ForbiddenException("No eres el dueño de este share");
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
        // 1) Primero intentamos resolver como share individual (compatibilidad
        //    con tokens por share, que ya estan en uso).
        var byShare = shareRepository.findByShareTokenAndIsActiveTrue(token);
        if (byShare.isPresent()) {
            return mapToDTO(byShare.get(), true);
        }

        // 2) Si no, lo tratamos como el publicToken de la lista (que es
        //    lo que la notificacion y la URL /shared/{token} usan hoy).
        UserList list = listRepository.findByPublicToken(token)
                .orElseThrow(() -> new com.lectuaria.backend.exception.list.InvalidShareTokenException("Token de share inválido o expirado"));

        if (list.getVisibility() == ListVisibility.PRIVATE) {
            // Un token publico no deberia existir para listas privadas, pero
            // por seguridad, si llega, lo rechazamos.
            throw new com.lectuaria.backend.exception.list.InvalidShareTokenException("Token de share inválido o expirado");
        }

        return mapListToPublicShareDTO(list);
    }

    /**
     * Construye un UserListShareDTO a partir de un UserList cuando el acceso
     * es por token publico de la lista (no por share individual). En este
     * caso no hay shareId, ni receiverId, ni sharedAt: el DTO lo refleja
     * con nulls (sus getters/setters ya aceptan null).
     */
    private UserListShareDTO mapListToPublicShareDTO(UserList list) {
        List<BookSummaryDTO> books = listBookRepository
                .findByUserListIdOrderByAddedAtDesc(list.getId())
                .stream()
                .map(ulb -> mapBookToSummaryDTO(ulb.getBook()))
                .collect(Collectors.toList());

        return new UserListShareDTO(
                null,                                  // id del share (no aplica)
                list.getId(),
                list.getName(),
                list.getDescription(),
                list.getUser().getId(),
                list.getUser().getFullName(),
                null,                                  // receiverId (no aplica)
                null,                                  // receiverName (no aplica)
                list.getCreatedAt(),                   // usamos createdAt de la lista como "momento"
                true,                                  // isActive (por construccion, listas PUBLIC/LISTED son accesibles)
                books,
                list.getPublicToken()
        );
    }

    /**
     * Resuelve qué token devolver en el campo {@code publicToken} del DTO
     * cuando el DTO se construye a partir de un share individual (caso
     * "compartidos conmigo").
     *
     * La regla debe coincidir con la del resolver {@link #getListByPublicToken}:
     *  - Listas PRIVATE nunca deben aparecer aqui (no se comparten), pero si
     *    llegara a colarse una, devolvemos null para que el front haga fallback
     *    a la ruta por id y no se filtre un token.
     *  - Listas LISTED: preferimos el shareToken del share (mantiene
     *    compatibilidad con tokens por share que ya estan en uso), y como
     *    respaldo el publicToken de la lista si el share no tiene token.
     *  - Listas PUBLIC: solo el publicToken de la lista (no hay share
     *    individual significativo para esta visibilidad).
     */
    private String resolvePublicTokenForListing(UserListShare share) {
        if (share.getList() == null) {
            return null;
        }
        ListVisibility visibility = share.getList().getVisibility();
        if (visibility == ListVisibility.PRIVATE) {
            return null;
        }
        if (visibility == ListVisibility.LISTED) {
            return share.getShareToken() != null
                    ? share.getShareToken()
                    : share.getList().getPublicToken();
        }
        // PUBLIC
        return share.getList().getPublicToken();
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
                resolvePublicTokenForListing(share)
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