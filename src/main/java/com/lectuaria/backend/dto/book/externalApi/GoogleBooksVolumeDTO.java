package com.lectuaria.backend.dto.book.externalApi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GoogleBooksVolumeDTO {

    @JsonProperty("id")
    private String id;

    @JsonProperty("volumeInfo")
    private GoogleBooksVolumeInfoDTO volumeInfo;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public GoogleBooksVolumeInfoDTO getVolumeInfo() {
        return volumeInfo;
    }

    public void setVolumeInfo(GoogleBooksVolumeInfoDTO volumeInfo) {
        this.volumeInfo = volumeInfo;
    }

}