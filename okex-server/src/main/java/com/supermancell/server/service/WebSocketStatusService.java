package com.supermancell.server.service;

import com.supermancell.server.dto.ConnectionStatusDTO;
import com.supermancell.server.dto.ReconnectionRecordDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Service for tracking and managing WebSocket connection status and reconnection history.
 * Thread-safe implementation using CopyOnWriteArrayList for reconnection history.
 */
@Service
public class WebSocketStatusService {
    
    private static final Logger log = LoggerFactory.getLogger(WebSocketStatusService.class);
    
    // Maximum number of reconnection records to keep in memory
    private static final int MAX_HISTORY_SIZE = 1000;
    
    // Current connection status
    private volatile String status = "DISCONNECTED";
    private volatile String url = "";
    private volatile String connectedAt;
    private volatile String disconnectedAt;
    private volatile String lastMessageTime;
    private volatile int reconnectAttempts = 0;
    private volatile Long currentReconnectDelay;
    
    // Thread-safe list for reconnection history
    private final List<ReconnectionRecordDTO> reconnectionHistory = new CopyOnWriteArrayList<>();
    
    /**
     * Get current connection status
     */
    public ConnectionStatusDTO getConnectionStatus() {
        return ConnectionStatusDTO.builder()
                .status(status)
                .url(url)
                .connectedAt(connectedAt)
                .disconnectedAt(disconnectedAt)
                .lastMessageTime(lastMessageTime)
                .reconnectAttempts(reconnectAttempts)
                .currentReconnectDelay(currentReconnectDelay)
                .build();
    }
    
    /**
     * Update connection status
     * 
     * @param newStatus New status (CONNECTED, DISCONNECTED, CONNECTING, RECONNECTING, ERROR)
     */
    public void updateConnectionStatus(String newStatus) {
        String oldStatus = this.status;
        this.status = newStatus;
        
        log.info("Connection status changed: {} -> {}", oldStatus, newStatus);
        
        // Update timestamps based on status
        String now = Instant.now().toString();
        if ("CONNECTED".equals(newStatus)) {
            this.connectedAt = now;
            this.reconnectAttempts = 0; // Reset on successful connection
            this.currentReconnectDelay = null;
        } else if ("DISCONNECTED".equals(newStatus) || "ERROR".equals(newStatus)) {
            this.disconnectedAt = now;
        }
    }
    
    /**
     * Update WebSocket URL
     */
    public void updateUrl(String url) {
        this.url = url;
    }
    
    /**
     * Update last message time (called whenever a message is received)
     */
    public void updateLastMessageTime() {
        this.lastMessageTime = Instant.now().toString();
    }
    
    /**
     * Record a reconnection attempt
     * 
     * @param reason Reason for reconnection (e.g., "transport-error", "heartbeat-timeout")
     * @param attempt Attempt number
     * @param success Whether the reconnection was successful
     * @param duration Duration in milliseconds (for successful reconnections)
     * @param error Error message (for failed reconnections)
     */
    public void recordReconnection(String reason, int attempt, boolean success, Long duration, String error) {
        ReconnectionRecordDTO record = ReconnectionRecordDTO.builder()
                .id(UUID.randomUUID().toString())
                .timestamp(Instant.now().toString())
                .reason(reason)
                .attempt(attempt)
                .success(success)
                .duration(duration)
                .error(error)
                .build();
        
        reconnectionHistory.add(0, record); // Add to front
        
        // Limit history size
        while (reconnectionHistory.size() > MAX_HISTORY_SIZE) {
            reconnectionHistory.remove(reconnectionHistory.size() - 1);
        }
        
        log.info("Recorded reconnection: attempt={}, success={}, reason={}", attempt, success, reason);
    }
    
    /**
     * Get reconnection history
     * 
     * @param limit Maximum number of records to return
     * @return List of reconnection records, most recent first
     */
    public List<ReconnectionRecordDTO> getReconnectionHistory(int limit) {
        if (limit <= 0) {
            return Collections.emptyList();
        }
        
        int size = reconnectionHistory.size();
        if (size == 0) {
            return Collections.emptyList();
        }
        
        int endIndex = Math.min(limit, size);
        return new ArrayList<>(reconnectionHistory.subList(0, endIndex));
    }
    
    /**
     * Update reconnect attempts counter
     */
    public void updateReconnectAttempts(int attempts) {
        this.reconnectAttempts = attempts;
    }
    
    /**
     * Update current reconnect delay
     */
    public void updateCurrentReconnectDelay(long delayMs) {
        this.currentReconnectDelay = delayMs;
    }
    
    /**
     * Get total number of reconnection records
     */
    public int getReconnectionHistorySize() {
        return reconnectionHistory.size();
    }
    
    /**
     * Clear reconnection history (for testing or maintenance)
     */
    public void clearReconnectionHistory() {
        reconnectionHistory.clear();
        log.info("Reconnection history cleared");
    }
}
