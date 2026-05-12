package com.lectuaria.backend.dto.home;

import com.lectuaria.backend.dto.book.BookSummaryDTO;
import java.time.Instant;

public class FriendActivityDTO {
    private String id;
    private Long friendId;
    private String friendName;
    private String friendUsername;
    private String friendPhotoUrl;
    private String action;
    private String listName;
    private BookSummaryDTO book;
    private Instant occurredAt;

    public FriendActivityDTO() {}

    public FriendActivityDTO(String id, Long friendId, String friendName, String friendUsername, String friendPhotoUrl,
            String action, String listName, BookSummaryDTO book, Instant occurredAt) {
        this.id = id;
        this.friendId = friendId;
        this.friendName = friendName;
        this.friendUsername = friendUsername;
        this.friendPhotoUrl = friendPhotoUrl;
        this.action = action;
        this.listName = listName;
        this.book = book;
        this.occurredAt = occurredAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Long getFriendId() { return friendId; }
    public void setFriendId(Long friendId) { this.friendId = friendId; }
    public String getFriendName() { return friendName; }
    public void setFriendName(String friendName) { this.friendName = friendName; }
    public String getFriendUsername() { return friendUsername; }
    public void setFriendUsername(String friendUsername) { this.friendUsername = friendUsername; }
    public String getFriendPhotoUrl() { return friendPhotoUrl; }
    public void setFriendPhotoUrl(String friendPhotoUrl) { this.friendPhotoUrl = friendPhotoUrl; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getListName() { return listName; }
    public void setListName(String listName) { this.listName = listName; }
    public BookSummaryDTO getBook() { return book; }
    public void setBook(BookSummaryDTO book) { this.book = book; }
    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }
}
