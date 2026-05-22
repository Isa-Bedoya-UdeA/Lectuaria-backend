package com.lectuaria.backend.model.user;

import com.lectuaria.backend.model.auth.User;
import jakarta.persistence.*;
import java.time.Instant;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "user_privacy_settings")
public class UserPrivacySettings {

    @Id
    @Column(name = "id_user")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "id_user", nullable = false, unique = true)
    private User user;

    @Convert(attributeName = "profileVisibility", converter = VisibilityConverter.class)
    @Column(name = "profile_visibility", nullable = false)
    private Visibility profileVisibility = Visibility.FRIENDS;

    @Convert(attributeName = "reviewsVisibility", converter = VisibilityConverter.class)
    @Column(name = "reviews_visibility", nullable = false)
    private Visibility reviewsVisibility = Visibility.FRIENDS;

    @Convert(attributeName = "readingListsVisibility", converter = VisibilityConverter.class)
    @Column(name = "reading_lists_visibility", nullable = false)
    private Visibility readingListsVisibility = Visibility.FRIENDS;

    @Convert(attributeName = "readingListsActivityVisibility", converter = VisibilityConverter.class)
    @Column(name = "reading_lists_activity_visibility", nullable = false)
    private Visibility readingListsActivityVisibility = Visibility.FRIENDS;

    @Convert(attributeName = "friendsVisibility", converter = VisibilityConverter.class)
    @Column(name = "friends_visibility", nullable = false)
    private Visibility friendsVisibility = Visibility.FRIENDS;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public UserPrivacySettings() {}

    public UserPrivacySettings(User user) {
        this.user = user;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

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

    public Instant getUpdatedAt() { return updatedAt; }
}