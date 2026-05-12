package com.lectuaria.backend.dto.book.externalApi;

import java.util.List;

public class ExternalBookMetadataDTO {

    private String title;
    private List<String> authors;
    private String publisher;
    private String publishedDate;
    private String description;
    private Integer pageCount;
    private String coverUrl;
    private String isbn13;
    private String isbn10;

    public String getTitle() {
        return title;
    }

    public List<String> getAuthors() {
        return authors;
    }

    public String getPublisher() {
        return publisher;
    }

    public String getPublishedDate() {
        return publishedDate;
    }

    public String getDescription() {
        return description;
    }

    public Integer getPageCount() {
        return pageCount;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public String getIsbn13() {
        return isbn13;
    }

    public String getIsbn10() {
        return isbn10;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setAuthors(List<String> authors) {
        this.authors = authors;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public void setPublishedDate(String publishedDate) {
        this.publishedDate = publishedDate;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setPageCount(Integer pageCount) {
        this.pageCount = pageCount;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public void setIsbn13(String isbn13) {
        this.isbn13 = isbn13;
    }

    public void setIsbn10(String isbn10) {
        this.isbn10 = isbn10;
    }
}