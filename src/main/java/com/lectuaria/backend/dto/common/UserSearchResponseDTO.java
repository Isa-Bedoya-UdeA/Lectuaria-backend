package com.lectuaria.backend.dto.common;

public class UserSearchResponseDTO {
    private Long id;
    private String fullName;
    private String username;
    private String photoUrl;
    private String city;
    private String friendshipStatus; // "none", "friends", "pending_sent", "pending_received", "self"
    private Long friendshipRequestId; // If pending, the ID of the request to accept/reject/cancel

    public UserSearchResponseDTO(Long id, String fullName, String username, String photoUrl, String city, String friendshipStatus, Long friendshipRequestId) {
        this.id = id;
        this.fullName = fullName;
        this.username = username;
        this.photoUrl = photoUrl;
        this.city = city;
        this.friendshipStatus = friendshipStatus;
        this.friendshipRequestId = friendshipRequestId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getFriendshipStatus() {
        return friendshipStatus;
    }

    public void setFriendshipStatus(String friendshipStatus) {
        this.friendshipStatus = friendshipStatus;
    }

    public Long getFriendshipRequestId() {
        return friendshipRequestId;
    }

    public void setFriendshipRequestId(Long friendshipRequestId) {
        this.friendshipRequestId = friendshipRequestId;
    }
}
