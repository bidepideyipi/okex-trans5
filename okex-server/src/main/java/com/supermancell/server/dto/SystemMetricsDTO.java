package com.supermancell.server.dto;

/**
 * System metrics data transfer object
 */
public class SystemMetricsDTO {
    
    private long messagesReceived;
    private double messagesPerSecond;
    private long dataProcessed;  // bytes
    private double cacheHitRate;  // 0.0 to 1.0
    private int mongodbConnections;
    private int redisConnections;
    private double memoryUsage;  // 0.0 to 1.0
    private double cpuUsage;     // 0.0 to 1.0
    
    // Constructors
    public SystemMetricsDTO() {}
    
    private SystemMetricsDTO(Builder builder) {
        this.messagesReceived = builder.messagesReceived;
        this.messagesPerSecond = builder.messagesPerSecond;
        this.dataProcessed = builder.dataProcessed;
        this.cacheHitRate = builder.cacheHitRate;
        this.mongodbConnections = builder.mongodbConnections;
        this.redisConnections = builder.redisConnections;
        this.memoryUsage = builder.memoryUsage;
        this.cpuUsage = builder.cpuUsage;
    }
    
    // Getters and Setters
    public long getMessagesReceived() { return messagesReceived; }
    public void setMessagesReceived(long messagesReceived) { this.messagesReceived = messagesReceived; }
    
    public double getMessagesPerSecond() { return messagesPerSecond; }
    public void setMessagesPerSecond(double messagesPerSecond) { this.messagesPerSecond = messagesPerSecond; }
    
    public long getDataProcessed() { return dataProcessed; }
    public void setDataProcessed(long dataProcessed) { this.dataProcessed = dataProcessed; }
    
    public double getCacheHitRate() { return cacheHitRate; }
    public void setCacheHitRate(double cacheHitRate) { this.cacheHitRate = cacheHitRate; }
    
    public int getMongodbConnections() { return mongodbConnections; }
    public void setMongodbConnections(int mongodbConnections) { this.mongodbConnections = mongodbConnections; }
    
    public int getRedisConnections() { return redisConnections; }
    public void setRedisConnections(int redisConnections) { this.redisConnections = redisConnections; }
    
    public double getMemoryUsage() { return memoryUsage; }
    public void setMemoryUsage(double memoryUsage) { this.memoryUsage = memoryUsage; }
    
    public double getCpuUsage() { return cpuUsage; }
    public void setCpuUsage(double cpuUsage) { this.cpuUsage = cpuUsage; }
    
    // Builder
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private long messagesReceived;
        private double messagesPerSecond;
        private long dataProcessed;
        private double cacheHitRate;
        private int mongodbConnections;
        private int redisConnections;
        private double memoryUsage;
        private double cpuUsage;
        
        public Builder messagesReceived(long messagesReceived) {
            this.messagesReceived = messagesReceived;
            return this;
        }
        
        public Builder messagesPerSecond(double messagesPerSecond) {
            this.messagesPerSecond = messagesPerSecond;
            return this;
        }
        
        public Builder dataProcessed(long dataProcessed) {
            this.dataProcessed = dataProcessed;
            return this;
        }
        
        public Builder cacheHitRate(double cacheHitRate) {
            this.cacheHitRate = cacheHitRate;
            return this;
        }
        
        public Builder mongodbConnections(int mongodbConnections) {
            this.mongodbConnections = mongodbConnections;
            return this;
        }
        
        public Builder redisConnections(int redisConnections) {
            this.redisConnections = redisConnections;
            return this;
        }
        
        public Builder memoryUsage(double memoryUsage) {
            this.memoryUsage = memoryUsage;
            return this;
        }
        
        public Builder cpuUsage(double cpuUsage) {
            this.cpuUsage = cpuUsage;
            return this;
        }
        
        public SystemMetricsDTO build() {
            return new SystemMetricsDTO(this);
        }
    }
}
