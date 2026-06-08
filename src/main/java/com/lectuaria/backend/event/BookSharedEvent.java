package com.lectuaria.backend.event;

import com.lectuaria.backend.model.auth.User;
import com.lectuaria.backend.model.book.Book;
import org.springframework.context.ApplicationEvent;

/**
 * Evento publicado cuando un usuario comparte un libro con uno o varios
 * amigos. Otros modulos (notificaciones, metricas, busqueda de feed)
 * reaccionan a este evento sin acoplamiento al servicio de comparticion.
 *
 * Design Pattern: Observer (GoF) implementado con
 * {@link org.springframework.context.ApplicationEventPublisher} +
 * {@link org.springframework.context.event.EventListener} de Spring.
 */
public class BookSharedEvent extends ApplicationEvent {

    private final User sender;
    private final User receiver;
    private final Book book;
    private final String message;

    public BookSharedEvent(Object source, User sender, User receiver, Book book, String message) {
        super(source);
        this.sender = sender;
        this.receiver = receiver;
        this.book = book;
        this.message = message;
    }

    public User getSender() { return sender; }
    public User getReceiver() { return receiver; }
    public Book getBook() { return book; }
    public String getMessage() { return message; }
}
