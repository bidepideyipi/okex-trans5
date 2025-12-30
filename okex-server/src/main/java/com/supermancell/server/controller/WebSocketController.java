package com.supermancell.server.controller;

import com.supermancell.server.dto.ApiResponse;
import com.supermancell.server.dto.ConnectionStatusDTO;
import com.supermancell.server.dto.ReconnectionRecordDTO;
import com.supermancell.server.service.WebSocketStatusService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for WebSocket connection monitoring
 */
@RestController
@RequestMapping("/api/websocket")
public class WebSocketController {
    
    private static final Logger log = LoggerFactory.getLogger(WebSocketController.class);
    
    private final WebSocketStatusService statusService;
    
    public WebSocketController(WebSocketStatusService statusService) {
        this.statusService = statusService;
    }
    
    /**
     * Get current WebSocket connection status
     * 
     * GET /api/websocket/status
     * 
     * @return Current connection status including URL, timestamps, and reconnection info
     */
    @GetMapping("/status")
    public ApiResponse<ConnectionStatusDTO> getConnectionStatus() {
        try {
            ConnectionStatusDTO status = statusService.getConnectionStatus();
            log.debug("Retrieved connection status: {}", status.getStatus());
            return ApiResponse.success(status);
        } catch (Exception e) {
            log.error("Failed to get connection status", e);
            return ApiResponse.error("Failed to retrieve connection status: " + e.getMessage());
        }
    }
    
    /**
     * Get reconnection history
     * 
     * GET /api/websocket/reconnect-history?limit=50
     * 
     * @param limit Maximum number of records to return (default: 50, max: 1000)
     * @return List of reconnection records, most recent first
     */
    @GetMapping("/reconnect-history")
    public ApiResponse<List<ReconnectionRecordDTO>> getReconnectionHistory(
            @RequestParam(name = "limit", defaultValue = "50") int limit) {
        try {
            // Validate limit
            if (limit <= 0) {
                return ApiResponse.error("Limit must be greater than 0");
            }
            if (limit > 1000) {
                limit = 1000; // Cap at maximum
            }
            
            List<ReconnectionRecordDTO> history = statusService.getReconnectionHistory(limit);
            log.debug("Retrieved {} reconnection records", history.size());
            return ApiResponse.success(history);
        } catch (Exception e) {
            log.error("Failed to get reconnection history", e);
            return ApiResponse.error("Failed to retrieve reconnection history: " + e.getMessage());
        }
    }
    
    /**
     * Get reconnection history statistics
     * 
     * GET /api/websocket/reconnect-stats
     * 
     * @return Statistics about reconnection history
     */
    @GetMapping("/reconnect-stats")
    public ApiResponse<ReconnectionStats> getReconnectionStats() {
        try {
            int totalRecords = statusService.getReconnectionHistorySize();
            List<ReconnectionRecordDTO> recent = statusService.getReconnectionHistory(100);
            
            long successCount = recent.stream().filter(ReconnectionRecordDTO::isSuccess).count();
            long failureCount = recent.size() - successCount;
            
            ReconnectionStats stats = new ReconnectionStats(
                totalRecords,
                recent.size(),
                (int) successCount,
                (int) failureCount
            );
            
            log.debug("Retrieved reconnection stats: total={}, success={}, failure={}", 
                totalRecords, successCount, failureCount);
            return ApiResponse.success(stats);
        } catch (Exception e) {
            log.error("Failed to get reconnection stats", e);
            return ApiResponse.error("Failed to retrieve reconnection statistics: " + e.getMessage());
        }
    }
    
    /**
     * Statistics about reconnection history
     */
    public static class ReconnectionStats {
        private int totalRecords;
        private int recentCount;
        private int successCount;
        private int failureCount;
        
        public ReconnectionStats(int totalRecords, int recentCount, int successCount, int failureCount) {
            this.totalRecords = totalRecords;
            this.recentCount = recentCount;
            this.successCount = successCount;
            this.failureCount = failureCount;
        }
        
        public int getTotalRecords() { return totalRecords; }
        public void setTotalRecords(int totalRecords) { this.totalRecords = totalRecords; }
        
        public int getRecentCount() { return recentCount; }
        public void setRecentCount(int recentCount) { this.recentCount = recentCount; }
        
        public int getSuccessCount() { return successCount; }
        public void setSuccessCount(int successCount) { this.successCount = successCount; }
        
        public int getFailureCount() { return failureCount; }
        public void setFailureCount(int failureCount) { this.failureCount = failureCount; }
    }
}
