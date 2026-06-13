package com.lectuaria.backend.model.book;

import com.lectuaria.backend.model.library.Library;
import com.lectuaria.backend.model.library.Librarian;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "book_edit_history")
public class BookEditHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_history")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_book", nullable = false)
    private Book book;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_library", nullable = false)
    private Library library;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_librarian", nullable = false)
    private Librarian librarian;

    @Column(name = "old_value_json", columnDefinition = "jsonb")
    private String oldValueJson;

    @Column(name = "new_value_json", columnDefinition = "jsonb")
    private String newValueJson;

    @CreationTimestamp
    @Column(name = "edit_date", updatable = false)
    private Instant editDate;

    // Constructor sin argumentos requerido por JPA (Jakarta Persistence)
    // para instanciar la entidad via reflection al cargar desde la BD.
    public BookEditHistory() {}

    public Long getId() { return id; }
    public Book getBook() { return book; }
    public void setBook(Book book) { this.book = book; }
    public Library getLibrary() { return library; }
    public void setLibrary(Library library) { this.library = library; }
    public Librarian getLibrarian() { return librarian; }
    public void setLibrarian(Librarian librarian) { this.librarian = librarian; }
    public String getOldValueJson() { return oldValueJson; }
    public void setOldValueJson(String oldValueJson) { this.oldValueJson = oldValueJson; }
    public String getNewValueJson() { return newValueJson; }
    public void setNewValueJson(String newValueJson) { this.newValueJson = newValueJson; }
    public Instant getEditDate() { return editDate; }
}