package com.supermancell.server.controller;

import com.supermancell.server.dto.ApiResponse;
import com.supermancell.server.dto.SystemMetricsDTO;
import com.supermancell.server.service.SystemMetricsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for system metrics monitoring
 */
@RestController
@RequestMapping("/api/metrics")
public class MetricsController {
    
    private static final Logger log = LoggerFactory.getLogger(MetricsController.class);
    
    private final SystemMetricsService metricsService;
    
    public MetricsController(SystemMetricsService metricsService) {
        this.metricsService = metricsService;
    }
    
    /**
     * Get current system metrics
     * 
     * GET /api/metrics
     * 
     * @return Current system metrics including message rates, data processing, cache, and resource usage
     */
    @GetMapping
    public ApiResponse<SystemMetricsDTO> getSystemMetrics() {
        try {
            SystemMetricsDTO metrics = metricsService.getSystemMetrics();
            log.debug("Retrieved system metrics: messages={}, mps={}", 
                metrics.getMessagesReceived(), metrics.getMessagesPerSecond());
            return ApiResponse.success(metrics);
        } catch (Exception e) {
            log.error("Failed to get system metrics", e);
            return ApiResponse.error("Failed to retrieve system metrics: " + e.getMessage());
        }
    }
    
    /**
     * Get message statistics
     * 
     * GET /api/metrics/messages
     * 
     * @return Message-related metrics only
     */
    @GetMapping("/messages")
    public ApiResponse<MessageMetrics> getMessageMetrics() {
        try {
            SystemMetricsDTO metrics = metricsService.getSystemMetrics();
            MessageMetrics messageMetrics = new MessageMetrics(
                metrics.getMessagesReceived(),
                metrics.getMessagesPerSecond(),
                metrics.getDataProcessed()
            );
            return ApiResponse.success(messageMetrics);
        } catch (Exception e) {
            log.error("Failed to get message metrics", e);
            return ApiResponse.error("Failed to retrieve message metrics: " + e.getMessage());
        }
    }
    
    /**
     * Get resource usage metrics
     * 
     * GET /api/metrics/resources
     * 
     * @return Resource usage metrics (CPU, memory, connections)
     */
    @GetMapping("/resources")
    public ApiResponse<ResourceMetrics> getResourceMetrics() {
        try {
            SystemMetricsDTO metrics = metricsService.getSystemMetrics();
            ResourceMetrics resourceMetrics = new ResourceMetrics(
                metrics.getMemoryUsage(),
                metrics.getCpuUsage(),
                metrics.getMongodbConnections(),
                metrics.getRedisConnections()
            );
            return ApiResponse.success(resourceMetrics);
        } catch (Exception e) {
            log.error("Failed to get resource metrics", e);
            return ApiResponse.error("Failed to retrieve resource metrics: " + e.getMessage());
        }
    }
    
    /**
     * Get cache statistics
     * 
     * GET /api/metrics/cache
     * 
     * @return Cache hit rate and statistics
     */
    @GetMapping("/cache")
    public ApiResponse<CacheMetrics> getCacheMetrics() {
        try {
            SystemMetricsDTO metrics = metricsService.getSystemMetrics();
            String summary = metricsService.getCacheStatsSummary();
            CacheMetrics cacheMetrics = new CacheMetrics(
                metrics.getCacheHitRate(),
                summary
            );
            return ApiResponse.success(cacheMetrics);
        } catch (Exception e) {
            log.error("Failed to get cache metrics", e);
            return ApiResponse.error("Failed to retrieve cache metrics: " + e.getMessage());
        }
    }
    
    /**
     * Reset all metrics (for testing/maintenance)
     * 
     * POST /api/metrics/reset
     * 
     * @return Success response
     */
    @PostMapping("/reset")
    public ApiResponse<String> resetMetrics() {
        try {
            metricsService.resetMetrics();
            log.info("System metrics reset");
            return ApiResponse.success("Metrics reset successfully");
        } catch (Exception e) {
            log.error("Failed to reset metrics", e);
            return ApiResponse.error("Failed to reset metrics: " + e.getMessage());
        }
    }
    
    /**
     * Message-related metrics
     */
    public static class MessageMetrics {
        private long messagesReceived;
        private double messagesPerSecond;
        private long dataProcessed;
        
        public MessageMetrics(long messagesReceived, double messagesPerSecond, long dataProcessed) {
            this.messagesReceived = messagesReceived;
            this.messagesPerSecond = messagesPerSecond;
            this.dataProcessed = dataProcessed;
        }
        
        public long getMessagesReceived() { return messagesReceived; }
        public void setMessagesReceived(long messagesReceived) { this.messagesReceived = messagesReceived; }
        
        public double getMessagesPerSecond() { return messagesPerSecond; }
        public void setMessagesPerSecond(double messagesPerSecond) { this.messagesPerSecond = messagesPerSecond; }
        
        public long getDataProcessed() { return dataProcessed; }
        public void setDataProcessed(long dataProcessed) { this.dataProcessed = dataProcessed; }
    }
    
    /**
     * Resource usage metrics
     */
    public static class ResourceMetrics {
        private double memoryUsage;
        private double cpuUsage;
        private int mongodbConnections;
        private int redisConnections;
        
        public ResourceMetrics(double memoryUsage, double cpuUsage, int mongodbConnections, int redisConnections) {
            this.memoryUsage = memoryUsage;
            this.cpuUsage = cpuUsage;
            this.mongodbConnections = mongodbConnections;
            this.redisConnections = redisConnections;
        }
        
        public double getMemoryUsage() { return memoryUsage; }
        public void setMemoryUsage(double memoryUsage) { this.memoryUsage = memoryUsage; }
        
        public double getCpuUsage() { return cpuUsage; }
        public void setCpuUsage(double cpuUsage) { this.cpuUsage = cpuUsage; }
        
        public int getMongodbConnections() { return mongodbConnections; }
        public void setMongodbConnections(int mongodbConnections) { this.mongodbConnections = mongodbConnections; }
        
        public int getRedisConnections() { return redisConnections; }
        public void setRedisConnections(int redisConnections) { this.redisConnections = redisConnections; }
    }
    
    /**
     * Cache-related metrics
     */
    public static class CacheMetrics {
        private double hitRate;
        private String summary;
        
        public CacheMetrics(double hitRate, String summary) {
            this.hitRate = hitRate;
            this.summary = summary;
        }
        
        public double getHitRate() { return hitRate; }
        public void setHitRate(double hitRate) { this.hitRate = hitRate; }
        
        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }
    }
}
