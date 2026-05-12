package com.lectuaria.backend.dto.home;

import com.lectuaria.backend.dto.book.BookCatalogItemDTO;
import com.lectuaria.backend.dto.book.FeaturedSectionsDTO;
import java.util.List;

public class HomeResponseDTO {
    private List<FriendActivityDTO> friendActivity;
    private List<BookCatalogItemDTO> newCatalogBooks;
    private FeaturedSectionsDTO featuredSections;

    public HomeResponseDTO() {}

    public HomeResponseDTO(List<FriendActivityDTO> friendActivity, List<BookCatalogItemDTO> newCatalogBooks,
            FeaturedSectionsDTO featuredSections) {
        this.friendActivity = friendActivity;
        this.newCatalogBooks = newCatalogBooks;
        this.featuredSections = featuredSections;
    }

    public List<FriendActivityDTO> getFriendActivity() { return friendActivity; }
    public void setFriendActivity(List<FriendActivityDTO> friendActivity) { this.friendActivity = friendActivity; }
    public List<BookCatalogItemDTO> getNewCatalogBooks() { return newCatalogBooks; }
    public void setNewCatalogBooks(List<BookCatalogItemDTO> newCatalogBooks) { this.newCatalogBooks = newCatalogBooks; }
    public FeaturedSectionsDTO getFeaturedSections() { return featuredSections; }
    public void setFeaturedSections(FeaturedSectionsDTO featuredSections) { this.featuredSections = featuredSections; }
}
