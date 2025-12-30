package com.supermancell.server.dto;

/**
 * Subscription information data transfer object
 */
public class SubscriptionDTO {
    
    /**
     * Trading symbol (e.g., "BTC-USDT-SWAP")
     */
    private String symbol;
    
    /**
     * Time interval (e.g., "1m", "1H")
     */
    private String interval;
    
    /**
     * Subscription timestamp (ISO format)
     */
    private String subscribedAt;
    
    /**
     * Total number of messages received for this subscription
     */
    private long messagesReceived;
    
    /**
     * Last update timestamp (ISO format)
     */
    private String lastUpdate;
    
    // Constructors
    public SubscriptionDTO() {}
    
    public SubscriptionDTO(String symbol, String interval, String subscribedAt, long messagesReceived, String lastUpdate) {
        this.symbol = symbol;
        this.interval = interval;
        this.subscribedAt = subscribedAt;
        this.messagesReceived = messagesReceived;
        this.lastUpdate = lastUpdate;
    }
    
    // Getters and Setters
    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    
    public String getInterval() { return interval; }
    public void setInterval(String interval) { this.interval = interval; }
    
    public String getSubscribedAt() { return subscribedAt; }
    public void setSubscribedAt(String subscribedAt) { this.subscribedAt = subscribedAt; }
    
    public long getMessagesReceived() { return messagesReceived; }
    public void setMessagesReceived(long messagesReceived) { this.messagesReceived = messagesReceived; }
    
    public String getLastUpdate() { return lastUpdate; }
    public void setLastUpdate(String lastUpdate) { this.lastUpdate = lastUpdate; }
    
    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String symbol;
        private String interval;
        private String subscribedAt;
        private long messagesReceived;
        private String lastUpdate;
        
        public Builder symbol(String symbol) {
            this.symbol = symbol;
            return this;
        }
        
        public Builder interval(String interval) {
            this.interval = interval;
            return this;
        }
        
        public Builder subscribedAt(String subscribedAt) {
            this.subscribedAt = subscribedAt;
            return this;
        }
        
        public Builder messagesReceived(long messagesReceived) {
            this.messagesReceived = messagesReceived;
            return this;
        }
        
        public Builder lastUpdate(String lastUpdate) {
            this.lastUpdate = lastUpdate;
            return this;
        }
        
        public SubscriptionDTO build() {
            return new SubscriptionDTO(symbol, interval, subscribedAt, messagesReceived, lastUpdate);
        }
    }
}
