package com.lectuaria.backend.model.list;

import com.lectuaria.backend.model.book.Book;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "user_list_book")
public class UserListBook {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_list_book")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_list", nullable = false)
    private UserList userList;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_book", nullable = false)
    private Book book;

    @CreationTimestamp
    @Column(name = "added_at", updatable = false)
    private Instant addedAt;

    public UserListBook() {}

    public UserListBook(UserList userList, Book book) {
        this.userList = userList;
        this.book = book;
    }

    public Long getId() { return id; }
    public UserList getUserList() { return userList; }
    public Book getBook() { return book; }
    public Instant getAddedAt() { return addedAt; }
}
