package com.lectuaria.backend.dto.list;

import com.lectuaria.backend.dto.book.BookSummaryDTO;
import com.lectuaria.backend.model.list.ListType;
import com.lectuaria.backend.model.list.ListVisibility;
import java.time.Instant;
import java.util.List;

public class UserListDTO {
    private Long id;
    private String name;
    private String description;
    private ListType listType;
    private ListVisibility visibility;
    private Long bookCount;
    private Instant createdAt;
    private List<BookSummaryDTO> books;
    private Long userId; // ID del usuario dueño de la lista

    public UserListDTO() {}

    public UserListDTO(Long id, String name, String description, ListType listType, ListVisibility visibility, Long bookCount, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.listType = listType;
        this.visibility = visibility;
        this.bookCount = bookCount;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public ListType getListType() { return listType; }
    public void setListType(ListType listType) { this.listType = listType; }
    public ListVisibility getVisibility() { return visibility; }
    public void setVisibility(ListVisibility visibility) { this.visibility = visibility; }
    public Long getBookCount() { return bookCount; }
    public void setBookCount(Long bookCount) { this.bookCount = bookCount; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public List<BookSummaryDTO> getBooks() { return books; }
    public void setBooks(List<BookSummaryDTO> books) { this.books = books; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}
