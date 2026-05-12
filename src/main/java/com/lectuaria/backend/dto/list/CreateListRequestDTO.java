package com.lectuaria.backend.dto.list;

import com.lectuaria.backend.model.list.ListVisibility;

public class CreateListRequestDTO {
    private String name;
    private String description;
    private ListVisibility visibility;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public ListVisibility getVisibility() { return visibility; }
    public void setVisibility(ListVisibility visibility) { this.visibility = visibility; }
}
