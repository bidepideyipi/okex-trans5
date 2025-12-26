package com.supermancell.common.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public class Candle {
    @JsonProperty("symbol")
    private String symbol;
    
    @JsonProperty("timestamp")
    private Instant timestamp;
    
    @JsonProperty("interval")
    private String interval;
    
    @JsonProperty("open")
    private double open;
    
    @JsonProperty("high")
    private double high;
    
    @JsonProperty("low")
    private double low;
    
    @JsonProperty("close")
    private double close;
    
    @JsonProperty("volume")
    private double volume;
    
    @JsonProperty("created_at")
    private Instant createdAt;

    public Candle() {}

    public Candle(String symbol, Instant timestamp, String interval, 
                  double open, double high, double low, double close, double volume) {
        this.symbol = symbol;
        this.timestamp = timestamp;
        this.interval = interval;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
        this.createdAt = Instant.now();
    }

    // Getters and Setters
    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getInterval() {
        return interval;
    }

    public void setInterval(String interval) {
        this.interval = interval;
    }

    public double getOpen() {
        return open;
    }

    public void setOpen(double open) {
        this.open = open;
    }

    public double getHigh() {
        return high;
    }

    public void setHigh(double high) {
        this.high = high;
    }

    public double getLow() {
        return low;
    }

    public void setLow(double low) {
        this.low = low;
    }

    public double getClose() {
        return close;
    }

    public void setClose(double close) {
        this.close = close;
    }

    public double getVolume() {
        return volume;
    }

    public void setVolume(double volume) {
        this.volume = volume;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Candle{" +
                "symbol='" + symbol + '\'' +
                ", timestamp=" + timestamp +
                ", interval='" + interval + '\'' +
                ", open=" + open +
                ", high=" + high +
                ", low=" + low +
                ", close=" + close +
                ", volume=" + volume +
                ", createdAt=" + createdAt +
                '}';
    }
}