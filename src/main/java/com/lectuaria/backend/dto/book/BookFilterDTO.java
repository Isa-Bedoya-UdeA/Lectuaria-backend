package com.lectuaria.backend.dto.book;

import java.util.List;

public class BookFilterDTO {
    private List<String> keywords;
    private List<Long> genreIds;
    private List<Long> libraryIds;
    private List<String> formatTypes;
    private Integer minYear;
    private Integer maxYear;
    private Float minRating;

    public BookFilterDTO() {}

    public BookFilterDTO(List<String> keywords, List<Long> genreIds, List<Long> libraryIds,
                        List<String> formatTypes, Integer minYear, Integer maxYear, Float minRating) {
        this.keywords = keywords;
        this.genreIds = genreIds;
        this.libraryIds = libraryIds;
        this.formatTypes = formatTypes;
        this.minYear = minYear;
        this.maxYear = maxYear;
        this.minRating = minRating;
    }

    public List<String> getKeywords() { return keywords; }
    public void setKeywords(List<String> keywords) { this.keywords = keywords; }

    public List<Long> getGenreIds() { return genreIds; }
    public void setGenreIds(List<Long> genreIds) { this.genreIds = genreIds; }

    public List<Long> getLibraryIds() { return libraryIds; }
    public void setLibraryIds(List<Long> libraryIds) { this.libraryIds = libraryIds; }

    public List<String> getFormatTypes() { return formatTypes; }
    public void setFormatTypes(List<String> formatTypes) { this.formatTypes = formatTypes; }

    public Integer getMinYear() { return minYear; }
    public void setMinYear(Integer minYear) { this.minYear = minYear; }

    public Integer getMaxYear() { return maxYear; }
    public void setMaxYear(Integer maxYear) { this.maxYear = maxYear; }

    public Float getMinRating() { return minRating; }
    public void setMinRating(Float minRating) { this.minRating = minRating; }
}
