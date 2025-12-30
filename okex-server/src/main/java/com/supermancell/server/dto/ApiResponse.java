package com.supermancell.server.dto;

import java.time.Instant;

/**
 * Generic API response wrapper
 * 
 * @param <T> Type of the response data
 */
public class ApiResponse<T> {
    
    private boolean success;
    private T data;
    private String error;
    private String timestamp;
    
    // Constructors
    public ApiResponse() {}
    
    public ApiResponse(boolean success, T data, String error, String timestamp) {
        this.success = success;
        this.data = data;
        this.error = error;
        this.timestamp = timestamp;
    }
    
    // Getters and Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
    
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    
    /**
     * Create a successful response with data
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, Instant.now().toString());
    }
    
    /**
     * Create a successful response without data
     */
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(true, null, null, Instant.now().toString());
    }
    
    /**
     * Create an error response
     */
    public static <T> ApiResponse<T> error(String errorMessage) {
        return new ApiResponse<>(false, null, errorMessage, Instant.now().toString());
    }
}
