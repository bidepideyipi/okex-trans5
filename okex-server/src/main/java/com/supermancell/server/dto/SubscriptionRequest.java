package com.supermancell.server.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Subscription request data transfer object
 * Used for adding/removing subscriptions
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionRequest {
    
    /**
     * Trading symbol (e.g., "BTC-USDT-SWAP", "ETH-USDT-SWAP")
     */
    private String symbol;
    
    /**
     * Time interval (e.g., "1m", "1H", "1D")
     */
    private String interval;
}
