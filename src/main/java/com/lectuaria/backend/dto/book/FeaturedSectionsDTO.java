package com.lectuaria.backend.dto.book;

import java.time.Instant;
import java.util.List;

public class FeaturedSectionsDTO {
    private List<BookSummaryDTO> mostReadThisMonth;
    private List<BookSummaryDTO> topRated;

    private Instant nextUpdateAt;

    public FeaturedSectionsDTO() {
    }

    public FeaturedSectionsDTO(List<BookSummaryDTO> mostReadThisMonth, List<BookSummaryDTO> topRated,
            Instant nextUpdateAt) {
        this.mostReadThisMonth = mostReadThisMonth;
        this.topRated = topRated;
        this.nextUpdateAt = nextUpdateAt;
    }

    public List<BookSummaryDTO> getMostReadThisMonth() {
        return mostReadThisMonth;
    }

    public void setMostReadThisMonth(List<BookSummaryDTO> mostReadThisMonth) {
        this.mostReadThisMonth = mostReadThisMonth;
    }

    public List<BookSummaryDTO> getTopRated() {
        return topRated;
    }

    public void setTopRated(List<BookSummaryDTO> topRated) {
        this.topRated = topRated;
    }

    public Instant getNextUpdateAt() {
        return nextUpdateAt;
    }

    public void setNextUpdateAt(Instant nextUpdateAt) {
        this.nextUpdateAt = nextUpdateAt;
    }
}
