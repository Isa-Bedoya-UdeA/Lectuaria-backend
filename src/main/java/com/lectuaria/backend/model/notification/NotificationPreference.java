package com.lectuaria.backend.model.notification;

import com.lectuaria.backend.model.auth.User;
import jakarta.persistence.*;

@Entity
@Table(name = "notification_preference")
public class NotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_preference")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_user", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 50)
    private NotificationType notificationType;

    @Column(name = "is_enabled", nullable = false)
    private boolean isEnabled = true;

    public NotificationPreference() {}

    public NotificationPreference(User user, NotificationType notificationType, boolean isEnabled) {
        this.user = user;
        this.notificationType = notificationType;
        this.isEnabled = isEnabled;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public NotificationType getNotificationType() { return notificationType; }
    public boolean isEnabled() { return isEnabled; }

    public void setUser(User user) { this.user = user; }
    public void setNotificationType(NotificationType notificationType) { this.notificationType = notificationType; }
    public void setEnabled(boolean enabled) { isEnabled = enabled; }
}
