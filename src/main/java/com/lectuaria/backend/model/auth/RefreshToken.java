package com.lectuaria.backend.model.auth;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "refresh_token")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_refresh_token")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "id_user", nullable = false)
    private User user;

    @Column(name = "sended_token", nullable = false)
    private String token;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked")
    private boolean revoked;

    public RefreshToken() {
    }

    public RefreshToken(User user, String token, Instant expiresAt) {
        this.user = user;
        this.token = token;
        this.expiresAt = expiresAt;
        this.revoked = false;
    }

    public String getToken() {
        return token;
    }

    public boolean isExpired() {
        return expiresAt.isBefore(Instant.now());
    }

    public void revoke() {
        this.revoked = true;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public User getUser() {
        return user;
    }
}