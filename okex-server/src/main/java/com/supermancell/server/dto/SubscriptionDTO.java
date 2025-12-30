package com.supermancell.server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Subscription information data transfer object
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
}
