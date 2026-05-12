package com.lectuaria.backend.dto.book;

import java.util.List;

public class ShareResultDTO {
    private int successfulShares;
    private int failedShares;
    private List<String> errorMessages;
    private String message;

    public ShareResultDTO() {}

    public ShareResultDTO(int successfulShares, int failedShares, List<String> errorMessages, String message) {
        this.successfulShares = successfulShares;
        this.failedShares = failedShares;
        this.errorMessages = errorMessages;
        this.message = message;
    }

    public int getSuccessfulShares() { return successfulShares; }
    public void setSuccessfulShares(int successfulShares) { this.successfulShares = successfulShares; }
    public int getFailedShares() { return failedShares; }
    public void setFailedShares(int failedShares) { this.failedShares = failedShares; }
    public List<String> getErrorMessages() { return errorMessages; }
    public void setErrorMessages(List<String> errorMessages) { this.errorMessages = errorMessages; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
