package com.lectuaria.backend.model.book;

import jakarta.persistence.*;

@Entity
@Table(name = "book_format")
public class BookFormat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_book_format")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_book", nullable = false)
    private Book book;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_format", nullable = false)
    private Format format;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_platform")
    private Platform platform;

    public BookFormat() {
    }

    public Long getId() {
        return id;
    }

    public Book getBook() {
        return book;
    }

    public void setBook(Book book) {
        this.book = book;
    }

    public Format getFormat() {
        return format;
    }

    public void setFormat(Format format) {
        this.format = format;
    }

    public Platform getPlatform() {
        return platform;
    }

    public void setPlatform(Platform platform) {
        this.platform = platform;
    }
}