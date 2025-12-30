package com.supermancell.server.dto;

/**
 * WebSocket connection status data transfer object
 */
public class ConnectionStatusDTO {
    
    /**
     * Connection status: CONNECTED, DISCONNECTED, CONNECTING, RECONNECTING, ERROR
     */
    private String status;
    
    /**
     * WebSocket URL
     */
    private String url;
    
    /**
     * Connection established time (ISO format)
     */
    private String connectedAt;
    
    /**
     * Disconnection time (ISO format)
     */
    private String disconnectedAt;
    
    /**
     * Last message received time (ISO format)
     */
    private String lastMessageTime;
    
    /**
     * Current reconnection attempt count
     */
    private int reconnectAttempts;
    
    /**
     * Current reconnection delay in milliseconds
     */
    private Long currentReconnectDelay;
    
    // Constructors
    public ConnectionStatusDTO() {}
    
    private ConnectionStatusDTO(Builder builder) {
        this.status = builder.status;
        this.url = builder.url;
        this.connectedAt = builder.connectedAt;
        this.disconnectedAt = builder.disconnectedAt;
        this.lastMessageTime = builder.lastMessageTime;
        this.reconnectAttempts = builder.reconnectAttempts;
        this.currentReconnectDelay = builder.currentReconnectDelay;
    }
    
    // Getters and Setters
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    
    public String getConnectedAt() { return connectedAt; }
    public void setConnectedAt(String connectedAt) { this.connectedAt = connectedAt; }
    
    public String getDisconnectedAt() { return disconnectedAt; }
    public void setDisconnectedAt(String disconnectedAt) { this.disconnectedAt = disconnectedAt; }
    
    public String getLastMessageTime() { return lastMessageTime; }
    public void setLastMessageTime(String lastMessageTime) { this.lastMessageTime = lastMessageTime; }
    
    public int getReconnectAttempts() { return reconnectAttempts; }
    public void setReconnectAttempts(int reconnectAttempts) { this.reconnectAttempts = reconnectAttempts; }
    
    public Long getCurrentReconnectDelay() { return currentReconnectDelay; }
    public void setCurrentReconnectDelay(Long currentReconnectDelay) { this.currentReconnectDelay = currentReconnectDelay; }
    
    // Builder
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String status;
        private String url;
        private String connectedAt;
        private String disconnectedAt;
        private String lastMessageTime;
        private int reconnectAttempts;
        private Long currentReconnectDelay;
        
        public Builder status(String status) {
            this.status = status;
            return this;
        }
        
        public Builder url(String url) {
            this.url = url;
            return this;
        }
        
        public Builder connectedAt(String connectedAt) {
            this.connectedAt = connectedAt;
            return this;
        }
        
        public Builder disconnectedAt(String disconnectedAt) {
            this.disconnectedAt = disconnectedAt;
            return this;
        }
        
        public Builder lastMessageTime(String lastMessageTime) {
            this.lastMessageTime = lastMessageTime;
            return this;
        }
        
        public Builder reconnectAttempts(int reconnectAttempts) {
            this.reconnectAttempts = reconnectAttempts;
            return this;
        }
        
        public Builder currentReconnectDelay(Long currentReconnectDelay) {
            this.currentReconnectDelay = currentReconnectDelay;
            return this;
        }
        
        public ConnectionStatusDTO build() {
            return new ConnectionStatusDTO(this);
        }
    }
}
