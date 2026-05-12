package com.lectuaria.backend.dto.book.externalApi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GoogleBooksIndustryIdentifierDTO {

    @JsonProperty("type")
    private String type;

    @JsonProperty("identifier")
    private String identifier;

    public String getType() {
        return type;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

}