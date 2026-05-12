package com.lectuaria.backend.dto.user;

import java.time.Instant;
import java.util.List;

public class UserProfileDTO {
    private Long id;
    private String username;
    private String fullName;
    private String photoUrl;
    private String biography;
    private Instant createdAt;
    private UserStatsDTO stats;
    private FriendshipStatus friendshipStatus;
    private List<BookSummaryDTO> recentReviews;

    public UserProfileDTO() {}

    public UserProfileDTO(Long id, String username, String fullName, String photoUrl, String biography, Instant createdAt) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.photoUrl = photoUrl;
        this.biography = biography;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public String getBiography() { return biography; }
    public void setBiography(String biography) { this.biography = biography; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public UserStatsDTO getStats() { return stats; }
    public void setStats(UserStatsDTO stats) { this.stats = stats; }

    public FriendshipStatus getFriendshipStatus() { return friendshipStatus; }
    public void setFriendshipStatus(FriendshipStatus friendshipStatus) { this.friendshipStatus = friendshipStatus; }

    public List<BookSummaryDTO> getRecentReviews() { return recentReviews; }
    public void setRecentReviews(List<BookSummaryDTO> recentReviews) { this.recentReviews = recentReviews; }

    public static class BookSummaryDTO {
        private Long bookId;
        private String title;
        private String coverUrl;
        private String content;
        private Instant createdAt;

        public BookSummaryDTO() {}

        public BookSummaryDTO(Long bookId, String title, String coverUrl, String content, Instant createdAt) {
            this.bookId = bookId;
            this.title = title;
            this.coverUrl = coverUrl;
            this.content = content;
            this.createdAt = createdAt;
        }

        public Long getBookId() { return bookId; }
        public void setBookId(Long bookId) { this.bookId = bookId; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getCoverUrl() { return coverUrl; }
        public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }

        public Instant getCreatedAt() { return createdAt; }
        public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    }
}
