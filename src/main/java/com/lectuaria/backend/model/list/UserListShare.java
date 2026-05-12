package com.lectuaria.backend.model.list;

import com.lectuaria.backend.model.auth.User;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "user_list_share")
public class UserListShare {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_share")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_list", nullable = false)
    private UserList list;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_owner", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_receiver", nullable = false)
    private User receiver;

    @CreationTimestamp
    @Column(name = "shared_at", updatable = false)
    private Instant sharedAt;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    public UserListShare() {}

    public UserListShare(UserList list, User owner, User receiver) {
        this.list = list;
        this.owner = owner;
        this.receiver = receiver;
    }

    public Long getId() { return id; }
    public UserList getList() { return list; }
    public User getOwner() { return owner; }
    public User getReceiver() { return receiver; }
    public Instant getSharedAt() { return sharedAt; }
    public boolean isActive() { return isActive; }

    public void setList(UserList list) { this.list = list; }
    public void setOwner(User owner) { this.owner = owner; }
    public void setReceiver(User receiver) { this.receiver = receiver; }
    public void setActive(boolean active) { isActive = active; }
}
