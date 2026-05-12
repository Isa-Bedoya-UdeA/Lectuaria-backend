package com.lectuaria.backend.service.book.impl;

import com.lectuaria.backend.dto.book.BookShareRequestDTO;
import com.lectuaria.backend.dto.book.BookShareResponseDTO;
import com.lectuaria.backend.dto.book.ShareResultDTO;
import com.lectuaria.backend.exception.ConflictException;
import com.lectuaria.backend.exception.ResourceNotFoundException;
import com.lectuaria.backend.exception.list.AlreadySharedException;
import com.lectuaria.backend.model.auth.User;
import com.lectuaria.backend.model.book.Book;
import com.lectuaria.backend.model.book.BookShare;
import com.lectuaria.backend.model.notification.NotificationType;
import com.lectuaria.backend.repository.auth.UserRepository;
import com.lectuaria.backend.repository.book.BookRepository;
import com.lectuaria.backend.repository.book.BookShareRepository;
import com.lectuaria.backend.service.book.IBookShareService;
import com.lectuaria.backend.service.notification.INotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BookShareServiceImpl implements IBookShareService {

    private final BookShareRepository bookShareRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final INotificationService notificationService;

    public BookShareServiceImpl(BookShareRepository bookShareRepository,
                               BookRepository bookRepository,
                               UserRepository userRepository,
                               INotificationService notificationService) {
        this.bookShareRepository = bookShareRepository;
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public ShareResultDTO shareBookWithFriends(Long bookId, BookShareRequestDTO request, User sender) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Libro no encontrado"));

        int successfulShares = 0;
        int failedShares = 0;
        List<String> errorMessages = new ArrayList<>();
        List<String> alreadySharedFriends = new ArrayList<>();
        List<String> otherErrors = new ArrayList<>();

        for (Long friendId : request.getFriendIds()) {
            User receiver = userRepository.findById(friendId)
                    .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

            if (bookShareRepository.existsBySenderAndReceiverAndBook(sender.getId(), friendId, bookId)) {
                failedShares++;
                alreadySharedFriends.add(receiver.getFullName());
                continue;
            }

            try {
                BookShare bookShare = new BookShare(sender, receiver, book, request.getMessage());
                bookShare = bookShareRepository.save(bookShare);

                String message = request.getMessage() != null && !request.getMessage().isEmpty()
                        ? sender.getFullName() + " te ha compartido este libro: " + book.getTitle() + " - " + request.getMessage()
                        : sender.getFullName() + " te ha compartido este libro: " + book.getTitle();

                notificationService.createNotification(
                        friendId,
                        NotificationType.SHARED,
                        message,
                        book.getIsbn()
                );
                successfulShares++;
            } catch (Exception e) {
                failedShares++;
                otherErrors.add("Error al compartir con " + receiver.getFullName() + ": " + e.getMessage());
            }
        }

        // Construir mensajes de error más inteligentes
        if (!alreadySharedFriends.isEmpty()) {
            if (alreadySharedFriends.size() == 1) {
                errorMessages.add("El libro ya ha sido compartido con " + alreadySharedFriends.get(0));
            } else if (alreadySharedFriends.size() == 2) {
                errorMessages.add("El libro ya ha sido compartido con " + alreadySharedFriends.get(0) + " y " + alreadySharedFriends.get(1));
            } else {
                // 3 o más amigos
                String friendsList = String.join(", ", alreadySharedFriends.subList(0, alreadySharedFriends.size() - 1)) 
                    + " y " + alreadySharedFriends.get(alreadySharedFriends.size() - 1);
                errorMessages.add("El libro ya ha sido compartido con " + friendsList);
            }
        }
        errorMessages.addAll(otherErrors);

        String resultMessage;
        if (failedShares == 0) {
            resultMessage = "Libro compartido exitosamente con " + successfulShares + " amigo(s)";
        } else if (successfulShares == 0) {
            resultMessage = "No se pudo compartir el libro con ningún amigo";
        } else {
            resultMessage = "Libro compartido con " + successfulShares + " amigo(s), pero falló con " + failedShares + " amigo(s)";
        }

        return new ShareResultDTO(successfulShares, failedShares, errorMessages, resultMessage);
    }

    @Transactional(readOnly = true)
    public List<BookShareResponseDTO> getReceivedShares(User user) {
        return bookShareRepository.findByReceiverId(user.getId()).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BookShareResponseDTO> getSentShares(User user) {
        return bookShareRepository.findBySenderId(user.getId()).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private BookShareResponseDTO mapToDTO(BookShare bookShare) {
        return new BookShareResponseDTO(
                bookShare.getId(),
                bookShare.getBook().getId(),
                bookShare.getBook().getTitle(),
                bookShare.getSender().getId(),
                bookShare.getSender().getFullName(),
                bookShare.getReceiver().getId(),
                bookShare.getReceiver().getFullName(),
                bookShare.getMessage(),
                bookShare.getSharedAt()
        );
    }

    @Override
    public boolean isBookSharedWithFriend(Long senderId, Long receiverId, Long bookId) {
        return bookShareRepository.existsBySenderAndReceiverAndBook(senderId, receiverId, bookId);
    }
}
