package com.lectuaria.backend.event;

import com.lectuaria.backend.model.notification.NotificationType;
import com.lectuaria.backend.service.notification.INotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Listener del evento {@link BookSharedEvent} (Observer Pattern, GoF).
 * Crea la notificacion interna que recibe el amigo al que se le compartio
 * el libro. Esta clase NO se acopla al servicio de comparticion: solo
 * reacciona al evento publicado por {@code BookShareServiceImpl}.
 */
@Component
public class BookSharedNotificationListener {

    private static final Logger logger = LoggerFactory.getLogger(BookSharedNotificationListener.class);

    private final INotificationService notificationService;

    public BookSharedNotificationListener(INotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @EventListener
    public void onBookShared(BookSharedEvent event) {
        String body = event.getMessage() != null && !event.getMessage().isEmpty()
                ? event.getSender().getFullName() + " te ha compartido este libro: "
                  + event.getBook().getTitle() + " - " + event.getMessage()
                : event.getSender().getFullName() + " te ha compartido este libro: "
                  + event.getBook().getTitle();
        try {
            notificationService.createNotification(
                    event.getReceiver().getId(),
                    NotificationType.SHARED,
                    body,
                    event.getBook().getIsbn()
            );
        } catch (Exception e) {
            logger.error("Fallo al crear notificacion por BookSharedEvent: {}", e.getMessage(), e);
        }
    }
}
