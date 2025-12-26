package com.supermancell.server.websocket;

import com.supermancell.common.model.Candle;
import com.supermancell.server.repository.CandleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Batch writer for candle data with time window optimization.
 * 
 * Key features:
 * - Deduplicates candles by (symbol + interval + timestamp)
 * - Flushes to database every configured interval (default 20 seconds)
 * - Thread-safe using ConcurrentHashMap
 * - Only keeps the latest candle for each unique key
 */
@Component
public class CandleBatchWriter {

    private static final Logger log = LoggerFactory.getLogger(CandleBatchWriter.class);

    private final CandleRepository candleRepository;
    private final Map<CandleKey, Candle> candleBuffer = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @Value("${candle.batch.flush.interval.seconds:20}")
    private int flushIntervalSeconds;

    public CandleBatchWriter(CandleRepository candleRepository) {
        this.candleRepository = candleRepository;
    }

    @PostConstruct
    public void init() {
        // Start scheduled flush task
        scheduler.scheduleAtFixedRate(
            this::flush,
            flushIntervalSeconds,
            flushIntervalSeconds,
            TimeUnit.SECONDS
        );
        log.info("CandleBatchWriter initialized with flush interval: {} seconds", flushIntervalSeconds);
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down CandleBatchWriter...");
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        // Flush remaining data before shutdown
        flush();
        log.info("CandleBatchWriter shutdown complete");
    }

    /**
     * Add a candle to the buffer. If a candle with the same key already exists,
     * it will be replaced (keeping only the latest version).
     * 
     * @param candle The candle to buffer
     */
    public void addCandle(Candle candle) {
        if (candle == null) {
            return;
        }
        
        CandleKey key = new CandleKey(
            candle.getSymbol(),
            candle.getInterval(),
            candle.getTimestamp().toEpochMilli()
        );
        
        candleBuffer.put(key, candle);
        log.debug("Buffered candle: {} (buffer size: {})", key, candleBuffer.size());
    }

    /**
     * Flush all buffered candles to the database.
     * This is called automatically by the scheduler and during shutdown.
     */
    public synchronized void flush() {
        if (candleBuffer.isEmpty()) {
            log.debug("No candles to flush");
            return;
        }

        List<Candle> candlesToSave = new ArrayList<>(candleBuffer.values());
        int count = candlesToSave.size();
        
        try {
            long startTime = System.currentTimeMillis();
            candleRepository.saveBatch(candlesToSave);
            long duration = System.currentTimeMillis() - startTime;
            
            candleBuffer.clear();
            log.info("Flushed {} candles to database in {}ms", count, duration);
        } catch (Exception e) {
            log.error("Failed to flush {} candles to database", count, e);
            // Don't clear buffer on error to allow retry on next flush
        }
    }

    /**
     * Get current buffer size (for monitoring/testing).
     */
    public int getBufferSize() {
        return candleBuffer.size();
    }

    /**
     * Composite key for deduplicating candles.
     * Uses symbol + interval + timestamp as the unique identifier.
     */
    private static class CandleKey {
        private final String symbol;
        private final String interval;
        private final long timestamp;

        CandleKey(String symbol, String interval, long timestamp) {
            this.symbol = symbol;
            this.interval = interval;
            this.timestamp = timestamp;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CandleKey that = (CandleKey) o;
            return timestamp == that.timestamp &&
                   symbol.equals(that.symbol) &&
                   interval.equals(that.interval);
        }

        @Override
        public int hashCode() {
            int result = symbol.hashCode();
            result = 31 * result + interval.hashCode();
            result = 31 * result + Long.hashCode(timestamp);
            return result;
        }

        @Override
        public String toString() {
            return symbol + ":" + interval + ":" + timestamp;
        }
    }
}
