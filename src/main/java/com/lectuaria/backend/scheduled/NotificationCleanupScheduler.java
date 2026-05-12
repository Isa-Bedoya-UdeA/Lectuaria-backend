package com.lectuaria.backend.scheduled;

import com.lectuaria.backend.service.notification.INotificationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NotificationCleanupScheduler {

    private final INotificationService notificationService;

    public NotificationCleanupScheduler(INotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupOldNotifications() {
        notificationService.deleteOldNotifications();
    }
}
