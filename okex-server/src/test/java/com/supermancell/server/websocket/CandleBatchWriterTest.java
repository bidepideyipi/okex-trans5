package com.supermancell.server.websocket;

import com.supermancell.common.model.Candle;
import com.supermancell.server.repository.CandleRepository;
import com.supermancell.server.service.SystemMetricsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CandleBatchWriterTest {

    @Mock
    private CandleRepository candleRepository;
    
    @Mock
    private SystemMetricsService metricsService;

    private CandleBatchWriter batchWriter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        batchWriter = new CandleBatchWriter(candleRepository, metricsService);
        // Set flush interval to prevent automatic scheduling during tests
        ReflectionTestUtils.setField(batchWriter, "flushIntervalSeconds", 3600);
    }

    @Test
    void shouldBufferSingleCandle() {
        Candle candle = createCandle("BTC-USDT-SWAP", "1m", 1703505600000L);
        
        batchWriter.addCandle(candle);
        
        assertEquals(1, batchWriter.getBufferSize());
        verify(candleRepository, never()).saveBatch(any());
    }

    @Test
    void shouldDeduplicateCandlesWithSameKey() {
        long timestamp = 1703505600000L;
        
        // Add same candle 3 times (simulating 3 updates within 1 minute)
        Candle candle1 = createCandle("BTC-USDT-SWAP", "1m", timestamp);
        candle1.setClose(42000.0);
        batchWriter.addCandle(candle1);
        
        Candle candle2 = createCandle("BTC-USDT-SWAP", "1m", timestamp);
        candle2.setClose(42050.0);
        batchWriter.addCandle(candle2);
        
        Candle candle3 = createCandle("BTC-USDT-SWAP", "1m", timestamp);
        candle3.setClose(42100.0);
        batchWriter.addCandle(candle3);
        
        // Buffer should contain only 1 candle (deduplicated)
        assertEquals(1, batchWriter.getBufferSize());
        
        // Flush and verify only one candle is saved with the latest value
        batchWriter.flush();
        
        ArgumentCaptor<List<Candle>> captor = ArgumentCaptor.forClass(List.class);
        verify(candleRepository, times(1)).saveBatch(captor.capture());
        
        List<Candle> savedCandles = captor.getValue();
        assertEquals(1, savedCandles.size());
        assertEquals(42100.0, savedCandles.get(0).getClose(), 0.01);
    }

    @Test
    void shouldKeepCandlesWithDifferentKeys() {
        long timestamp = 1703505600000L;
        
        // Different symbol
        Candle candle1 = createCandle("BTC-USDT-SWAP", "1m", timestamp);
        batchWriter.addCandle(candle1);
        
        Candle candle2 = createCandle("ETH-USDT-SWAP", "1m", timestamp);
        batchWriter.addCandle(candle2);
        
        // Different interval
        Candle candle3 = createCandle("BTC-USDT-SWAP", "1H", timestamp);
        batchWriter.addCandle(candle3);
        
        // Different timestamp
        Candle candle4 = createCandle("BTC-USDT-SWAP", "1m", timestamp + 60000L);
        batchWriter.addCandle(candle4);
        
        // Buffer should contain 4 different candles
        assertEquals(4, batchWriter.getBufferSize());
        
        batchWriter.flush();
        
        ArgumentCaptor<List<Candle>> captor = ArgumentCaptor.forClass(List.class);
        verify(candleRepository, times(1)).saveBatch(captor.capture());
        
        List<Candle> savedCandles = captor.getValue();
        assertEquals(4, savedCandles.size());
    }

    @Test
    void shouldClearBufferAfterSuccessfulFlush() {
        Candle candle = createCandle("BTC-USDT-SWAP", "1m", 1703505600000L);
        batchWriter.addCandle(candle);
        
        assertEquals(1, batchWriter.getBufferSize());
        
        batchWriter.flush();
        
        assertEquals(0, batchWriter.getBufferSize());
        verify(candleRepository, times(1)).saveBatch(any());
    }

    @Test
    void shouldNotClearBufferOnFlushError() {
        Candle candle = createCandle("BTC-USDT-SWAP", "1m", 1703505600000L);
        batchWriter.addCandle(candle);
        
        // Simulate database error
        doThrow(new RuntimeException("Database connection failed"))
            .when(candleRepository).saveBatch(any());
        
        batchWriter.flush();
        
        // Buffer should still contain the candle for retry
        assertEquals(1, batchWriter.getBufferSize());
    }

    @Test
    void shouldHandleEmptyBufferFlush() {
        assertEquals(0, batchWriter.getBufferSize());
        
        batchWriter.flush();
        
        verify(candleRepository, never()).saveBatch(any());
    }

    @Test
    void shouldHandleNullCandle() {
        batchWriter.addCandle(null);
        
        assertEquals(0, batchWriter.getBufferSize());
    }

    private Candle createCandle(String symbol, String interval, long timestampMs) {
        Candle candle = new Candle();
        candle.setSymbol(symbol);
        candle.setInterval(interval);
        candle.setTimestamp(Instant.ofEpochMilli(timestampMs));
        candle.setOpen(42000.0);
        candle.setHigh(42100.0);
        candle.setLow(41950.0);
        candle.setClose(42050.0);
        candle.setVolume(1250.8);
        candle.setConfirm("0");
        candle.setCreatedAt(Instant.now());
        return candle;
    }
}
