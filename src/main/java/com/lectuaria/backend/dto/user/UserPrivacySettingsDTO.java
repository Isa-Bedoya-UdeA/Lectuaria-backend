package com.lectuaria.backend.dto.user;

import com.lectuaria.backend.model.user.Visibility;

public class UserPrivacySettingsDTO {
    private Long id;
    private Visibility profileVisibility;
    private Visibility reviewsVisibility;
    private Visibility readingListsVisibility;
    private Visibility readingListsActivityVisibility;
    private Visibility friendsVisibility;

    public UserPrivacySettingsDTO() {}

    public UserPrivacySettingsDTO(Visibility profileVisibility, Visibility reviewsVisibility,
                                   Visibility readingListsVisibility, Visibility readingListsActivityVisibility,
                                   Visibility friendsVisibility) {
        this.profileVisibility = profileVisibility;
        this.reviewsVisibility = reviewsVisibility;
        this.readingListsVisibility = readingListsVisibility;
        this.readingListsActivityVisibility = readingListsActivityVisibility;
        this.friendsVisibility = friendsVisibility;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Visibility getProfileVisibility() { return profileVisibility; }
    public void setProfileVisibility(Visibility profileVisibility) { this.profileVisibility = profileVisibility; }

    public Visibility getReviewsVisibility() { return reviewsVisibility; }
    public void setReviewsVisibility(Visibility reviewsVisibility) { this.reviewsVisibility = reviewsVisibility; }

    public Visibility getReadingListsVisibility() { return readingListsVisibility; }
    public void setReadingListsVisibility(Visibility readingListsVisibility) { this.readingListsVisibility = readingListsVisibility; }

    public Visibility getReadingListsActivityVisibility() { return readingListsActivityVisibility; }
    public void setReadingListsActivityVisibility(Visibility readingListsActivityVisibility) { this.readingListsActivityVisibility = readingListsActivityVisibility; }

    public Visibility getFriendsVisibility() { return friendsVisibility; }
    public void setFriendsVisibility(Visibility friendsVisibility) { this.friendsVisibility = friendsVisibility; }
}