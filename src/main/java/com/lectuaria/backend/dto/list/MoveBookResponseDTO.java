package com.lectuaria.backend.dto.list;

import java.time.Instant;

public class MoveBookResponseDTO {
    private Long bookId;
    private Long sourceListId;
    private Long targetListId;
    private long sourceListBookCount;
    private long targetListBookCount;
    private String message;
    private Instant movedAt;

    public MoveBookResponseDTO() {}

    public MoveBookResponseDTO(Long bookId, Long sourceListId, Long targetListId, long sourceListBookCount,
            long targetListBookCount, String message, Instant movedAt) {
        this.bookId = bookId;
        this.sourceListId = sourceListId;
        this.targetListId = targetListId;
        this.sourceListBookCount = sourceListBookCount;
        this.targetListBookCount = targetListBookCount;
        this.message = message;
        this.movedAt = movedAt;
    }

    public Long getBookId() { return bookId; }
    public void setBookId(Long bookId) { this.bookId = bookId; }
    public Long getSourceListId() { return sourceListId; }
    public void setSourceListId(Long sourceListId) { this.sourceListId = sourceListId; }
    public Long getTargetListId() { return targetListId; }
    public void setTargetListId(Long targetListId) { this.targetListId = targetListId; }
    public long getSourceListBookCount() { return sourceListBookCount; }
    public void setSourceListBookCount(long sourceListBookCount) { this.sourceListBookCount = sourceListBookCount; }
    public long getTargetListBookCount() { return targetListBookCount; }
    public void setTargetListBookCount(long targetListBookCount) { this.targetListBookCount = targetListBookCount; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Instant getMovedAt() { return movedAt; }
    public void setMovedAt(Instant movedAt) { this.movedAt = movedAt; }
}
