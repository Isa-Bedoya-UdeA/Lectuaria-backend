package com.lectuaria.backend.service.user.impl;

import com.lectuaria.backend.dto.user.FriendActivityDTO;
import com.lectuaria.backend.dto.user.FriendshipStatus;
import com.lectuaria.backend.dto.user.UserProfileDTO;
import com.lectuaria.backend.dto.user.UserStatsDTO;
import com.lectuaria.backend.dto.statistics.GenreCountDTO;
import com.lectuaria.backend.dto.statistics.MonthlyBooksReadDTO;
import com.lectuaria.backend.dto.statistics.ReadingStatisticsDTO;
import com.lectuaria.backend.dto.statistics.SocialStatisticsDTO;
import com.lectuaria.backend.dto.statistics.YearComparisonDTO;
import com.lectuaria.backend.exception.ResourceNotFoundException;
import com.lectuaria.backend.model.auth.User;
import com.lectuaria.backend.model.book.BookReview;
import com.lectuaria.backend.model.book.ReviewStatus;
import com.lectuaria.backend.model.friendship.Friendship;
import com.lectuaria.backend.model.friendship.FriendshipRequest;
import com.lectuaria.backend.model.list.UserListBook;
import com.lectuaria.backend.model.list.ListVisibility;
import com.lectuaria.backend.model.user.UserPrivacySettings;
import com.lectuaria.backend.model.user.Visibility;
import com.lectuaria.backend.repository.auth.UserRepository;
import com.lectuaria.backend.repository.book.BookReviewRepository;
import com.lectuaria.backend.repository.book.BookRatingRepository;
import com.lectuaria.backend.repository.book.GenreRepository;
import com.lectuaria.backend.repository.friendship.FriendshipRepository;
import com.lectuaria.backend.repository.friendship.FriendshipRequestRepository;
import com.lectuaria.backend.repository.list.UserListBookRepository;
import com.lectuaria.backend.repository.list.UserListShareRepository;
import com.lectuaria.backend.repository.list.UserListRepository;
import com.lectuaria.backend.repository.book.BookShareRepository;
import com.lectuaria.backend.repository.user.UserPrivacySettingsRepository;
import com.lectuaria.backend.model.list.UserListShare;
import com.lectuaria.backend.service.user.IUserProfileService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.HashSet;
import java.util.Set;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.stream.IntStream;

@Service
public class UserProfileServiceImpl implements IUserProfileService {

    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;
    private final FriendshipRequestRepository friendshipRequestRepository;
    private final BookReviewRepository bookReviewRepository;
    private final BookRatingRepository bookRatingRepository;
    private final UserListBookRepository userListBookRepository;
    private final UserListShareRepository userListShareRepository;
    private final UserListRepository userListRepository;
    private final BookShareRepository bookShareRepository;
    private final GenreRepository genreRepository;
    private final UserPrivacySettingsRepository privacyRepository;

    public UserProfileServiceImpl(UserRepository userRepository,
                                   FriendshipRepository friendshipRepository,
                                   FriendshipRequestRepository friendshipRequestRepository,
                                   BookReviewRepository bookReviewRepository,
                                   BookRatingRepository bookRatingRepository,
                                   UserListBookRepository userListBookRepository,
                                   UserListShareRepository userListShareRepository,
                                   UserListRepository userListRepository,
                                   BookShareRepository bookShareRepository,
                                   GenreRepository genreRepository,
                                   UserPrivacySettingsRepository privacyRepository) {
        this.userRepository = userRepository;
        this.friendshipRepository = friendshipRepository;
        this.friendshipRequestRepository = friendshipRequestRepository;
        this.bookReviewRepository = bookReviewRepository;
        this.bookRatingRepository = bookRatingRepository;
        this.userListBookRepository = userListBookRepository;
        this.userListShareRepository = userListShareRepository;
        this.userListRepository = userListRepository;
        this.bookShareRepository = bookShareRepository;
        this.genreRepository = genreRepository;
        this.privacyRepository = privacyRepository;
    }

    @Transactional(readOnly = true)
    public UserProfileDTO getUserProfileByUsername(String usernameSlug, User currentUser) {
        User profileUser = userRepository.findByUsernameIgnoreCase(usernameSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        UserPrivacySettings privacy = getOrCreatePrivacySettings(profileUser.getId());
        boolean areFriends = currentUser != null && friendshipRepository
                .findByUsers(profileUser.getId(), currentUser.getId()).isPresent();
        boolean isSelf = currentUser != null && profileUser.getId().equals(currentUser.getId());

        // Gate: profile visibility
        boolean profileVisible = isSelf
                || privacy.getProfileVisibility() == Visibility.PUBLIC
                || (privacy.getProfileVisibility() == Visibility.FRIENDS && areFriends);

        if (!profileVisible) {
            throw new com.lectuaria.backend.exception.ForbiddenException("Este perfil es privado");
        }

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

        // Privacy gate info for UI decisions
        UserProfileDTO.PrivacyGateDTO privacyGate = new UserProfileDTO.PrivacyGateDTO();
        privacyGate.setProfileVisible(true);
        privacyGate.setProfileVisibility(privacy.getProfileVisibility().name());
        privacyGate.setReviewsVisibility(privacy.getReviewsVisibility().name());
        privacyGate.setReadingListsVisibility(privacy.getReadingListsVisibility().name());
        privacyGate.setReadingListsActivityVisibility(privacy.getReadingListsActivityVisibility().name());
        privacyGate.setFriendsVisibility(privacy.getFriendsVisibility().name());
        dto.setPrivacy(privacyGate);

        // Reviews — filtered by privacy
        if (isSelf || privacy.getReviewsVisibility() == Visibility.PUBLIC
                || (privacy.getReviewsVisibility() == Visibility.FRIENDS && areFriends)) {
            dto.setRecentReviews(getRecentReviews(profileUser.getId()));
        }

        // Reading lists — filtered by privacy
        if (isSelf || privacy.getReadingListsVisibility() == Visibility.PUBLIC
                || (privacy.getReadingListsVisibility() == Visibility.FRIENDS && areFriends)) {
            dto.setReadingLists(getReadingLists(profileUser.getId(), currentUser));
        }

        // Friends — filtered by privacy
        if (isSelf || privacy.getFriendsVisibility() == Visibility.PUBLIC
                || (privacy.getFriendsVisibility() == Visibility.FRIENDS && areFriends)) {
            dto.setFriends(getFriends(profileUser.getId()));
        }

        return dto;
    }

    @Transactional(readOnly = true)
    public UserStatsDTO getUserStats(String usernameSlug) {
        User user = userRepository.findByUsernameIgnoreCase(usernameSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        Integer friendsCount = friendshipRepository.findFriendsByUserId(user.getId()).size();
        Integer reviewsCount = (int) bookReviewRepository.countByUserIdAndStatus(user.getId(), ReviewStatus.PUBLISHED);
        Integer favoritesCount = (int) userListBookRepository.countDistinctByUserListUserId(user.getId());

        Set<Long> readBookIds = new HashSet<>();
        readBookIds.addAll(bookRatingRepository.findRatedBookIdsByUserId(user.getId()));
        readBookIds.addAll(userListBookRepository.findReadBookIdsInListsByUserId(user.getId()));
        Integer booksRead = readBookIds.size();

        return new UserStatsDTO(booksRead, reviewsCount, friendsCount, favoritesCount);
    }

    @Transactional(readOnly = true)
    public ReadingStatisticsDTO getReadingStatistics(String usernameSlug) {
        User user = userRepository.findByUsernameIgnoreCase(usernameSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        int currentYear = LocalDate.now(ZoneOffset.UTC).getYear();
        LocalDate currentYearStart = LocalDate.of(currentYear, 1, 1);
        LocalDate nextYearStart = LocalDate.of(currentYear + 1, 1, 1);
        LocalDate previousYearStart = LocalDate.of(currentYear - 1, 1, 1);

        List<Object[]> ratedBooksData = bookRatingRepository.findRatedBooksAndCreatedAtByUserId(user.getId());
        List<Object[]> listBooksData = userListBookRepository.findReadBooksAndAddedAtByUserId(user.getId());

        Map<Long, Instant> readBooksMap = new java.util.HashMap<>();

        for (Object[] row : ratedBooksData) {
            Long bookId = ((Number) row[0]).longValue();
            Instant createdAt = (Instant) row[1];
            readBooksMap.put(bookId, createdAt);
        }

        for (Object[] row : listBooksData) {
            Long bookId = ((Number) row[0]).longValue();
            Instant addedAt = (Instant) row[1];
            if (readBooksMap.containsKey(bookId)) {
                Instant earlierDate = readBooksMap.get(bookId);
                if (addedAt.isBefore(earlierDate)) {
                    readBooksMap.put(bookId, addedAt);
                }
            } else {
                readBooksMap.put(bookId, addedAt);
            }
        }

        Map<Integer, Long> monthlyCounts = new java.util.HashMap<>();
        for (Map.Entry<Long, Instant> entry : readBooksMap.entrySet()) {
            Instant readDate = entry.getValue();
            LocalDate date = LocalDate.ofInstant(readDate, ZoneOffset.UTC);

            if (!date.isBefore(currentYearStart) && date.isBefore(nextYearStart)) {
                int month = date.getMonthValue();
                monthlyCounts.put(month, monthlyCounts.getOrDefault(month, 0L) + 1);
            }
        }

        List<MonthlyBooksReadDTO> booksReadByMonth = IntStream.rangeClosed(1, 12)
                .mapToObj(month -> new MonthlyBooksReadDTO(month, monthlyCounts.getOrDefault(month, 0L)))
                .toList();

        long currentYearBooks = readBooksMap.values().stream()
                .filter(instant -> {
                    LocalDate date = LocalDate.ofInstant(instant, ZoneOffset.UTC);
                    return !date.isBefore(currentYearStart) && date.isBefore(nextYearStart);
                })
                .count();

        long previousYearBooks = readBooksMap.values().stream()
                .filter(instant -> {
                    LocalDate date = instant.atZone(ZoneOffset.UTC).toLocalDate();
                    return !date.isBefore(previousYearStart) && date.isBefore(currentYearStart);
                })
                .count();

        Integer reviewsCount = (int) bookReviewRepository.countByUserIdAndStatus(user.getId(), ReviewStatus.PUBLISHED);

        List<GenreCountDTO> topGenres = new ArrayList<>();
        if (!readBooksMap.isEmpty()) {
            topGenres = genreRepository.findTopGenresByBookIds(new ArrayList<>(readBooksMap.keySet()), PageRequest.of(0, 5))
                    .stream()
                    .map(row -> new GenreCountDTO(((Number) row[0]).longValue(), (String) row[1], ((Number) row[2]).longValue()))
                    .toList();
        }

        return new ReadingStatisticsDTO(
                (long) readBooksMap.size(),
                reviewsCount,
                topGenres,
                booksReadByMonth,
                new YearComparisonDTO(currentYear, currentYearBooks, currentYear - 1, previousYearBooks),
                Instant.now());
    }

    @Transactional(readOnly = true)
    public SocialStatisticsDTO getSocialStatistics(String usernameSlug) {
        User user = userRepository.findByUsernameIgnoreCase(usernameSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        Long friendsCount = (long) friendshipRepository.findFriendsByUserId(user.getId()).size();
        Long listsSharedByFriends = (long) userListShareRepository.findByReceiverIdAndIsActiveTrue(user.getId()).size();
        Long listsIShared = (long) userListShareRepository.findByOwnerIdAndIsActiveTrue(user.getId()).size();
        Long booksSharedWithFriends = (long) bookShareRepository.findBySenderId(user.getId()).size();
        Long booksSharedByFriends = (long) bookShareRepository.findByReceiverId(user.getId()).size();

        return new SocialStatisticsDTO(
                friendsCount,
                listsSharedByFriends,
                listsIShared,
                booksSharedWithFriends,
                booksSharedByFriends,
                Instant.now()
        );
    }

    @Transactional(readOnly = true)
    public List<FriendActivityDTO> getFriendActivity(String usernameSlug, User currentUser) {
        User profileUser = userRepository.findByUsernameIgnoreCase(usernameSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        UserPrivacySettings privacy = getOrCreatePrivacySettings(profileUser.getId());
        boolean isSelf = currentUser != null && currentUser.getId().equals(profileUser.getId());
        // Visitante anonimo: no es amigo de nadie, no es self. Solo ve lo publico.
        boolean friendOfFriend = currentUser != null && isFriend(currentUser.getId(), profileUser.getId());

        List<FriendActivityDTO> activities = new ArrayList<>();
        PageRequest pageRequest = PageRequest.of(0, 10);

        boolean showReviews = isSelf
                || privacy.getReviewsVisibility() == Visibility.PUBLIC
                || (privacy.getReviewsVisibility() == Visibility.FRIENDS && friendOfFriend);
        boolean showActivity = isSelf
                || privacy.getReadingListsActivityVisibility() == Visibility.PUBLIC
                || (privacy.getReadingListsActivityVisibility() == Visibility.FRIENDS && friendOfFriend);

        if (showReviews) {
            List<BookReview> reviews = bookReviewRepository.findRecentByUserIdsAndStatus(
                    List.of(profileUser.getId()), ReviewStatus.PUBLISHED, pageRequest);
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
                        0, null, null, null, null, null
                ));
            }
        }

        if (showActivity) {
            List<UserListBook> listBooks = userListBookRepository.findRecentByUserIds(List.of(profileUser.getId()), pageRequest);
            final Long currentUserId = currentUser != null ? currentUser.getId() : null;
            listBooks = listBooks.stream()
                    .filter(listBook -> {
                        if (listBook.getUserList().getVisibility() == ListVisibility.PUBLIC) return true;
                        if (currentUserId != null && listBook.getUserList().getUser().getId().equals(currentUserId)) return true;
                        if (listBook.getUserList().getVisibility() == ListVisibility.LISTED && currentUserId != null) {
                            return userListShareRepository.findByListIdAndReceiverIdAndIsActiveTrue(
                                    listBook.getUserList().getId(), currentUserId).isPresent();
                        }
                        return false;
                    })
                    .toList();

            for (UserListBook listBook : listBooks) {
                String publicToken = null;
                if (listBook.getUserList().getVisibility() == ListVisibility.LISTED && currentUserId != null) {
                    UserListShare share = userListShareRepository.findByListIdAndReceiverIdAndIsActiveTrue(
                            listBook.getUserList().getId(), currentUserId).orElse(null);
                    if (share != null && share.getShareToken() != null) publicToken = share.getShareToken();
                }

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
                        null, null, null, null,
                        listBook.getUserList().getId(),
                        listBook.getUserList().getName(),
                        listBook.getUserList().getVisibility() == ListVisibility.PUBLIC,
                        publicToken,
                        listBook.getUserList().getVisibility().name()
                ));
            }
        }

        activities.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        return activities;
    }

    // --- private helpers ---

    private UserPrivacySettings getOrCreatePrivacySettings(Long userId) {
        return privacyRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
                    UserPrivacySettings settings = new UserPrivacySettings(user);
                    return privacyRepository.save(settings);
                });
    }

    private FriendshipStatus determineFriendshipStatus(User profileUser, User currentUser) {
        if (currentUser == null) return FriendshipStatus.NONE;
        if (profileUser.getId().equals(currentUser.getId())) return FriendshipStatus.SELF;
        if (friendshipRepository.findByUsers(profileUser.getId(), currentUser.getId()).isPresent()) return FriendshipStatus.ACCEPTED;
        if (friendshipRequestRepository.findPendingRequestBetween(currentUser.getId(), profileUser.getId()).isPresent()) return FriendshipStatus.PENDING;
        return FriendshipStatus.NONE;
    }

    private boolean isFriend(Long userId1, Long userId2) {
        return friendshipRepository.findByUsers(userId1, userId2).isPresent();
    }

    private List<UserProfileDTO.BookSummaryDTO> getRecentReviews(Long userId) {
        List<BookReview> reviews = bookReviewRepository.findRecentByUserIdsAndStatus(
                List.of(userId), ReviewStatus.PUBLISHED, PageRequest.of(0, 5));
        return reviews.stream()
                .map(r -> new UserProfileDTO.BookSummaryDTO(
                        r.getBook().getId(),
                        r.getBook().getTitle(),
                        r.getBook().getCoverUrl(),
                        r.getReviewText(),
                        r.getCreatedAt()))
                .toList();
    }

    private List<UserProfileDTO.ReadingListSummaryDTO> getReadingLists(Long profileUserId, User currentUser) {
        boolean isSelf = currentUser != null && currentUser.getId().equals(profileUserId);
        List<com.lectuaria.backend.model.list.UserList> lists = userListRepository.findByUserIdOrderByCreatedAtAsc(profileUserId);

        return lists.stream()
                .filter(list -> {
                    if (isSelf) return true;
                    if (list.getVisibility() == ListVisibility.PUBLIC) return true;
                    if (list.getVisibility() == ListVisibility.LISTED && currentUser != null) {
                        return userListShareRepository.findByListIdAndReceiverIdAndIsActiveTrue(list.getId(), currentUser.getId()).isPresent();
                    }
                    return false;
                })
                .map(list -> new UserProfileDTO.ReadingListSummaryDTO(
                        list.getId(),
                        list.getName(),
                        list.getDescription(),
                        list.getVisibility().name(),
                        (int) userListBookRepository.countByUserListId(list.getId())))
                .collect(java.util.stream.Collectors.toList());
    }

    private List<UserProfileDTO.FriendSummaryDTO> getFriends(Long userId) {
        List<Friendship> friendships = friendshipRepository.findFriendsByUserId(userId);
        return friendships.stream()
                .map(f -> {
                    User friend = f.getUser1().getId().equals(userId) ? f.getUser2() : f.getUser1();
                    return new UserProfileDTO.FriendSummaryDTO(friend.getId(), friend.getUsername(), friend.getFullName(), friend.getPhotoUrl());
                })
                .toList();
    }
}