package com.lectuaria.backend.model.notification;

import com.lectuaria.backend.model.auth.User;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "notification")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_notification")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_user", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 50)
    private NotificationType notificationType;

    @Column(name = "notification_message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    public Notification() {}

    public Notification(User user, NotificationType notificationType, String message, Long referenceId) {
        this.user = user;
        this.notificationType = notificationType;
        this.message = message;
        this.referenceId = referenceId;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public NotificationType getNotificationType() { return notificationType; }
    public String getMessage() { return message; }
    public Long getReferenceId() { return referenceId; }
    public boolean isRead() { return isRead; }
    public Instant getCreatedAt() { return createdAt; }

    public void setUser(User user) { this.user = user; }
    public void setNotificationType(NotificationType notificationType) { this.notificationType = notificationType; }
    public void setMessage(String message) { this.message = message; }
    public void setReferenceId(Long referenceId) { this.referenceId = referenceId; }
    public void setRead(boolean read) { isRead = read; }
}
