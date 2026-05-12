package com.lectuaria.backend.dto.common;

public class ApiError {
    private int status;
    private String message;

    public ApiError() {}

    public ApiError(int status, String message) {
        this.status = status;
        this.message = message;
    }

    public int getStatus() { return status; }
    public String getMessage() { return message; }

    public void setStatus(int status) { this.status = status; }
    public void setMessage(String message) { this.message = message; }
}
