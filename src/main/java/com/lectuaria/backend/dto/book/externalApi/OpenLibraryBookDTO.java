package com.lectuaria.backend.dto.book.externalApi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenLibraryBookDTO {

    @JsonProperty("title")
    private String title;

    @JsonProperty("authors")
    private List<NameObject> authors;

    @JsonProperty("publishers")
    private List<NameObject> publishers;

    @JsonProperty("subjects")
    private List<NameObject> subjects;

    @JsonProperty("publish_date")
    private String publishDate;

    @JsonProperty("number_of_pages")
    private Integer numberOfPages;

    @JsonProperty("cover")
    private CoverObject cover;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NameObject {
        @JsonProperty("name")
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CoverObject {
        @JsonProperty("large")
        private String large;

        public String getLarge() {
            return large;
        }

        public void setLarge(String large) {
            this.large = large;
        }
    }

    // Getters y Setters
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<NameObject> getAuthors() {
        return authors;
    }

    public void setAuthors(List<NameObject> authors) {
        this.authors = authors;
    }

    public List<NameObject> getPublishers() {
        return publishers;
    }

    public void setPublishers(List<NameObject> publishers) {
        this.publishers = publishers;
    }

    public List<NameObject> getSubjects() {
        return subjects;
    }

    public void setSubjects(List<NameObject> subjects) {
        this.subjects = subjects;
    }

    public String getPublishDate() {
        return publishDate;
    }

    public void setPublishDate(String publishDate) {
        this.publishDate = publishDate;
    }

    public Integer getNumberOfPages() {
        return numberOfPages;
    }

    public void setNumberOfPages(Integer numberOfPages) {
        this.numberOfPages = numberOfPages;
    }

    public CoverObject getCover() {
        return cover;
    }

    public void setCover(CoverObject cover) {
        this.cover = cover;
    }
}