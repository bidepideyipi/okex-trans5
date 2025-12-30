package com.supermancell.server.controller;

import com.supermancell.server.dto.ApiResponse;
import com.supermancell.server.dto.SubscriptionDTO;
import com.supermancell.server.dto.SubscriptionRequest;
import com.supermancell.server.websocket.OkexWebSocketClient;
import com.supermancell.server.websocket.SubscriptionConfig;
import com.supermancell.server.websocket.SubscriptionConfigLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * REST controller for subscription management
 */
@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {
    
    private static final Logger log = LoggerFactory.getLogger(SubscriptionController.class);
    
    private final SubscriptionConfigLoader configLoader;
    private final OkexWebSocketClient webSocketClient;
    
    public SubscriptionController(SubscriptionConfigLoader configLoader, OkexWebSocketClient webSocketClient) {
        this.configLoader = configLoader;
        this.webSocketClient = webSocketClient;
    }
    
    /**
     * Get current active subscriptions
     * 
     * GET /api/subscriptions
     * 
     * @return List of active subscription pairs (symbol + interval combinations)
     */
    @GetMapping
    public ApiResponse<List<SubscriptionDTO>> getActiveSubscriptions() {
        try {
            SubscriptionConfig config = configLoader.loadCurrentConfig();
            List<SubscriptionDTO> subscriptions = new ArrayList<>();
            
            String currentTime = Instant.now().toString();
            
            // Build all subscription pairs
            for (String symbol : config.getSymbols()) {
                for (String interval : config.getIntervals()) {
                    SubscriptionDTO dto = SubscriptionDTO.builder()
                            .symbol(symbol)
                            .interval(interval)
                            .subscribedAt(currentTime)
                            .messagesReceived(0) // TODO: Track actual message counts per subscription
                            .lastUpdate(currentTime)
                            .build();
                    subscriptions.add(dto);
                }
            }
            
            log.debug("Retrieved {} active subscriptions", subscriptions.size());
            return ApiResponse.success(subscriptions);
        } catch (Exception e) {
            log.error("Failed to get active subscriptions", e);
            return ApiResponse.error("Failed to retrieve active subscriptions: " + e.getMessage());
        }
    }
    
    /**
     * Get subscription configuration
     * 
     * GET /api/subscriptions/config
     * 
     * @return Current subscription configuration from application.yml
     */
    @GetMapping("/config")
    public ApiResponse<SubscriptionConfigDTO> getSubscriptionConfig() {
        try {
            SubscriptionConfig config = configLoader.loadCurrentConfig();
            
            SubscriptionConfigDTO dto = new SubscriptionConfigDTO(
                config.getSymbols(),
                config.getIntervals(),
                configLoader.getRefreshIntervalMs()
            );
            
            log.debug("Retrieved subscription config: symbols={}, intervals={}", 
                config.getSymbols().size(), config.getIntervals().size());
            return ApiResponse.success(dto);
        } catch (Exception e) {
            log.error("Failed to get subscription config", e);
            return ApiResponse.error("Failed to retrieve subscription configuration: " + e.getMessage());
        }
    }
    
    /**
     * Update subscriptions dynamically
     * 
     * POST /api/subscriptions/update
     * 
     * @param request New subscription configuration
     * @return Updated subscription status
     */
    @PostMapping("/update")
    public ApiResponse<String> updateSubscriptions(@RequestBody SubscriptionRequest request) {
        try {
            // Validate request
            if (request.getSymbols() == null || request.getSymbols().isEmpty()) {
                return ApiResponse.error("Symbols list cannot be empty");
            }
            if (request.getIntervals() == null || request.getIntervals().isEmpty()) {
                return ApiResponse.error("Intervals list cannot be empty");
            }
            
            // Create new config
            SubscriptionConfig newConfig = new SubscriptionConfig(
                request.getSymbols(),
                request.getIntervals()
            );
            
            // Apply to WebSocket client
            webSocketClient.applySubscriptions(newConfig);
            
            int totalSubscriptions = request.getSymbols().size() * request.getIntervals().size();
            log.info("Updated subscriptions: {} symbols x {} intervals = {} total subscriptions",
                request.getSymbols().size(), request.getIntervals().size(), totalSubscriptions);
            
            return ApiResponse.success(
                String.format("Successfully updated subscriptions: %d symbols, %d intervals, %d total pairs",
                    request.getSymbols().size(), request.getIntervals().size(), totalSubscriptions)
            );
        } catch (Exception e) {
            log.error("Failed to update subscriptions", e);
            return ApiResponse.error("Failed to update subscriptions: " + e.getMessage());
        }
    }
    
    /**
     * Subscription configuration DTO
     */
    public static class SubscriptionConfigDTO {
        private List<String> symbols;
        private List<String> intervals;
        private long refreshIntervalMs;
        
        public SubscriptionConfigDTO(List<String> symbols, List<String> intervals, long refreshIntervalMs) {
            this.symbols = symbols;
            this.intervals = intervals;
            this.refreshIntervalMs = refreshIntervalMs;
        }
        
        public List<String> getSymbols() { return symbols; }
        public void setSymbols(List<String> symbols) { this.symbols = symbols; }
        
        public List<String> getIntervals() { return intervals; }
        public void setIntervals(List<String> intervals) { this.intervals = intervals; }
        
        public long getRefreshIntervalMs() { return refreshIntervalMs; }
        public void setRefreshIntervalMs(long refreshIntervalMs) { this.refreshIntervalMs = refreshIntervalMs; }
    }
}
