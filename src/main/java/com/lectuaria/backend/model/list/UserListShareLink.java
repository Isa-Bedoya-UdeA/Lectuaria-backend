package com.lectuaria.backend.model.list;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "user_list_share_link")
public class UserListShareLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_link")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_list", nullable = false)
    private UserList list;

    @Column(name = "public_token", nullable = false, unique = true, columnDefinition = "TEXT")
    private String publicToken;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    public UserListShareLink() {}

    public UserListShareLink(UserList list, String publicToken) {
        this.list = list;
        this.publicToken = publicToken;
    }

    public Long getId() { return id; }
    public UserList getList() { return list; }
    public String getPublicToken() { return publicToken; }
    public Instant getCreatedAt() { return createdAt; }
    public boolean isActive() { return isActive; }

    public void setList(UserList list) { this.list = list; }
    public void setPublicToken(String publicToken) { this.publicToken = publicToken; }
    public void setActive(boolean active) { isActive = active; }
}
