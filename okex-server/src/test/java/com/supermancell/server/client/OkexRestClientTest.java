package com.supermancell.server.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.supermancell.common.model.Candle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for OkexRestClient
 * Note: These are integration tests that require network access to OKEx API
 * For true unit tests, mock the HTTP connections
 */
class OkexRestClientTest {
    
    private OkexRestClient okexRestClient;
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        okexRestClient = new OkexRestClient(objectMapper);
        
        // Set test configuration
        ReflectionTestUtils.setField(okexRestClient, "okexRestApiUrl", "https://www.okx.com");
        ReflectionTestUtils.setField(okexRestClient, "connectionTimeout", 10000);
    }
    
    @Test
    void testGetCandles_ValidRequest() {
        // Test fetching candles with valid parameters
        String symbol = "BTC-USDT-SWAP";
        String interval = "1m";
        int limit = 10;
        
        List<Candle> candles = okexRestClient.getCandles(symbol, interval, limit);
        
        // Should return some data
        assertNotNull(candles);
        assertTrue(candles.size() > 0, "Should fetch at least some candles");
        
        // Verify candle data
        Candle firstCandle = candles.get(0);
        assertNotNull(firstCandle.getSymbol());
        assertEquals(symbol, firstCandle.getSymbol());
        assertEquals(interval, firstCandle.getInterval());
        assertNotNull(firstCandle.getTimestamp());
        assertTrue(firstCandle.getOpen() > 0);
        assertTrue(firstCandle.getHigh() > 0);
        assertTrue(firstCandle.getLow() > 0);
        assertTrue(firstCandle.getClose() > 0);
        assertTrue(firstCandle.getVolume() >= 0);
        assertNotNull(firstCandle.getConfirm());
    }
    
    @Test
    void testGetCandles_ChronologicalOrder() {
        // Test that candles are returned in chronological order (oldest first)
        String symbol = "ETH-USDT-SWAP";
        String interval = "1H";
        int limit = 5;
        
        List<Candle> candles = okexRestClient.getCandles(symbol, interval, limit);
        
        assertNotNull(candles);
        assertTrue(candles.size() >= 2, "Need at least 2 candles to test order");
        
        // Check chronological order
        for (int i = 1; i < candles.size(); i++) {
            Instant prev = candles.get(i - 1).getTimestamp();
            Instant curr = candles.get(i).getTimestamp();
            assertTrue(prev.isBefore(curr) || prev.equals(curr), 
                    "Candles should be in chronological order");
        }
    }
    
    @Test
    void testGetCandles_LimitExceeds300() {
        // Test that limit is capped at 300
        String symbol = "BTC-USDT-SWAP";
        String interval = "1m";
        int limit = 500; // Exceeds maximum
        
        List<Candle> candles = okexRestClient.getCandles(symbol, interval, limit);
        
        assertNotNull(candles);
        // OKEx will return at most 300 candles
        assertTrue(candles.size() <= 300, "Should not exceed 300 candles");
    }
    
    @Test
    void testGetCandles_InvalidSymbol() {
        // Test with invalid symbol
        String symbol = "INVALID-SYMBOL";
        String interval = "1m";
        int limit = 10;
        
        List<Candle> candles = okexRestClient.getCandles(symbol, interval, limit);
        
        // Should return empty list for invalid symbol
        assertNotNull(candles);
        assertTrue(candles.isEmpty(), "Should return empty list for invalid symbol");
    }
    
    @Test
    void testGetCandles_DifferentIntervals() {
        // Test different interval formats
        String symbol = "BTC-USDT-SWAP";
        String[] intervals = {"1m", "5m", "15m", "1H", "4H"};
        
        for (String interval : intervals) {
            List<Candle> candles = okexRestClient.getCandles(symbol, interval, 5);
            
            assertNotNull(candles, "Should return candles for interval: " + interval);
            if (!candles.isEmpty()) {
                assertEquals(interval, candles.get(0).getInterval(), 
                        "Interval should match for: " + interval);
            }
        }
    }
    
    @Test
    void testGetCandles_ConfirmFlag() {
        // Test that confirm flag is properly parsed
        String symbol = "BTC-USDT-SWAP";
        String interval = "1m";
        int limit = 10;
        
        List<Candle> candles = okexRestClient.getCandles(symbol, interval, limit);
        
        assertNotNull(candles);
        assertTrue(candles.size() > 0);
        
        // Check confirm flag format
        for (Candle candle : candles) {
            String confirm = candle.getConfirm();
            assertNotNull(confirm, "Confirm flag should not be null");
            assertTrue(confirm.equals("0") || confirm.equals("1"), 
                    "Confirm flag should be '0' or '1'");
        }
    }
    
    @Test
    void testGetCandles_PriceValidity() {
        // Test that OHLC prices are logically valid
        String symbol = "BTC-USDT-SWAP";
        String interval = "1H";
        int limit = 10;
        
        List<Candle> candles = okexRestClient.getCandles(symbol, interval, limit);
        
        assertNotNull(candles);
        assertTrue(candles.size() > 0);
        
        for (Candle candle : candles) {
            // High should be >= all other prices
            assertTrue(candle.getHigh() >= candle.getOpen(), 
                    "High should be >= Open");
            assertTrue(candle.getHigh() >= candle.getClose(), 
                    "High should be >= Close");
            assertTrue(candle.getHigh() >= candle.getLow(), 
                    "High should be >= Low");
            
            // Low should be <= all other prices
            assertTrue(candle.getLow() <= candle.getOpen(), 
                    "Low should be <= Open");
            assertTrue(candle.getLow() <= candle.getClose(), 
                    "Low should be <= Close");
            assertTrue(candle.getLow() <= candle.getHigh(), 
                    "Low should be <= High");
        }
    }
}
