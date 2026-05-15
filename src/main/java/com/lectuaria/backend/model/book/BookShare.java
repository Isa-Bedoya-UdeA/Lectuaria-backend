package com.lectuaria.backend.model.book;

import com.lectuaria.backend.model.auth.User;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "book_share")
public class BookShare {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_share")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @CreationTimestamp
    @Column(name = "shared_at", updatable = false)
    private Instant sharedAt;

    public BookShare() {}

    public BookShare(User sender, User receiver, Book book, String message) {
        this.sender = sender;
        this.receiver = receiver;
        this.book = book;
        this.message = message;
    }

    public Long getId() { return id; }
    public User getSender() { return sender; }
    public User getReceiver() { return receiver; }
    public Book getBook() { return book; }
    public String getMessage() { return message; }
    public Instant getSharedAt() { return sharedAt; }

    public void setSender(User sender) { this.sender = sender; }
    public void setReceiver(User receiver) { this.receiver = receiver; }
    public void setBook(Book book) { this.book = book; }
    public void setMessage(String message) { this.message = message; }
}
