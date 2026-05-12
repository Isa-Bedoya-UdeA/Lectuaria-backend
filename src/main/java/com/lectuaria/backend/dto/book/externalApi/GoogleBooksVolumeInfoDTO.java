package com.lectuaria.backend.dto.book.externalApi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GoogleBooksVolumeInfoDTO {

    @JsonProperty("title")
    private String title;

    @JsonProperty("authors")
    private List<String> authors;

    @JsonProperty("publisher")
    private String publisher;

    @JsonProperty("publishedDate")
    private String publishedDate;

    @JsonProperty("description")
    private String description;

    @JsonProperty("pageCount")
    private Integer pageCount;

    @JsonProperty("language")
    private String language;

    @JsonProperty("industryIdentifiers")
    private List<GoogleBooksIndustryIdentifierDTO> industryIdentifiers;

    @JsonProperty("imageLinks")
    private GoogleBooksImageLinksDTO imageLinks;

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

    public String getLanguage() {
        return language;
    }

    public List<GoogleBooksIndustryIdentifierDTO> getIndustryIdentifiers() {
        return industryIdentifiers;
    }

    public GoogleBooksImageLinksDTO getImageLinks() {
        return imageLinks;
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

    public void setLanguage(String language) {
        this.language = language;
    }

    public void setIndustryIdentifiers(List<GoogleBooksIndustryIdentifierDTO> industryIdentifiers) {
        this.industryIdentifiers = industryIdentifiers;
    }

    public void setImageLinks(GoogleBooksImageLinksDTO imageLinks) {
        this.imageLinks = imageLinks;
    }

}