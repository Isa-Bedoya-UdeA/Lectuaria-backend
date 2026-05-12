package com.lectuaria.backend.dto.book;

import java.util.ArrayList;
import java.util.List;

public class BulkUploadResultDTO {
    private int totalProcessed;
    private int successCount;
    private int errorCount;
    private List<String> errors = new ArrayList<>();
    private List<BookPublishResponseDTO> successes = new ArrayList<>();

    public BulkUploadResultDTO() {}

    public void addError(String error) {
        this.errors.add(error);
        this.errorCount++;
        this.totalProcessed++;
    }

    public void addSuccess(BookPublishResponseDTO response) {
        this.successes.add(response);
        this.successCount++;
        this.totalProcessed++;
    }

    // Getters and Setters
    public int getTotalProcessed() { return totalProcessed; }
    public void setTotalProcessed(int totalProcessed) { this.totalProcessed = totalProcessed; }

    public int getSuccessCount() { return successCount; }
    public void setSuccessCount(int successCount) { this.successCount = successCount; }

    public int getErrorCount() { return errorCount; }
    public void setErrorCount(int errorCount) { this.errorCount = errorCount; }

    public List<String> getErrors() { return errors; }
    public void setErrors(List<String> errors) { this.errors = errors; }

    public List<BookPublishResponseDTO> getSuccesses() { return successes; }
    public void setSuccesses(List<BookPublishResponseDTO> successes) { this.successes = successes; }
}
