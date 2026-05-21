package com.lectuaria.backend.dto.statistics;

import java.time.Instant;

public class SocialStatisticsDTO {
    private Long friendsCount;
    private Long listsSharedByFriends;
    private Long listsIShared;
    private Long booksSharedWithFriends;
    private Long booksSharedByFriends;
    private Instant updatedAt;

    public SocialStatisticsDTO() {}

    public SocialStatisticsDTO(Long friendsCount, Long listsSharedByFriends, Long listsIShared,
            Long booksSharedWithFriends, Long booksSharedByFriends, Instant updatedAt) {
        this.friendsCount = friendsCount;
        this.listsSharedByFriends = listsSharedByFriends;
        this.listsIShared = listsIShared;
        this.booksSharedWithFriends = booksSharedWithFriends;
        this.booksSharedByFriends = booksSharedByFriends;
        this.updatedAt = updatedAt;
    }

    public Long getFriendsCount() { return friendsCount; }
    public void setFriendsCount(Long friendsCount) { this.friendsCount = friendsCount; }
    public Long getListsSharedByFriends() { return listsSharedByFriends; }
    public void setListsSharedByFriends(Long listsSharedByFriends) { this.listsSharedByFriends = listsSharedByFriends; }
    public Long getListsIShared() { return listsIShared; }
    public void setListsIShared(Long listsIShared) { this.listsIShared = listsIShared; }
    public Long getBooksSharedWithFriends() { return booksSharedWithFriends; }
    public void setBooksSharedWithFriends(Long booksSharedWithFriends) { this.booksSharedWithFriends = booksSharedWithFriends; }
    public Long getBooksSharedByFriends() { return booksSharedByFriends; }
    public void setBooksSharedByFriends(Long booksSharedByFriends) { this.booksSharedByFriends = booksSharedByFriends; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}