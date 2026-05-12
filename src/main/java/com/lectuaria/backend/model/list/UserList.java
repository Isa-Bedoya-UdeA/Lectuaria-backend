package com.lectuaria.backend.model.list;

import com.lectuaria.backend.model.auth.User;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "user_list")
public class UserList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_list")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_user", nullable = false)
    private User user;

    @Column(name = "name_list", nullable = false, length = 100)
    private String name;

    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "list_type", nullable = false)
    private ListType listType;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false)
    private ListVisibility visibility = ListVisibility.LISTED;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    public UserList() {}

    public UserList(User user, String name, String description, ListType listType, ListVisibility visibility) {
        this.user = user;
        this.name = name;
        this.description = description;
        this.listType = listType;
        this.visibility = visibility;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public ListType getListType() { return listType; }
    public ListVisibility getVisibility() { return visibility; }
    public Instant getCreatedAt() { return createdAt; }

    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setVisibility(ListVisibility visibility) { this.visibility = visibility; }
}
