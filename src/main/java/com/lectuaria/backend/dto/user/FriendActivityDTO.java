package com.lectuaria.backend.dto.user;

import java.time.Instant;
import java.util.List;

public class FriendActivityDTO {

    private final Long id;
    private final Long userId;
    private final String userName;
    private final String activityType;
    private final Instant createdAt;
    private final Instant updatedAt;
    
    // For BOOK_REVIEWED activity
    private final Long bookId;
    private final String bookTitle;
    private final String bookIsbn;
    private final String bookCoverUrl;
    private final List<String> bookAuthors;
    private final Integer rating;
    private final String reviewText;
    private final String status;
    private final Integer helpfulCount;
    
    // For BOOK_ADDED_TO_LIST activity
    private final Long listId;
    private final String listName;
    private final Boolean isPublic;
    private final String publicToken;
    private final String visibility;

    public FriendActivityDTO(Long id, Long userId, String userName, String activityType,
            Instant createdAt, Instant updatedAt, Long bookId, String bookTitle, 
            String bookIsbn, String bookCoverUrl, List<String> bookAuthors,
            Integer rating, String reviewText, String status, Integer helpfulCount,
            Long listId, String listName, Boolean isPublic, String publicToken, String visibility) {
        this.id = id;
        this.userId = userId;
        this.userName = userName;
        this.activityType = activityType;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.bookId = bookId;
        this.bookTitle = bookTitle;
        this.bookIsbn = bookIsbn;
        this.bookCoverUrl = bookCoverUrl;
        this.bookAuthors = bookAuthors;
        this.rating = rating;
        this.reviewText = reviewText;
        this.status = status;
        this.helpfulCount = helpfulCount;
        this.listId = listId;
        this.listName = listName;
        this.isPublic = isPublic;
        this.publicToken = publicToken;
        this.visibility = visibility;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public String getActivityType() {
        return activityType;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Long getBookId() {
        return bookId;
    }

    public String getBookTitle() {
        return bookTitle;
    }

    public String getBookIsbn() {
        return bookIsbn;
    }

    public String getBookCoverUrl() {
        return bookCoverUrl;
    }

    public List<String> getBookAuthors() {
        return bookAuthors;
    }

    public Integer getRating() {
        return rating;
    }

    public String getReviewText() {
        return reviewText;
    }

    public String getStatus() {
        return status;
    }

    public Integer getHelpfulCount() {
        return helpfulCount;
    }

    public Long getListId() {
        return listId;
    }

    public String getListName() {
        return listName;
    }

    public Boolean getIsPublic() {
        return isPublic;
    }

    public String getPublicToken() {
        return publicToken;
    }

    public String getVisibility() {
        return visibility;
    }
}
