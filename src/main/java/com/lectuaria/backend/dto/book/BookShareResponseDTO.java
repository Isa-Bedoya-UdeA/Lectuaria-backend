package com.lectuaria.backend.dto.book;

import java.time.Instant;

public class BookShareResponseDTO {
    private Long id;
    private Long bookId;
    private String bookTitle;
    private Long senderId;
    private String senderName;
    private Long receiverId;
    private String receiverName;
    private String message;
    private Instant sharedAt;

    public BookShareResponseDTO() {}

    public BookShareResponseDTO(Long id, Long bookId, String bookTitle, Long senderId, String senderName,
                                 Long receiverId, String receiverName, String message, Instant sharedAt) {
        this.id = id;
        this.bookId = bookId;
        this.bookTitle = bookTitle;
        this.senderId = senderId;
        this.senderName = senderName;
        this.receiverId = receiverId;
        this.receiverName = receiverName;
        this.message = message;
        this.sharedAt = sharedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getBookId() { return bookId; }
    public void setBookId(Long bookId) { this.bookId = bookId; }

    public String getBookTitle() { return bookTitle; }
    public void setBookTitle(String bookTitle) { this.bookTitle = bookTitle; }

    public Long getSenderId() { return senderId; }
    public void setSenderId(Long senderId) { this.senderId = senderId; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public Long getReceiverId() { return receiverId; }
    public void setReceiverId(Long receiverId) { this.receiverId = receiverId; }

    public String getReceiverName() { return receiverName; }
    public void setReceiverName(String receiverName) { this.receiverName = receiverName; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Instant getSharedAt() { return sharedAt; }
    public void setSharedAt(Instant sharedAt) { this.sharedAt = sharedAt; }
}
