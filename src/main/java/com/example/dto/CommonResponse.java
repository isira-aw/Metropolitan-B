package com.example.dto;

public class CommonResponse<T> {

    private String status;
    private String message;
    private T data;

    // Constructor for success responses
    public CommonResponse(String status, String message, T data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }

    // Constructor for error responses
    public CommonResponse(String status, String message) {
        this.status = status;
        this.message = message;
        this.data = null;
    }

    // Getters and Setters
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
