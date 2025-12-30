package com.supermancell.server.service;

import com.supermancell.server.dto.SystemMetricsDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Service for tracking system metrics including WebSocket messages, data processing,
 * cache statistics, and system resource usage.
 * Thread-safe implementation using atomic counters.
 */
@Service
public class SystemMetricsService {
    
    private static final Logger log = LoggerFactory.getLogger(SystemMetricsService.class);
    
    // Message statistics
    private final LongAdder messagesReceived = new LongAdder();
    private final AtomicLong lastMessageCount = new AtomicLong(0);
    private final AtomicLong lastMessageTime = new AtomicLong(System.currentTimeMillis());
    
    // Data processing statistics
    private final LongAdder dataProcessed = new LongAdder();
    
    // Cache statistics
    private final LongAdder cacheHits = new LongAdder();
    private final LongAdder cacheMisses = new LongAdder();
    
    // Connection pool statistics (these would be updated from actual connection pools)
    private volatile int mongodbConnections = 0;
    private volatile int redisConnections = 0;
    
    // System resource beans
    private final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
    private final OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
    
    /**
     * Get current system metrics
     */
    public SystemMetricsDTO getSystemMetrics() {
        return SystemMetricsDTO.builder()
                .messagesReceived(messagesReceived.sum())
                .messagesPerSecond(calculateMessagesPerSecond())
                .dataProcessed(dataProcessed.sum())
                .cacheHitRate(calculateCacheHitRate())
                .mongodbConnections(mongodbConnections)
                .redisConnections(redisConnections)
                .memoryUsage(getMemoryUsage())
                .cpuUsage(getCpuUsage())
                .build();
    }
    
    /**
     * Increment message received counter
     */
    public void incrementMessageCount() {
        messagesReceived.increment();
    }
    
    /**
     * Record data processed (in bytes)
     */
    public void recordDataProcessed(long bytes) {
        if (bytes > 0) {
            dataProcessed.add(bytes);
        }
    }
    
    /**
     * Record a cache access
     * 
     * @param hit true if cache hit, false if cache miss
     */
    public void recordCacheAccess(boolean hit) {
        if (hit) {
            cacheHits.increment();
        } else {
            cacheMisses.increment();
        }
    }
    
    /**
     * Update MongoDB connections count
     */
    public void updateMongodbConnections(int count) {
        this.mongodbConnections = Math.max(0, count);
    }
    
    /**
     * Update Redis connections count
     */
    public void updateRedisConnections(int count) {
        this.redisConnections = Math.max(0, count);
    }
    
    /**
     * Calculate messages per second rate
     * Uses sliding window approach based on time since last calculation
     */
    private double calculateMessagesPerSecond() {
        long currentTime = System.currentTimeMillis();
        long currentCount = messagesReceived.sum();
        
        long lastTime = lastMessageTime.get();
        long lastCount = lastMessageCount.get();
        
        // Calculate time difference in seconds
        double timeDiffSeconds = (currentTime - lastTime) / 1000.0;
        
        if (timeDiffSeconds < 0.1) {
            // Too soon to calculate, return 0
            return 0.0;
        }
        
        // Calculate rate
        double rate = (currentCount - lastCount) / timeDiffSeconds;
        
        // Update last values for next calculation
        lastMessageTime.set(currentTime);
        lastMessageCount.set(currentCount);
        
        return Math.max(0.0, rate);
    }
    
    /**
     * Calculate cache hit rate (0.0 to 1.0)
     */
    private double calculateCacheHitRate() {
        long hits = cacheHits.sum();
        long misses = cacheMisses.sum();
        long total = hits + misses;
        
        if (total == 0) {
            return 0.0;
        }
        
        return (double) hits / total;
    }
    
    /**
     * Get memory usage percentage (0.0 to 1.0)
     */
    private double getMemoryUsage() {
        try {
            long usedMemory = memoryMXBean.getHeapMemoryUsage().getUsed();
            long maxMemory = memoryMXBean.getHeapMemoryUsage().getMax();
            
            if (maxMemory <= 0) {
                return 0.0;
            }
            
            double usage = (double) usedMemory / maxMemory;
            return Math.min(1.0, Math.max(0.0, usage));
        } catch (Exception e) {
            log.warn("Failed to get memory usage", e);
            return 0.0;
        }
    }
    
    /**
     * Get CPU usage percentage (0.0 to 1.0)
     * Note: This is system-wide CPU load, not just JVM process
     */
    private double getCpuUsage() {
        try {
            double load = operatingSystemMXBean.getSystemLoadAverage();
            int processors = operatingSystemMXBean.getAvailableProcessors();
            
            if (load < 0 || processors <= 0) {
                // Load average not available on this platform
                return 0.0;
            }
            
            // Normalize by number of processors
            double usage = load / processors;
            return Math.min(1.0, Math.max(0.0, usage));
        } catch (Exception e) {
            log.warn("Failed to get CPU usage", e);
            return 0.0;
        }
    }
    
    /**
     * Reset all metrics (for testing or maintenance)
     */
    public void resetMetrics() {
        messagesReceived.reset();
        lastMessageCount.set(0);
        lastMessageTime.set(System.currentTimeMillis());
        dataProcessed.reset();
        cacheHits.reset();
        cacheMisses.reset();
        mongodbConnections = 0;
        redisConnections = 0;
        log.info("System metrics reset");
    }
    
    /**
     * Get current message count
     */
    public long getMessageCount() {
        return messagesReceived.sum();
    }
    
    /**
     * Get current data processed
     */
    public long getDataProcessed() {
        return dataProcessed.sum();
    }
    
    /**
     * Get cache statistics summary for logging
     */
    public String getCacheStatsSummary() {
        long hits = cacheHits.sum();
        long misses = cacheMisses.sum();
        double hitRate = calculateCacheHitRate();
        return String.format("Cache stats: hits=%d, misses=%d, hit_rate=%.2f%%", 
            hits, misses, hitRate * 100);
    }
}
