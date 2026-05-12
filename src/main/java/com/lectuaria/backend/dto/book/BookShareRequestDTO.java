package com.lectuaria.backend.dto.book;

import java.util.List;

public class BookShareRequestDTO {
    private List<Long> friendIds;
    private String message;

    public BookShareRequestDTO() {}

    public BookShareRequestDTO(List<Long> friendIds, String message) {
        this.friendIds = friendIds;
        this.message = message;
    }

    public List<Long> getFriendIds() { return friendIds; }
    public void setFriendIds(List<Long> friendIds) { this.friendIds = friendIds; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
