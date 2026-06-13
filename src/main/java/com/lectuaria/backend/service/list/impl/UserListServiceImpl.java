package com.lectuaria.backend.service.list.impl;

import com.lectuaria.backend.dto.book.BookSummaryDTO;
import com.lectuaria.backend.service.list.IUserListService;
import com.lectuaria.backend.dto.book.GenreDTO;
import com.lectuaria.backend.dto.list.CreateListRequestDTO;
import com.lectuaria.backend.dto.list.UpdateListRequestDTO;
import com.lectuaria.backend.dto.list.UserListDTO;
import com.lectuaria.backend.dto.list.MoveBookResponseDTO;
import com.lectuaria.backend.exception.ConflictException;
import com.lectuaria.backend.exception.UnauthorizedException;
import com.lectuaria.backend.model.auth.User;
import com.lectuaria.backend.model.auth.UserRole;
import com.lectuaria.backend.model.book.Author;
import com.lectuaria.backend.model.book.Book;
import com.lectuaria.backend.model.book.Genre;
import com.lectuaria.backend.model.list.ListType;
import com.lectuaria.backend.model.list.ListVisibility;
import com.lectuaria.backend.model.list.UserList;
import com.lectuaria.backend.model.list.UserListBook;
import com.lectuaria.backend.repository.book.BookRepository;
import com.lectuaria.backend.repository.list.UserListBookRepository;
import com.lectuaria.backend.repository.list.UserListRepository;
import com.lectuaria.backend.repository.list.UserListShareRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserListServiceImpl implements IUserListService {
    private static final String FAVORITES_LIST_NAME = "Favoritos";

    private final UserListRepository listRepository;
    private final UserListBookRepository listBookRepository;
    private final BookRepository bookRepository;
    private final UserListShareRepository listShareRepository;

    public UserListServiceImpl(UserListRepository listRepository, 
                           UserListBookRepository listBookRepository,
                           BookRepository bookRepository,
                           UserListShareRepository listShareRepository) {
        this.listRepository = listRepository;
        this.listBookRepository = listBookRepository;
        this.bookRepository = bookRepository;
        this.listShareRepository = listShareRepository;
    }

    @Transactional
    public void createDefaultLists(User user) {
        if (user.getRole() == UserRole.LIBRARIAN) return;

        createListIfNotExist(user, "Por leer", "Libros que planeo leer en el futuro.", ListType.SYSTEM, ListVisibility.LISTED);
        createListIfNotExist(user, "Leyendo", "Libros que estoy leyendo actualmente.", ListType.SYSTEM, ListVisibility.LISTED);
        createListIfNotExist(user, "Leídos", "Libros que ya he terminado de leer.", ListType.SYSTEM, ListVisibility.LISTED);
        createListIfNotExist(user, FAVORITES_LIST_NAME, "Libros marcados como favoritos.", ListType.SYSTEM, ListVisibility.LISTED);
    }

    private void createListIfNotExist(User user, String name, String description, ListType type, ListVisibility visibility) {
        if (listRepository.findByUserIdAndName(user.getId(), name).isEmpty()) {
            listRepository.save(new UserList(user, name, description, type, visibility));
        }
    }

    @Transactional
    public UserListDTO createCustomList(CreateListRequestDTO request, User user) {
        if (user.getRole() == UserRole.LIBRARIAN) {
            throw new ConflictException("Los bibliotecarios no pueden tener listas de lectura.", List.of());
        }

        if (listRepository.findByUserIdAndName(user.getId(), request.getName()).isPresent()) {
            throw new ConflictException("Ya tienes una lista con este nombre.", List.of());
        }

        UserList newList = new UserList(user, request.getName(), request.getDescription(), ListType.CUSTOM, request.getVisibility());
        UserList saved = listRepository.save(newList);
        return mapToDTO(saved);
    }

    public List<UserListDTO> getUserLists(Long userId) {
        return listRepository.findByUserIdOrderByCreatedAtAsc(userId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public UserListDTO getListDetails(Long listId, Long userId) {
        UserList list = listRepository.findById(listId)
                .orElseThrow(() -> new IllegalArgumentException("Lista no encontrada"));

        // If private, only owner can see it
        if (list.getVisibility() == ListVisibility.PRIVATE && !list.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("Esta lista es privada");
        }

        UserListDTO dto = mapToDTO(list);
        
        // Load books
        List<BookSummaryDTO> books = listBookRepository.findByUserListIdOrderByAddedAtDesc(listId)
                .stream()
                .map(ulb -> mapBookToSummaryDTO(ulb.getBook()))
                .collect(Collectors.toList());
        
        dto.setBooks(books);
        return dto;
    }

    @Transactional
    public void addBookToList(Long listId, Long bookId, User user, boolean forceMove) {
        UserList targetList = listRepository.findById(listId)
                .orElseThrow(() -> new IllegalArgumentException("Lista no encontrada"));

        if (!targetList.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("No tienes permiso para modificar esta lista");
        }

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Libro no encontrado"));

        // Verificar si el libro YA está en ESTA lista específica
        Optional<UserListBook> existingInSameList = listBookRepository.findByUserListIdAndBookId(listId, bookId);
        
        if (existingInSameList.isPresent()) {
            throw new ConflictException("El libro ya se encuentra en esta lista", List.of());
        }

        // Simplemente guardamos la nueva asociación sin borrar las anteriores
        listBookRepository.save(new UserListBook(targetList, book));
    }

    @Transactional
    public MoveBookResponseDTO moveBookBetweenLists(Long sourceListId, Long targetListId, Long bookId, User user) {
        if (sourceListId.equals(targetListId)) {
            throw new ConflictException("La lista origen y destino deben ser diferentes.", List.of());
        }

        UserList sourceList = listRepository.findById(sourceListId)
                .orElseThrow(() -> new IllegalArgumentException("Lista origen no encontrada"));
        UserList targetList = listRepository.findById(targetListId)
                .orElseThrow(() -> new IllegalArgumentException("Lista destino no encontrada"));

        if (!sourceList.getUser().getId().equals(user.getId()) || !targetList.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("No tienes permiso para modificar estas listas");
        }

        UserListBook sourceMembership = listBookRepository.findByUserListIdAndBookId(sourceListId, bookId)
                .orElseThrow(() -> new IllegalArgumentException("El libro no está en la lista origen"));

        if (listBookRepository.findByUserListIdAndBookId(targetListId, bookId).isPresent()) {
            throw new ConflictException("El libro ya se encuentra en la lista destino", List.of());
        }

        Book book = sourceMembership.getBook();
        listBookRepository.delete(sourceMembership);
        listBookRepository.save(new UserListBook(targetList, book));

        return new MoveBookResponseDTO(
                bookId,
                sourceListId,
                targetListId,
                listBookRepository.countByListId(sourceListId),
                listBookRepository.countByListId(targetListId),
                "Libro movido correctamente a " + targetList.getName(),
                Instant.now());
    }

    @Transactional
    public void removeBookFromList(Long listId, Long bookId, User user) {
        UserList targetList = listRepository.findById(listId)
                .orElseThrow(() -> new IllegalArgumentException("Lista no encontrada"));

        if (!targetList.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("No tienes permiso para modificar esta lista");
        }

        Optional<UserListBook> membership = listBookRepository.findByUserListIdAndBookId(listId, bookId);
        membership.ifPresent(listBookRepository::delete);
    }

    @Transactional
    public void deleteList(Long listId, User user, boolean confirmDelete, boolean forceDeleteWithBooks) {
        UserList list = listRepository.findById(listId)
                .orElseThrow(() -> new IllegalArgumentException("Lista no encontrada"));

        if (!list.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("No tienes permiso para eliminar esta lista");
        }

        if (list.getListType() != ListType.CUSTOM) {
            throw new ConflictException("Solo se pueden eliminar listas personalizadas.", List.of());
        }

        if (!confirmDelete) {
            throw new ConflictException("Confirma la eliminación para continuar.", List.of());
        }

        long linkedBooks = listBookRepository.countByUserListId(listId);
        if (linkedBooks > 0 && !forceDeleteWithBooks) {
            throw new ConflictException(
                    "La lista contiene libros. Si deseas eliminarla de forma permanente y desvincular sus libros, usa force=true.",
                    List.of()
            );
        }

        listBookRepository.deleteByUserListId(listId);
        listShareRepository.deleteByListId(listId);
        listRepository.delete(list);
    }

    @Transactional
    public boolean toggleFavorite(Long bookId, User user) {
        UserList favoriteList = getFavoriteList(user);
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Libro no encontrado"));

        Optional<UserListBook> existingFavorite = listBookRepository.findByUserListIdAndBookId(favoriteList.getId(), bookId);

        if (existingFavorite.isPresent()) {
            listBookRepository.delete(existingFavorite.get());
            return false;
        }

        listBookRepository.save(new UserListBook(favoriteList, book));
        return true;
    }

    public UserListDTO getMyFavorites(User user) {
        UserList favoriteList = getFavoriteList(user);
        return getListDetails(favoriteList.getId(), user.getId());
    }

    @Override
    @Transactional
    public UserListDTO updateCustomList(Long listId, UpdateListRequestDTO request, User user) {
        if (user.getRole() == UserRole.LIBRARIAN) {
            throw new ConflictException("Los bibliotecarios no pueden editar listas de lectura.", List.of());
        }

        UserList list = listRepository.findById(listId)
                .orElseThrow(() -> new IllegalArgumentException("Lista no encontrada"));

        if (list.getListType() != ListType.CUSTOM) {
            throw new ConflictException("Solo se pueden editar listas personalizadas.", List.of());
        }

        if (!list.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("No tienes permiso para editar esta lista");
        }

        // Aplicar cambios de forma parcial: solo lo que venga en el request.
        if (request.getName() != null && !request.getName().isBlank()) {
            String newName = request.getName().trim();
            if (!newName.equals(list.getName())
                    && listRepository.findByUserIdAndName(user.getId(), newName).isPresent()) {
                throw new ConflictException("Ya tienes una lista con este nombre.", List.of());
            }
            list.setName(newName);
        }

        if (request.getDescription() != null) {
            list.setDescription(request.getDescription().isBlank() ? null : request.getDescription().trim());
        }

        if (request.getVisibility() != null && request.getVisibility() != list.getVisibility()) {
            list.setVisibility(request.getVisibility());

            // Si pasa a PRIVATE, no puede seguir compartida con nadie. Desactivamos
            // los shares existentes (no los borramos: queda historial para auditoria).
            if (request.getVisibility() == ListVisibility.PRIVATE) {
                listShareRepository.deactivateAllByListId(listId);
                list.setPublicToken(null);
            } else if (list.getPublicToken() == null || list.getPublicToken().isBlank()) {
                // Si pasa a LISTED/PUBLIC, garantizamos que tenga token publico.
                list.setPublicToken(UUID.randomUUID().toString());
            }
        }

        UserList saved = listRepository.save(list);
        return mapToDTO(saved);
    }

    private UserList getFavoriteList(User user) {
        return listRepository.findByUserIdAndNameAndListType(user.getId(), FAVORITES_LIST_NAME, ListType.SYSTEM)
                .orElseGet(() -> listRepository.save(
                        new UserList(user, FAVORITES_LIST_NAME, "Libros marcados como favoritos.", ListType.SYSTEM, ListVisibility.LISTED)
                ));
    }

    private UserListDTO mapToDTO(UserList list) {
        long count = listBookRepository.countByListId(list.getId());
        UserListDTO dto = new UserListDTO(
                list.getId(),
                list.getName(),
                list.getDescription(),
                list.getListType(),
                list.getVisibility(),
                count,
                list.getCreatedAt()
        );
        dto.setUserId(list.getUser().getId());
        return dto;
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
                book.getCreatedBy() != null ? book.getCreatedBy().getId() : null,
                book.getCreatedAt()
        );
    }
}
