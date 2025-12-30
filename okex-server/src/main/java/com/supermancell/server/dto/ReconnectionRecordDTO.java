package com.supermancell.server.dto;

/**
 * Reconnection history record data transfer object
 */
public class ReconnectionRecordDTO {
    
    private String id;
    private String timestamp;  // ISO format
    private String reason;  // "transport-error", "heartbeat-timeout", "connection-closed"
    private int attempt;
    private boolean success;
    private Long duration;  // milliseconds, only for successful reconnections
    private String error;   // only for failed reconnections
    
    // Constructors
    public ReconnectionRecordDTO() {}
    
    private ReconnectionRecordDTO(Builder builder) {
        this.id = builder.id;
        this.timestamp = builder.timestamp;
        this.reason = builder.reason;
        this.attempt = builder.attempt;
        this.success = builder.success;
        this.duration = builder.duration;
        this.error = builder.error;
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    
    public int getAttempt() { return attempt; }
    public void setAttempt(int attempt) { this.attempt = attempt; }
    
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public Long getDuration() { return duration; }
    public void setDuration(Long duration) { this.duration = duration; }
    
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    
    // Builder
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String id;
        private String timestamp;
        private String reason;
        private int attempt;
        private boolean success;
        private Long duration;
        private String error;
        
        public Builder id(String id) {
            this.id = id;
            return this;
        }
        
        public Builder timestamp(String timestamp) {
            this.timestamp = timestamp;
            return this;
        }
        
        public Builder reason(String reason) {
            this.reason = reason;
            return this;
        }
        
        public Builder attempt(int attempt) {
            this.attempt = attempt;
            return this;
        }
        
        public Builder success(boolean success) {
            this.success = success;
            return this;
        }
        
        public Builder duration(Long duration) {
            this.duration = duration;
            return this;
        }
        
        public Builder error(String error) {
            this.error = error;
            return this;
        }
        
        public ReconnectionRecordDTO build() {
            return new ReconnectionRecordDTO(this);
        }
    }
}
