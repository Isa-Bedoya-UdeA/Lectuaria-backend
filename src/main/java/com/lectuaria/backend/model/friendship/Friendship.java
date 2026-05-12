package com.lectuaria.backend.model.friendship;

import com.lectuaria.backend.model.auth.User;
import jakarta.persistence.*;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "friendship")
public class Friendship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_friendship")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_user1", nullable = false)
    private User user1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_user2", nullable = false)
    private User user2;

    @CreationTimestamp
    @Column(name = "friendship_date", updatable = false)
    private Instant friendshipDate;

    public Friendship() {
    }

    public Friendship(User user1, User user2) {
        // Enforce ordering: user1 is always the user with smaller ID
        if (user1.getId() < user2.getId()) {
            this.user1 = user1;
            this.user2 = user2;
        } else {
            this.user1 = user2;
            this.user2 = user1;
        }
    }

    public Long getId() {
        return id;
    }

    public User getUser1() {
        return user1;
    }

    public User getUser2() {
        return user2;
    }

    public Instant getFriendshipDate() {
        return friendshipDate;
    }
}
