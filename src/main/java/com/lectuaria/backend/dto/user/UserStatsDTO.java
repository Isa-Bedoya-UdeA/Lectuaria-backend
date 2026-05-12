package com.lectuaria.backend.dto.user;

public class UserStatsDTO {
    private Integer booksRead;
    private Integer reviewsCount;
    private Integer friendsCount;
    private Integer favoritesCount;

    public UserStatsDTO() {}

    public UserStatsDTO(Integer booksRead, Integer reviewsCount, Integer friendsCount, Integer favoritesCount) {
        this.booksRead = booksRead;
        this.reviewsCount = reviewsCount;
        this.friendsCount = friendsCount;
        this.favoritesCount = favoritesCount;
    }

    public Integer getBooksRead() { return booksRead; }
    public void setBooksRead(Integer booksRead) { this.booksRead = booksRead; }

    public Integer getReviewsCount() { return reviewsCount; }
    public void setReviewsCount(Integer reviewsCount) { this.reviewsCount = reviewsCount; }

    public Integer getFriendsCount() { return friendsCount; }
    public void setFriendsCount(Integer friendsCount) { this.friendsCount = friendsCount; }

    public Integer getFavoritesCount() { return favoritesCount; }
    public void setFavoritesCount(Integer favoritesCount) { this.favoritesCount = favoritesCount; }
}
