package com.supermancell.server.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.supermancell.common.model.Candle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CandleCacheService
 */
class CandleCacheServiceTest {
    
    @Mock
    private JedisPool jedisPool;
    
    @Mock
    private Jedis jedis;
    
    private CandleCacheService candleCacheService;
    private ObjectMapper objectMapper;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper();
        candleCacheService = new CandleCacheService(jedisPool, objectMapper);
        
        // Enable Redis by default
        ReflectionTestUtils.setField(candleCacheService, "redisEnabled", true);
        
        // Setup mock to return jedis instance
        when(jedisPool.getResource()).thenReturn(jedis);
    }
    
    @Test
    void testGenerateCacheKey() {
        String key = candleCacheService.generateCacheKey("BTC-USDT-SWAP", "1m", 100);
        assertEquals("candle:integrity:BTC-USDT-SWAP:1m:100", key);
        
        key = candleCacheService.generateCacheKey("ETH-USDT-SWAP", "1H", 50);
        assertEquals("candle:integrity:ETH-USDT-SWAP:1H:50", key);
    }
    
    @Test
    void testCacheCandles_Success() throws Exception {
        List<Candle> candles = createTestCandles(10);
        int expireSeconds = 60;
        
        candleCacheService.cacheCandles("BTC-USDT-SWAP", "1m", 100, candles, expireSeconds);
        
        verify(jedis, times(1)).setex(
                eq("candle:integrity:BTC-USDT-SWAP:1m:100"),
                eq(expireSeconds),
                anyString()
        );
        verify(jedis, times(1)).close();
    }
    
    @Test
    void testCacheCandles_WithZeroExpiration() {
        List<Candle> candles = createTestCandles(10);
        
        // Should not cache when expireSeconds is 0
        candleCacheService.cacheCandles("BTC-USDT-SWAP", "1m", 100, candles, 0);
        
        verify(jedis, never()).setex(anyString(), anyInt(), anyString());
    }
    
    @Test
    void testCacheCandles_WithNegativeExpiration() {
        List<Candle> candles = createTestCandles(10);
        
        // Should not cache when expireSeconds is negative
        candleCacheService.cacheCandles("BTC-USDT-SWAP", "1m", 100, candles, -1);
        
        verify(jedis, never()).setex(anyString(), anyInt(), anyString());
    }
    
    @Test
    void testCacheCandles_WithNullCandles() {
        candleCacheService.cacheCandles("BTC-USDT-SWAP", "1m", 100, null, 60);
        
        verify(jedis, never()).setex(anyString(), anyInt(), anyString());
    }
    
    @Test
    void testCacheCandles_WithEmptyCandles() {
        candleCacheService.cacheCandles("BTC-USDT-SWAP", "1m", 100, new ArrayList<>(), 60);
        
        verify(jedis, never()).setex(anyString(), anyInt(), anyString());
    }
    
    @Test
    void testGetCachedCandles_CacheHit() throws Exception {
        List<Candle> candles = createTestCandles(10);
        String candlesJson = objectMapper.writeValueAsString(candles);
        
        when(jedis.get("candle:integrity:BTC-USDT-SWAP:1m:100")).thenReturn(candlesJson);
        
        List<Candle> result = candleCacheService.getCachedCandles("BTC-USDT-SWAP", "1m", 100);
        
        assertNotNull(result);
        assertEquals(10, result.size());
        verify(jedis, times(1)).get("candle:integrity:BTC-USDT-SWAP:1m:100");
        verify(jedis, times(1)).close();
    }
    
    @Test
    void testGetCachedCandles_CacheMiss() {
        when(jedis.get(anyString())).thenReturn(null);
        
        List<Candle> result = candleCacheService.getCachedCandles("BTC-USDT-SWAP", "1m", 100);
        
        assertNull(result);
        verify(jedis, times(1)).get("candle:integrity:BTC-USDT-SWAP:1m:100");
    }
    
    @Test
    void testGetCachedCandles_RedisDisabled() {
        ReflectionTestUtils.setField(candleCacheService, "redisEnabled", false);
        
        List<Candle> result = candleCacheService.getCachedCandles("BTC-USDT-SWAP", "1m", 100);
        
        assertNull(result);
        verify(jedis, never()).get(anyString());
    }
    
    @Test
    void testExistsInCache_True() {
        when(jedis.exists("candle:integrity:BTC-USDT-SWAP:1m:100")).thenReturn(true);
        
        boolean exists = candleCacheService.existsInCache("BTC-USDT-SWAP", "1m", 100);
        
        assertTrue(exists);
        verify(jedis, times(1)).exists("candle:integrity:BTC-USDT-SWAP:1m:100");
    }
    
    @Test
    void testExistsInCache_False() {
        when(jedis.exists(anyString())).thenReturn(false);
        
        boolean exists = candleCacheService.existsInCache("BTC-USDT-SWAP", "1m", 100);
        
        assertFalse(exists);
    }
    
    @Test
    void testInvalidateCache() {
        when(jedis.del(anyString())).thenReturn(1L);
        
        candleCacheService.invalidateCache("BTC-USDT-SWAP", "1m", 100);
        
        verify(jedis, times(1)).del("candle:integrity:BTC-USDT-SWAP:1m:100");
        verify(jedis, times(1)).close();
    }
    
    @Test
    void testGetCacheTTL_KeyExists() {
        when(jedis.ttl("candle:integrity:BTC-USDT-SWAP:1m:100")).thenReturn(45L);
        
        long ttl = candleCacheService.getCacheTTL("BTC-USDT-SWAP", "1m", 100);
        
        assertEquals(45L, ttl);
        verify(jedis, times(1)).ttl("candle:integrity:BTC-USDT-SWAP:1m:100");
    }
    
    @Test
    void testGetCacheTTL_KeyDoesNotExist() {
        when(jedis.ttl(anyString())).thenReturn(-1L);
        
        long ttl = candleCacheService.getCacheTTL("BTC-USDT-SWAP", "1m", 100);
        
        assertEquals(-1L, ttl);
    }
    
    @Test
    void testGetCacheTTL_RedisDisabled() {
        ReflectionTestUtils.setField(candleCacheService, "redisEnabled", false);
        
        long ttl = candleCacheService.getCacheTTL("BTC-USDT-SWAP", "1m", 100);
        
        assertEquals(-1L, ttl);
        verify(jedis, never()).ttl(anyString());
    }
    
    // Helper method to create test candles
    private List<Candle> createTestCandles(int count) {
        List<Candle> candles = new ArrayList<>();
        Instant baseTime = Instant.now();
        
        for (int i = 0; i < count; i++) {
            Candle candle = new Candle();
            candle.setSymbol("BTC-USDT-SWAP");
            candle.setInterval("1m");
            candle.setTimestamp(baseTime.plusSeconds(i * 60));
            candle.setOpen(40000.0 + i);
            candle.setHigh(40100.0 + i);
            candle.setLow(39900.0 + i);
            candle.setClose(40050.0 + i);
            candle.setVolume(1000.0);
            candle.setConfirm("1");
            candle.setCreatedAt(Instant.now());
            candles.add(candle);
        }
        
        return candles;
    }
}
