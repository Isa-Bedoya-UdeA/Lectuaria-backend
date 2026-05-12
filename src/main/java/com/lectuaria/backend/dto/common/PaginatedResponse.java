package com.lectuaria.backend.dto.common;

import java.util.List;

public class PaginatedResponse<T> {
    private final List<T> content;
    private final int pageNumber;
    private final int pageSize;
    private final long totalElements;
    private final int totalPages;
    private final boolean isFirst;
    private final boolean isLast;
    private final boolean hasNext;
    private final boolean hasPrevious;

    public PaginatedResponse(List<T> content, int pageNumber, int pageSize,
            long totalElements, int totalPages,
            boolean isFirst, boolean isLast,
            boolean hasNext, boolean hasPrevious) {
        this.content = content;
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.isFirst = isFirst;
        this.isLast = isLast;
        this.hasNext = hasNext;
        this.hasPrevious = hasPrevious;
    }

    // Getters
    public List<T> getContent() {
        return content;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public int getPageSize() {
        return pageSize;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public boolean isFirst() {
        return isFirst;
    }

    public boolean isLast() {
        return isLast;
    }

    public boolean isHasNext() {
        return hasNext;
    }

    public boolean isHasPrevious() {
        return hasPrevious;
    }
}