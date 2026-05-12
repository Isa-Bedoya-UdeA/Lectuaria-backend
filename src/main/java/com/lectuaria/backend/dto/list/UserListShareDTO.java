package com.lectuaria.backend.dto.list;

import com.lectuaria.backend.dto.book.BookSummaryDTO;
import java.time.Instant;
import java.util.List;

public class UserListShareDTO {
    private Long id;
    private Long listId;
    private String listName;
    private String listDescription;
    private Long ownerId;
    private String ownerName;
    private Long receiverId;
    private String receiverName;
    private Instant sharedAt;
    private boolean isActive;
    private List<BookSummaryDTO> books;
    private String publicToken;

    public UserListShareDTO() {}

    public UserListShareDTO(Long id, Long listId, String listName, String listDescription, Long ownerId, String ownerName, Long receiverId, String receiverName, Instant sharedAt, boolean isActive, List<BookSummaryDTO> books, String publicToken) {
        this.id = id;
        this.listId = listId;
        this.listName = listName;
        this.listDescription = listDescription;
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.receiverId = receiverId;
        this.receiverName = receiverName;
        this.sharedAt = sharedAt;
        this.isActive = isActive;
        this.books = books;
        this.publicToken = publicToken;
    }

    public Long getId() { return id; }
    public Long getListId() { return listId; }
    public String getListName() { return listName; }
    public String getListDescription() { return listDescription; }
    public Long getOwnerId() { return ownerId; }
    public String getOwnerName() { return ownerName; }
    public Long getReceiverId() { return receiverId; }
    public String getReceiverName() { return receiverName; }
    public Instant getSharedAt() { return sharedAt; }
    public boolean isActive() { return isActive; }
    public List<BookSummaryDTO> getBooks() { return books; }
    public String getPublicToken() { return publicToken; }

    public void setId(Long id) { this.id = id; }
    public void setListId(Long listId) { this.listId = listId; }
    public void setListName(String listName) { this.listName = listName; }
    public void setListDescription(String listDescription) { this.listDescription = listDescription; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    public void setReceiverId(Long receiverId) { this.receiverId = receiverId; }
    public void setReceiverName(String receiverName) { this.receiverName = receiverName; }
    public void setSharedAt(Instant sharedAt) { this.sharedAt = sharedAt; }
    public void setActive(boolean active) { isActive = active; }
    public void setBooks(List<BookSummaryDTO> books) { this.books = books; }
    public void setPublicToken(String publicToken) { this.publicToken = publicToken; }
}
