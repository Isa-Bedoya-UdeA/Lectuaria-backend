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

    // transient: ApplicationEvent implementa Serializable (lo requiere
    // el contrato de Spring para eventos). User y Book son entidades JPA
    // con muchas asociaciones (LAZY) y no son serializables; marcarlas
    // transient evita el warning de SonarCloud S1948 y previene
    // serializaciones accidentales. El listener los consume dentro del
    // mismo thread del publishEvent (síncrono en Spring 4.2+), asi
    // que no hay problema de referencia nula.
    private final transient User sender;
    private final transient User receiver;
    private final transient Book book;
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
