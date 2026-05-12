package com.lectuaria.backend.dto.library;

public class LibrarySummaryDTO {
    private Long id;
    private String name;
    private String description;
    private String address;
    private String email;
    private String phone;
    private String openingHours;
    private String zoneName;

    public LibrarySummaryDTO() {}

    public LibrarySummaryDTO(Long id, String name, String description, String address, String email, String phone, String openingHours, String zoneName) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.address = address;
        this.email = email;
        this.phone = phone;
        this.openingHours = openingHours;
        this.zoneName = zoneName;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getOpeningHours() { return openingHours; }
    public void setOpeningHours(String openingHours) { this.openingHours = openingHours; }

    public String getZoneName() { return zoneName; }
    public void setZoneName(String zoneName) { this.zoneName = zoneName; }
}
