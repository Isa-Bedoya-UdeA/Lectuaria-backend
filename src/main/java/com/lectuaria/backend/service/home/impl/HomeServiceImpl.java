package com.lectuaria.backend.service.home.impl;

import com.lectuaria.backend.dto.book.BookSummaryDTO;
import com.lectuaria.backend.dto.book.GenreDTO;
import com.lectuaria.backend.dto.home.FriendActivityDTO;
import com.lectuaria.backend.dto.home.HomeResponseDTO;
import com.lectuaria.backend.dto.recommendation.RecommendationDTO;
import com.lectuaria.backend.model.auth.User;
import com.lectuaria.backend.model.book.Author;
import com.lectuaria.backend.model.book.Book;
import com.lectuaria.backend.model.book.BookReview;
import com.lectuaria.backend.model.book.ReviewStatus;
import com.lectuaria.backend.model.friendship.Friendship;
import com.lectuaria.backend.model.list.UserListBook;
import com.lectuaria.backend.repository.book.BookRatingRepository;
import com.lectuaria.backend.repository.book.BookRepository;
import com.lectuaria.backend.repository.book.BookReviewRepository;
import com.lectuaria.backend.repository.friendship.FriendshipRepository;
import com.lectuaria.backend.repository.list.UserListBookRepository;
import com.lectuaria.backend.service.book.IBookService;
import com.lectuaria.backend.service.home.IHomeService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class HomeServiceImpl implements IHomeService {
    private static final int MIN_RECOMMENDATIONS = 10;
    private static final Duration RECOMMENDATION_REFRESH_INTERVAL = Duration.ofDays(7);

    private final FriendshipRepository friendshipRepository;
    private final UserListBookRepository listBookRepository;
    private final BookReviewRepository reviewRepository;
    private final BookRatingRepository ratingRepository;
    private final BookRepository bookRepository;
    private final IBookService bookService;
    private final Map<Long, Set<Long>> hiddenRecommendations = new ConcurrentHashMap<>();

    public HomeServiceImpl(FriendshipRepository friendshipRepository, UserListBookRepository listBookRepository,
            BookReviewRepository reviewRepository, BookRatingRepository ratingRepository, BookRepository bookRepository,
            IBookService bookService) {
        this.friendshipRepository = friendshipRepository;
        this.listBookRepository = listBookRepository;
        this.reviewRepository = reviewRepository;
        this.ratingRepository = ratingRepository;
        this.bookRepository = bookRepository;
        this.bookService = bookService;
    }

    public HomeResponseDTO getHome(User user, Long genreId, String formatName) {
        return new HomeResponseDTO(
                getFriendActivity(user, 20),
                bookService.getNewCatalogBooks(0, 12, genreId, formatName).getContent(),
                bookService.getFeaturedSections(),
                getRecommendations(user, MIN_RECOMMENDATIONS));
    }

    public List<RecommendationDTO> getRecommendations(User user, int size) {
        int requestedSize = Math.max(size, MIN_RECOMMENDATIONS);
        Instant generatedAt = Instant.now();
        Instant nextRefreshAt = generatedAt.plus(RECOMMENDATION_REFRESH_INTERVAL);

        Set<Long> excludedIds = new HashSet<>(ratingRepository.findRatedBookIdsByUserId(user.getId()));
        excludedIds.addAll(listBookRepository.findBookIdsByUserId(user.getId()));
        excludedIds.addAll(hiddenRecommendations.getOrDefault(user.getId(), Set.of()));

        List<Object[]> genreRows = new ArrayList<>();
        genreRows.addAll(ratingRepository.findTopGenresByUserRatings(user.getId(), PageRequest.of(0, 10)));
        genreRows.addAll(listBookRepository.findTopGenresByUserLists(user.getId(), PageRequest.of(0, 10)));

        LinkedHashMap<Long, String> genreNames = new LinkedHashMap<>();
        for (Object[] row : genreRows) {
            genreNames.putIfAbsent(((Number) row[0]).longValue(), (String) row[1]);
        }

        List<Book> candidates = genreNames.isEmpty()
                ? new ArrayList<>()
                : bookRepository.findRecommendationsByGenreIds(new ArrayList<>(genreNames.keySet()),
                        PageRequest.of(0, requestedSize * 5));

        if (candidates.size() < requestedSize) {
            candidates.addAll(bookRepository.findFallbackRecommendations(PageRequest.of(0, requestedSize * 5)));
        }

        Set<Long> seen = new HashSet<>();
        return candidates.stream()
                .filter(book -> !excludedIds.contains(book.getId()))
                .filter(book -> seen.add(book.getId()))
                .limit(requestedSize)
                .map(book -> new RecommendationDTO(toSummaryDTO(book), buildRecommendationReason(book, genreNames),
                        generatedAt, nextRefreshAt))
                .toList();
    }

    @Transactional
    public void hideRecommendation(User user, Long bookId) {
        hiddenRecommendations.computeIfAbsent(user.getId(), ignored -> ConcurrentHashMap.newKeySet()).add(bookId);
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

    private String buildRecommendationReason(Book book, Map<Long, String> preferredGenres) {
        if (book.getGenres() != null) {
            for (GenreDTO genre : book.getGenres().stream()
                    .map(g -> new GenreDTO(g.getId(), g.getName(), g.getDescription())).toList()) {
                if (preferredGenres.containsKey(genre.getId())) {
                    return "Sugerido porque has guardado o calificado libros de " + genre.getName();
                }
            }
        }
        if (book.getAverageRating() != null && book.getAverageRating().compareTo(BigDecimal.valueOf(4)) >= 0) {
            return "Sugerido por su alta calificación en la comunidad";
        }
        return "Sugerido para descubrir nuevos libros populares";
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
