package com.lectuaria.backend.dto.list;

public class FavoriteToggleResponseDTO {
    private Long bookId;
    private boolean favorite;

    public FavoriteToggleResponseDTO(Long bookId, boolean favorite) {
        this.bookId = bookId;
        this.favorite = favorite;
    }

    public Long getBookId() {
        return bookId;
    }

    public boolean isFavorite() {
        return favorite;
    }
}
