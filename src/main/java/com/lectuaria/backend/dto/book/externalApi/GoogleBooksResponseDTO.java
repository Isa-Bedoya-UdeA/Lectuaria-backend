package com.lectuaria.backend.dto.book.externalApi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GoogleBooksResponseDTO {

    @JsonProperty("totalItems")
    private Integer totalItems;

    @JsonProperty("items")
    private List<GoogleBooksVolumeDTO> items;

    public Integer getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(Integer totalItems) {
        this.totalItems = totalItems;
    }

    public List<GoogleBooksVolumeDTO> getItems() {
        return items;
    }

    public void setItems(List<GoogleBooksVolumeDTO> items) {
        this.items = items;
    }

}