package com.lectuaria.backend.service.user.impl;

import com.lectuaria.backend.dto.user.FriendActivityDTO;
import com.lectuaria.backend.dto.user.FriendshipStatus;
import com.lectuaria.backend.dto.user.UserProfileDTO;
import com.lectuaria.backend.dto.user.UserStatsDTO;
import com.lectuaria.backend.dto.statistics.GenreCountDTO;
import com.lectuaria.backend.dto.statistics.MonthlyBooksReadDTO;
import com.lectuaria.backend.dto.statistics.ReadingStatisticsDTO;
import com.lectuaria.backend.dto.statistics.YearComparisonDTO;
import com.lectuaria.backend.exception.ResourceNotFoundException;
import com.lectuaria.backend.model.auth.User;
import com.lectuaria.backend.model.book.BookReview;
import com.lectuaria.backend.model.book.ReviewStatus;
import com.lectuaria.backend.model.friendship.Friendship;
import com.lectuaria.backend.model.friendship.FriendshipRequest;
import com.lectuaria.backend.model.list.UserListBook;
import com.lectuaria.backend.model.list.ListVisibility;
import com.lectuaria.backend.repository.auth.UserRepository;
import com.lectuaria.backend.repository.book.BookReviewRepository;
import com.lectuaria.backend.repository.book.BookRatingRepository;
import com.lectuaria.backend.repository.friendship.FriendshipRepository;
import com.lectuaria.backend.repository.friendship.FriendshipRequestRepository;
import com.lectuaria.backend.repository.list.UserListBookRepository;
import com.lectuaria.backend.repository.list.UserListShareLinkRepository;
import com.lectuaria.backend.repository.list.UserListShareRepository;
import com.lectuaria.backend.model.list.UserListShareLink;
import com.lectuaria.backend.model.list.UserListShare;
import com.lectuaria.backend.service.user.IUserProfileService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class UserProfileServiceImpl implements IUserProfileService {

    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;
    private final FriendshipRequestRepository friendshipRequestRepository;
    private final BookReviewRepository bookReviewRepository;
    private final BookRatingRepository bookRatingRepository;
    private final UserListBookRepository userListBookRepository;
    private final UserListShareLinkRepository userListShareLinkRepository;
    private final UserListShareRepository userListShareRepository;

    public UserProfileServiceImpl(UserRepository userRepository,
                                   FriendshipRepository friendshipRepository,
                                   FriendshipRequestRepository friendshipRequestRepository,
                                   BookReviewRepository bookReviewRepository,
                                   BookRatingRepository bookRatingRepository,
                                   UserListBookRepository userListBookRepository,
                                   UserListShareLinkRepository userListShareLinkRepository,
                                   UserListShareRepository userListShareRepository) {
        this.userRepository = userRepository;
        this.friendshipRepository = friendshipRepository;
        this.friendshipRequestRepository = friendshipRequestRepository;
        this.bookReviewRepository = bookReviewRepository;
        this.bookRatingRepository = bookRatingRepository;
        this.userListBookRepository = userListBookRepository;
        this.userListShareLinkRepository = userListShareLinkRepository;
        this.userListShareRepository = userListShareRepository;
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
        Integer reviewsCount = (int) bookReviewRepository.countByUserIdAndStatus(user.getId(), ReviewStatus.published);
        Integer favoritesCount = (int) userListBookRepository.countDistinctByUserListUserId(user.getId());
        Integer booksRead = (int) bookRatingRepository.countDistinctByUserId(user.getId());

        return new UserStatsDTO(booksRead, reviewsCount, friendsCount, favoritesCount);
    }

    @Transactional(readOnly = true)
    public ReadingStatisticsDTO getReadingStatistics(String usernameSlug) {
        User user = userRepository.findByUsernameIgnoreCase(usernameSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        int currentYear = LocalDate.now(ZoneOffset.UTC).getYear();
        Instant currentYearStart = LocalDate.of(currentYear, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant nextYearStart = LocalDate.of(currentYear + 1, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant previousYearStart = LocalDate.of(currentYear - 1, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC);

        Map<Integer, Long> monthlyCounts = bookRatingRepository
                .countBooksReadByMonth(user.getId(), currentYearStart, nextYearStart)
                .stream()
                .collect(Collectors.toMap(row -> ((Number) row[0]).intValue(), row -> ((Number) row[1]).longValue()));

        List<MonthlyBooksReadDTO> booksReadByMonth = IntStream.rangeClosed(1, 12)
                .mapToObj(month -> new MonthlyBooksReadDTO(month, monthlyCounts.getOrDefault(month, 0L)))
                .toList();

        long currentYearBooks = bookRatingRepository.countDistinctBooksByUserIdAndCreatedAtBetween(
                user.getId(), currentYearStart, nextYearStart);
        long previousYearBooks = bookRatingRepository.countDistinctBooksByUserIdAndCreatedAtBetween(
                user.getId(), previousYearStart, currentYearStart);

        BigDecimal averageRating = bookRatingRepository.calculateAverageRatingByUserId(user.getId())
                .setScale(2, RoundingMode.HALF_UP);

        return new ReadingStatisticsDTO(
                bookRatingRepository.countDistinctByUserId(user.getId()),
                averageRating,
                bookRatingRepository.findTopGenresByUserRatings(user.getId(), PageRequest.of(0, 5))
                        .stream()
                        .map(row -> new GenreCountDTO(((Number) row[0]).longValue(), (String) row[1], ((Number) row[2]).longValue()))
                        .toList(),
                booksReadByMonth,
                new YearComparisonDTO(currentYear, currentYearBooks, currentYear - 1, previousYearBooks),
                Instant.now());
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

    @Transactional(readOnly = true)
    public List<FriendActivityDTO> getFriendActivity(String usernameSlug, User currentUser) {
        User profileUser = userRepository.findByUsernameIgnoreCase(usernameSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        // Check if current user is friends with the profile user
        if (currentUser == null) {
            return new ArrayList<>();
        }
        if (!currentUser.getId().equals(profileUser.getId())) {
            boolean areFriends = friendshipRepository.findByUsers(profileUser.getId(), currentUser.getId()).isPresent();
            if (!areFriends) {
                return new ArrayList<>();
            }
        }

        List<FriendActivityDTO> activities = new ArrayList<>();
        PageRequest pageRequest = PageRequest.of(0, 10);

        // Get recent reviews
        List<BookReview> reviews = bookReviewRepository.findRecentByUserIdsAndStatus(
                List.of(profileUser.getId()), ReviewStatus.published, pageRequest);

        for (BookReview review : reviews) {
            activities.add(new FriendActivityDTO(
                    review.getId(),
                    review.getUser().getId(),
                    review.getUser().getFullName(),
                    "BOOK_REVIEWED",
                    review.getCreatedAt(),
                    review.getEditedAt() != null ? review.getEditedAt() : review.getCreatedAt(),
                    review.getBook().getId(),
                    review.getBook().getTitle(),
                    review.getBook().getIsbn().toString(),
                    review.getBook().getCoverUrl(),
                    review.getBook().getAuthors().stream().map(author -> author.getName()).toList(),
                    review.getRating() != null ? review.getRating().intValue() : null,
                    review.getReviewText(),
                    review.getStatus().name(),
                    0, // BookReview doesn't have helpfulCount field
                    null, // listId
                    null, // listName
                    null, // isPublic
                    null, // publicToken
                    null  // visibility
            ));
        }

        // Get recent book additions to lists
        List<UserListBook> listBooks = userListBookRepository.findRecentByUserIds(
                List.of(profileUser.getId()), pageRequest);

        // Filter out LISTED lists that are not shared with current user
        if (currentUser != null) {
            listBooks = listBooks.stream()
                    .filter(listBook -> {
                        // Allow PUBLIC lists and lists owned by current user
                        if (listBook.getUserList().getVisibility() == ListVisibility.PUBLIC || 
                            listBook.getUserList().getUser().getId().equals(currentUser.getId())) {
                            return true;
                        }
                        // For LISTED lists, only allow if shared with current user
                        if (listBook.getUserList().getVisibility() == ListVisibility.LISTED) {
                            return userListShareRepository.findByListIdAndReceiverIdAndIsActiveTrue(
                                    listBook.getUserList().getId(), currentUser.getId()).isPresent();
                        }
                        // PRIVATE lists are never shown
                        return false;
                    })
                    .toList();
        }

        for (UserListBook listBook : listBooks) {
            // Get token based on list visibility
            String publicToken = null;
            try {
                if (listBook.getUserList().getVisibility() == ListVisibility.PUBLIC) {
                    // Try to find public link for PUBLIC lists
                    UserListShareLink link = userListShareLinkRepository.findByListId(listBook.getUserList().getId()).orElse(null);
                    if (link != null && link.isActive()) {
                        publicToken = link.getPublicToken();
                    }
                } else if (listBook.getUserList().getVisibility() == ListVisibility.LISTED) {
                    // Try to find share token for LISTED lists shared with current user
                    if (currentUser != null) {
                        UserListShare share = userListShareRepository.findByListIdAndReceiverIdAndIsActiveTrue(
                                listBook.getUserList().getId(), currentUser.getId()).orElse(null);
                        if (share != null && share.getShareToken() != null) {
                            publicToken = share.getShareToken();
                        }
                    }
                }
            } catch (Exception e) {
                // If repository not available or error, keep publicToken as null
                publicToken = null;
            }
            
            System.out.println("DEBUG: ListBook - ListId: " + listBook.getUserList().getId() + 
                              ", ListName: " + listBook.getUserList().getName() + 
                              ", Visibility: " + listBook.getUserList().getVisibility().name() + 
                              ", PublicToken: " + publicToken);
            
            activities.add(new FriendActivityDTO(
                    listBook.getId(),
                    listBook.getUserList().getUser().getId(),
                    listBook.getUserList().getUser().getFullName(),
                    "BOOK_ADDED_TO_LIST",
                    listBook.getAddedAt(),
                    listBook.getAddedAt(),
                    listBook.getBook().getId(),
                    listBook.getBook().getTitle(),
                    listBook.getBook().getIsbn().toString(),
                    listBook.getBook().getCoverUrl(),
                    listBook.getBook().getAuthors().stream().map(author -> author.getName()).toList(),
                    null, // rating
                    null, // reviewText
                    null, // status
                    null, // helpfulCount
                    listBook.getUserList().getId(),
                    listBook.getUserList().getName(),
                    listBook.getUserList().getVisibility().name().equals("PUBLIC"),
                    publicToken,
                    listBook.getUserList().getVisibility().name()
            ));
        }

        // Sort by creation date (most recent first)
        activities.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));

        return activities;
    }
}
