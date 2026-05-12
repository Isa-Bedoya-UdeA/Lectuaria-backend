package com.lectuaria.backend.dto.shared;

import java.time.Instant;

public class SharedBookDTO {
    private Long notificationId;
    private Long bookId;
    private String isbn;
    private String title;
    private String coverUrl;
    private String ownerName;
    private String message;
    private Instant sharedAt;

    public SharedBookDTO() {}

    public SharedBookDTO(Long notificationId, Long bookId, String isbn, String title, String coverUrl, String ownerName, String message, Instant sharedAt) {
        this.notificationId = notificationId;
        this.bookId = bookId;
        this.isbn = isbn;
        this.title = title;
        this.coverUrl = coverUrl;
        this.ownerName = ownerName;
        this.message = message;
        this.sharedAt = sharedAt;
    }

    public Long getNotificationId() { return notificationId; }
    public Long getBookId() { return bookId; }
    public String getIsbn() { return isbn; }
    public String getTitle() { return title; }
    public String getCoverUrl() { return coverUrl; }
    public String getOwnerName() { return ownerName; }
    public String getMessage() { return message; }
    public Instant getSharedAt() { return sharedAt; }

    public void setNotificationId(Long notificationId) { this.notificationId = notificationId; }
    public void setBookId(Long bookId) { this.bookId = bookId; }
    public void setIsbn(String isbn) { this.isbn = isbn; }
    public void setTitle(String title) { this.title = title; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    public void setMessage(String message) { this.message = message; }
    public void setSharedAt(Instant sharedAt) { this.sharedAt = sharedAt; }
}
