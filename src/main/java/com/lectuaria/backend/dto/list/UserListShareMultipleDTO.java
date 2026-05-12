package com.lectuaria.backend.dto.list;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class UserListShareMultipleDTO {
    
    @NotEmpty(message = "La lista de amigos no puede estar vacía")
    private List<Long> friendIds;
    
    private String message;
    
    public UserListShareMultipleDTO() {}
    
    public UserListShareMultipleDTO(List<Long> friendIds, String message) {
        this.friendIds = friendIds;
        this.message = message;
    }
    
    public List<Long> getFriendIds() {
        return friendIds;
    }
    
    public void setFriendIds(List<Long> friendIds) {
        this.friendIds = friendIds;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
}
