package com.lectuaria.backend.dto.book;

import java.time.Instant;
import java.util.List;

public class FeaturedSectionsDTO {
    private List<BookSummaryDTO> mostReadThisMonth;
    private List<BookSummaryDTO> topRated;
    private List<BookSummaryDTO> trending;
    private Instant nextUpdateAt;

    public FeaturedSectionsDTO() {}

    public FeaturedSectionsDTO(List<BookSummaryDTO> mostReadThisMonth, List<BookSummaryDTO> topRated,
            List<BookSummaryDTO> trending, Instant nextUpdateAt) {
        this.mostReadThisMonth = mostReadThisMonth;
        this.topRated = topRated;
        this.trending = trending;
        this.nextUpdateAt = nextUpdateAt;
    }

    public List<BookSummaryDTO> getMostReadThisMonth() { return mostReadThisMonth; }
    public void setMostReadThisMonth(List<BookSummaryDTO> mostReadThisMonth) { this.mostReadThisMonth = mostReadThisMonth; }

    public List<BookSummaryDTO> getTopRated() { return topRated; }
    public void setTopRated(List<BookSummaryDTO> topRated) { this.topRated = topRated; }

    public List<BookSummaryDTO> getTrending() { return trending; }
    public void setTrending(List<BookSummaryDTO> trending) { this.trending = trending; }

    public Instant getNextUpdateAt() { return nextUpdateAt; }
    public void setNextUpdateAt(Instant nextUpdateAt) { this.nextUpdateAt = nextUpdateAt; }
}
