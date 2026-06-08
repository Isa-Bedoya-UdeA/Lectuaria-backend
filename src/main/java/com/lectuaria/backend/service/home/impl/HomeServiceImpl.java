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
import com.lectuaria.backend.service.home.recommendation.RecommendationStrategy;
import com.lectuaria.backend.model.book.UserRecommendation;
import com.lectuaria.backend.repository.book.UserRecommendationRepository;
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
import java.util.Optional;
import java.util.Set;
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
    private final UserRecommendationRepository userRecommendationRepository;
    private final List<RecommendationStrategy> recommendationStrategies;

    public HomeServiceImpl(FriendshipRepository friendshipRepository, UserListBookRepository listBookRepository,
            BookReviewRepository reviewRepository, BookRatingRepository ratingRepository, BookRepository bookRepository,
            IBookService bookService, UserRecommendationRepository userRecommendationRepository,
            List<RecommendationStrategy> recommendationStrategies) {
        this.friendshipRepository = friendshipRepository;
        this.listBookRepository = listBookRepository;
        this.reviewRepository = reviewRepository;
        this.ratingRepository = ratingRepository;
        this.bookRepository = bookRepository;
        this.bookService = bookService;
        this.userRecommendationRepository = userRecommendationRepository;
        this.recommendationStrategies = recommendationStrategies;
    }

    public HomeResponseDTO getHome(User user, Long genreId) {
        return new HomeResponseDTO(
                getFriendActivity(user, 20),
                bookService.getNewCatalogBooks(0, 12, genreId).getContent(),
                bookService.getFeaturedSections(),
                getRecommendations(user, MIN_RECOMMENDATIONS));
    }

    @Transactional
    public List<RecommendationDTO> getRecommendations(User user, int size) {
        int requestedSize = Math.max(size, MIN_RECOMMENDATIONS);
        Instant sevenDaysAgo = Instant.now().minus(RECOMMENDATION_REFRESH_INTERVAL);

        // 1. Try to fetch active (non-hidden) records from DB
        List<UserRecommendation> activeRecs = userRecommendationRepository.findByUserIdAndHiddenFalse(user.getId());

        if (!activeRecs.isEmpty()) {
            UserRecommendation firstRec = activeRecs.get(0);
            if (firstRec.getCalculatedAt() != null && firstRec.getCalculatedAt().isAfter(sevenDaysAgo)) {
                return activeRecs.stream()
                        .map(rec -> new RecommendationDTO(
                                toSummaryDTO(rec.getBook()),
                                rec.getReason(),
                                rec.getCalculatedAt(),
                                rec.getCalculatedAt().plus(RECOMMENDATION_REFRESH_INTERVAL)
                        ))
                        .collect(Collectors.toList());
            }
        }

        // 2. If no records OR stale (>7 days): delete active records, fetch hidden, compute new
        userRecommendationRepository.deleteActiveByUserId(user.getId());

        Set<Long> excludedIds = new HashSet<>(ratingRepository.findRatedBookIdsByUserId(user.getId()));
        excludedIds.addAll(listBookRepository.findBookIdsByUserId(user.getId()));
        excludedIds.addAll(userRecommendationRepository.findHiddenBookIdsByUserId(user.getId()));

        // Mapa de generos preferidos del usuario, para personalizar las razones.
        LinkedHashMap<Long, String> genreNames = new LinkedHashMap<>();
        for (Object[] row : ratingRepository.findTopGenresByUserRatings(user.getId(), PageRequest.of(0, 10))) {
            genreNames.putIfAbsent(((Number) row[0]).longValue(), (String) row[1]);
        }
        for (Object[] row : listBookRepository.findTopGenresByUserLists(user.getId(), PageRequest.of(0, 10))) {
            genreNames.putIfAbsent(((Number) row[0]).longValue(), (String) row[1]);
        }

        // Orquestacion de la cadena de RecommendationStrategy (GoF Strategy).
        // Cada estrategia aporta candidatos; se filtran los excluidos y se
        // desduplican. Si tras la primera cadena no hay suficientes, se cae
        // a la siguiente (e.g. fallback de "libros populares con rating alto").
        List<Book> candidates = new ArrayList<>();
        for (RecommendationStrategy strategy : recommendationStrategies) {
            if (candidates.size() >= requestedSize) {
                break;
            }
            try {
                List<Book> fromStrategy = strategy.selectCandidates(user, requestedSize * 5);
                if (fromStrategy != null) {
                    candidates.addAll(fromStrategy);
                }
            } catch (Exception e) {
                // Una estrategia que falla no debe tumbar toda la cadena.
                org.slf4j.LoggerFactory.getLogger(HomeServiceImpl.class)
                        .warn("Recommendation strategy {} failed: {}", strategy.name(), e.getMessage());
            }
        }

        Set<Long> seen = new HashSet<>();
        List<Book> selectedBooks = candidates.stream()
                .filter(book -> !excludedIds.contains(book.getId()))
                .filter(book -> seen.add(book.getId()))
                .limit(requestedSize)
                .toList();

        Instant generatedAt = Instant.now();
        List<UserRecommendation> newRecs = new ArrayList<>();
        for (Book book : selectedBooks) {
            // Construir la razon desde la primera estrategia que aporte una.
            // Esto preserva el comportamiento original: si preference-based
            // no matchea el genero, se usa el mensaje de fallback.
            String reason = null;
            for (RecommendationStrategy strategy : recommendationStrategies) {
                reason = strategy.buildReason(book, genreNames);
                if (reason != null) {
                    break;
                }
            }
            if (reason == null) {
                reason = "Sugerido para ti";
            }
            BigDecimal score = buildRecommendationScore(book);
            UserRecommendation rec = new UserRecommendation(user, book, reason, score);
            rec.setCalculatedAt(generatedAt);
            newRecs.add(rec);
        }

        userRecommendationRepository.saveAll(newRecs);

        // Sort by score descending, then by book rating for tie-breaking
        return newRecs.stream()
                .sorted(Comparator.comparing(UserRecommendation::getScore,
                        Comparator.nullsLast(Comparator.reverseOrder())).reversed()
                        .thenComparing((rec1, rec2) -> {
                            BigDecimal r1 = rec1.getBook().getAverageRating() != null ? rec1.getBook().getAverageRating() : BigDecimal.ZERO;
                            BigDecimal r2 = rec2.getBook().getAverageRating() != null ? rec2.getBook().getAverageRating() : BigDecimal.ZERO;
                            int ratingCompare = r2.compareTo(r1);
                            if (ratingCompare != 0) return ratingCompare;
                            Integer c1 = rec1.getBook().getRatingsCount() != null ? rec1.getBook().getRatingsCount() : 0;
                            Integer c2 = rec2.getBook().getRatingsCount() != null ? rec2.getBook().getRatingsCount() : 0;
                            return c2.compareTo(c1);
                        }))
                .map(rec -> new RecommendationDTO(
                        toSummaryDTO(rec.getBook()),
                        rec.getReason(),
                        rec.getCalculatedAt(),
                        rec.getCalculatedAt().plus(RECOMMENDATION_REFRESH_INTERVAL)
                ))
                .collect(Collectors.toList());
    }

    @Transactional
    public void hideRecommendation(User user, Long bookId) {
        Optional<UserRecommendation> existing = userRecommendationRepository.findByUserIdAndBookId(user.getId(), bookId);
        if (existing.isPresent()) {
            UserRecommendation rec = existing.get();
            rec.setHidden(true);
            userRecommendationRepository.save(rec);
        } else {
            Book book = bookRepository.findById(bookId)
                    .orElseThrow(() -> new IllegalArgumentException("Book not found with id: " + bookId));
            UserRecommendation rec = new UserRecommendation(user, book, "Ocultado por el usuario", BigDecimal.ZERO);
            rec.setHidden(true);
            rec.setCalculatedAt(Instant.now());
            userRecommendationRepository.save(rec);
        }
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

    private BigDecimal buildRecommendationScore(Book book) {
        BigDecimal rating = book.getAverageRating() != null ? book.getAverageRating() : BigDecimal.ZERO;
        Integer count = book.getRatingsCount() != null ? book.getRatingsCount() : 0;
        // Score: weighted combination of rating and number of ratings
        // Books with high rating AND many ratings get top priority
        // Add small bonus for count to differentiate books with same rating
        return rating.add(BigDecimal.valueOf(Math.min(count, 1000) / 1000.0));
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
                book.getCreatedBy() != null ? book.getCreatedBy().getId() : null,
                book.getCreatedAt());
    }
}
