package com.supermancell.server.dto;

import java.util.List;

/**
 * Subscription update request data transfer object
 * Used for dynamically updating subscriptions
 */
public class SubscriptionRequest {
    
    /**
     * List of trading symbols (e.g., ["BTC-USDT-SWAP", "ETH-USDT-SWAP"])
     */
    private List<String> symbols;
    
    /**
     * List of time intervals (e.g., ["1m", "1H"])
     */
    private List<String> intervals;
    
    // Constructors
    public SubscriptionRequest() {}
    
    public SubscriptionRequest(List<String> symbols, List<String> intervals) {
        this.symbols = symbols;
        this.intervals = intervals;
    }
    
    // Getters and Setters
    public List<String> getSymbols() { return symbols; }
    public void setSymbols(List<String> symbols) { this.symbols = symbols; }
    
    public List<String> getIntervals() { return intervals; }
    public void setIntervals(List<String> intervals) { this.intervals = intervals; }
}
