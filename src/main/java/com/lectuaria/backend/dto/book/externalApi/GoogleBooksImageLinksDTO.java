package com.lectuaria.backend.dto.book.externalApi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GoogleBooksImageLinksDTO {

    @JsonProperty("thumbnail")
    private String thumbnail;

    @JsonProperty("smallThumbnail")
    private String smallThumbnail;

    public String getThumbnail() {
        return thumbnail;
    }

    public String getSmallThumbnail() {
        return smallThumbnail;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public void setSmallThumbnail(String smallThumbnail) {
        this.smallThumbnail = smallThumbnail;
    }

}