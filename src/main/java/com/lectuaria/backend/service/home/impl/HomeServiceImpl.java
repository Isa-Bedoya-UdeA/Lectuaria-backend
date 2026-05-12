package com.lectuaria.backend.service.home.impl;

import com.lectuaria.backend.dto.book.BookSummaryDTO;
import com.lectuaria.backend.dto.book.GenreDTO;
import com.lectuaria.backend.dto.home.FriendActivityDTO;
import com.lectuaria.backend.dto.home.HomeResponseDTO;
import com.lectuaria.backend.model.auth.User;
import com.lectuaria.backend.model.book.Author;
import com.lectuaria.backend.model.book.Book;
import com.lectuaria.backend.model.book.BookReview;
import com.lectuaria.backend.model.book.ReviewStatus;
import com.lectuaria.backend.model.friendship.Friendship;
import com.lectuaria.backend.model.list.UserListBook;
import com.lectuaria.backend.repository.book.BookReviewRepository;
import com.lectuaria.backend.repository.friendship.FriendshipRepository;
import com.lectuaria.backend.repository.list.UserListBookRepository;
import com.lectuaria.backend.service.book.IBookService;
import com.lectuaria.backend.service.home.IHomeService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class HomeServiceImpl implements IHomeService {
    private final FriendshipRepository friendshipRepository;
    private final UserListBookRepository listBookRepository;
    private final BookReviewRepository reviewRepository;
    private final IBookService bookService;

    public HomeServiceImpl(FriendshipRepository friendshipRepository, UserListBookRepository listBookRepository,
            BookReviewRepository reviewRepository, IBookService bookService) {
        this.friendshipRepository = friendshipRepository;
        this.listBookRepository = listBookRepository;
        this.reviewRepository = reviewRepository;
        this.bookService = bookService;
    }

    public HomeResponseDTO getHome(User user, Long genreId, String formatName) {
        return new HomeResponseDTO(
                getFriendActivity(user, 20),
                bookService.getNewCatalogBooks(0, 12, genreId, formatName).getContent(),
                bookService.getFeaturedSections());
    }

    public List<FriendActivityDTO> getFriendActivity(User user, int size) {
        List<Long> friendIds = friendshipRepository.findFriendsByUserId(user.getId()).stream()
                .map(friendship -> getFriend(friendship, user.getId()).getId())
                .collect(Collectors.toList());

        if (friendIds.isEmpty()) {
            return List.of();
        }

        List<FriendActivityDTO> listActivities = listBookRepository
                .findRecentByUserIds(friendIds, PageRequest.of(0, size))
                .stream()
                .map(this::toListActivity)
                .collect(Collectors.toList());

        List<FriendActivityDTO> reviewActivities = reviewRepository
                .findRecentByUserIdsAndStatus(friendIds, ReviewStatus.published, PageRequest.of(0, size))
                .stream()
                .map(this::toReviewActivity)
                .collect(Collectors.toList());

        return java.util.stream.Stream.concat(listActivities.stream(), reviewActivities.stream())
                .sorted(Comparator.comparing(FriendActivityDTO::getOccurredAt).reversed())
                .limit(size)
                .collect(Collectors.toList());
    }

    private FriendActivityDTO toListActivity(UserListBook listBook) {
        User friend = listBook.getUserList().getUser();
        return new FriendActivityDTO(
                "list-" + listBook.getId(),
                friend.getId(),
                friend.getFullName(),
                friend.getUsername(),
                friend.getPhotoUrl(),
                "Agregó un libro a " + listBook.getUserList().getName(),
                listBook.getUserList().getName(),
                toSummaryDTO(listBook.getBook()),
                listBook.getAddedAt());
    }

    private FriendActivityDTO toReviewActivity(BookReview review) {
        User friend = review.getUser();
        return new FriendActivityDTO(
                "review-" + review.getId(),
                friend.getId(),
                friend.getFullName(),
                friend.getUsername(),
                friend.getPhotoUrl(),
                "Publicó una reseña",
                null,
                toSummaryDTO(review.getBook()),
                review.getPublishedAt() != null ? review.getPublishedAt() : review.getCreatedAt());
    }

    private User getFriend(Friendship friendship, Long userId) {
        return friendship.getUser1().getId().equals(userId) ? friendship.getUser2() : friendship.getUser1();
    }

    private BookSummaryDTO toSummaryDTO(Book book) {
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
                null,
                null,
                book.getCreatedBy() != null ? book.getCreatedBy().getId() : null);
    }
}
