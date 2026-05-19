package com.lectuaria.backend.dto.statistics;

public class GenreCountDTO {
    private Long genreId;
    private String genreName;
    private Long count;

    public GenreCountDTO() {}

    public GenreCountDTO(Long genreId, String genreName, Long count) {
        this.genreId = genreId;
        this.genreName = genreName;
        this.count = count;
    }

    public Long getGenreId() { return genreId; }
    public void setGenreId(Long genreId) { this.genreId = genreId; }
    public String getGenreName() { return genreName; }
    public void setGenreName(String genreName) { this.genreName = genreName; }
    public Long getCount() { return count; }
    public void setCount(Long count) { this.count = count; }
}
