package com.lectuaria.backend.dto.list;

import java.time.Instant;

public class UserListShareLinkDTO {
    private Long id;
    private Long listId;
    private String listName;
    private String publicToken;
    private Instant createdAt;
    private boolean isActive;

    public UserListShareLinkDTO() {}

    public UserListShareLinkDTO(Long id, Long listId, String listName, String publicToken, Instant createdAt, boolean isActive) {
        this.id = id;
        this.listId = listId;
        this.listName = listName;
        this.publicToken = publicToken;
        this.createdAt = createdAt;
        this.isActive = isActive;
    }

    public Long getId() { return id; }
    public Long getListId() { return listId; }
    public String getListName() { return listName; }
    public String getPublicToken() { return publicToken; }
    public Instant getCreatedAt() { return createdAt; }
    public boolean isActive() { return isActive; }

    public void setId(Long id) { this.id = id; }
    public void setListId(Long listId) { this.listId = listId; }
    public void setListName(String listName) { this.listName = listName; }
    public void setPublicToken(String publicToken) { this.publicToken = publicToken; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public void setActive(boolean active) { isActive = active; }
}
