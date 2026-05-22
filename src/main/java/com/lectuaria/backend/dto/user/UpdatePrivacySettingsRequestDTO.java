package com.lectuaria.backend.dto.user;

import com.lectuaria.backend.model.user.Visibility;
import jakarta.validation.constraints.NotNull;

public class UpdatePrivacySettingsRequestDTO {
    @NotNull
    private Visibility profileVisibility;
    @NotNull
    private Visibility reviewsVisibility;
    @NotNull
    private Visibility readingListsVisibility;
    @NotNull
    private Visibility readingListsActivityVisibility;
    @NotNull
    private Visibility friendsVisibility;

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