package com.supermancell.server.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supermancell.common.model.Candle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.Collections;
import java.util.List;

/**
 * Redis cache service for candle data integrity check
 * Caches validated candle data to reduce repeated database queries and validation overhead
 */
@Service
public class CandleCacheService {
    
    private static final Logger log = LoggerFactory.getLogger(CandleCacheService.class);
    
    private static final String CACHE_KEY_PREFIX = "candle:integrity:";
    
    private final JedisPool jedisPool;
    private final ObjectMapper objectMapper;
    
    @Value("${redis.enabled:true}")
    private boolean redisEnabled;
    
    public CandleCacheService(JedisPool jedisPool, ObjectMapper objectMapper) {
        this.jedisPool = jedisPool;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Generate cache key for candle data
     * Format: candle:integrity:{symbol}:{interval}:{limit}
     * 
     * @param symbol Trading pair (e.g., BTC-USDT-SWAP)
     * @param interval Time interval (e.g., 1m, 1H)
     * @param limit Number of candles
     * @return Cache key string
     */
    public String generateCacheKey(String symbol, String interval, int limit) {
        return String.format("%s%s:%s:%d", CACHE_KEY_PREFIX, symbol, interval, limit);
    }
    
    /**
     * Get cached candle data from Redis
     * 
     * @param symbol Trading pair
     * @param interval Time interval
     * @param limit Number of candles
     * @return List of cached candles, or null if cache miss
     */
    public List<Candle> getCachedCandles(String symbol, String interval, int limit) {
        if (!redisEnabled) {
            log.debug("Redis is disabled, skipping cache lookup");
            return null;
        }
        
        String cacheKey = generateCacheKey(symbol, interval, limit);
        
        try (Jedis jedis = jedisPool.getResource()) {
            String cachedJson = jedis.get(cacheKey);
            
            if (cachedJson == null) {
                log.debug("Cache miss for key: {}", cacheKey);
                return null;
            }
            
            List<Candle> candles = objectMapper.readValue(
                    cachedJson,
                    new TypeReference<List<Candle>>() {}
            );
            
            log.info("Cache hit for key: {}, returned {} candles", cacheKey, candles.size());
            return candles;
            
        } catch (Exception e) {
            log.error("Failed to get cached candles for key: {}", cacheKey, e);
            return null;
        }
    }
    
    /**
     * Save validated candle data to Redis with TTL
     * 
     * @param symbol Trading pair
     * @param interval Time interval
     * @param limit Number of candles
     * @param candles List of validated candles
     * @param expireSeconds Cache expiration time in seconds (0 = don't cache)
     */
    public void cacheCandles(String symbol, String interval, int limit, 
                            List<Candle> candles, int expireSeconds) {
        if (!redisEnabled) {
            log.debug("Redis is disabled, skipping cache save");
            return;
        }
        
        if (expireSeconds <= 0) {
            log.debug("Cache expiration is {} seconds, skipping cache save", expireSeconds);
            return;
        }
        
        if (candles == null || candles.isEmpty()) {
            log.warn("Cannot cache null or empty candle list");
            return;
        }
        
        String cacheKey = generateCacheKey(symbol, interval, limit);
        
        try (Jedis jedis = jedisPool.getResource()) {
            String candlesJson = objectMapper.writeValueAsString(candles);
            
            // Set with expiration
            jedis.setex(cacheKey, expireSeconds, candlesJson);
            
            log.info("Cached {} candles for key: {} with TTL: {}s", 
                    candles.size(), cacheKey, expireSeconds);
            
        } catch (Exception e) {
            log.error("Failed to cache candles for key: {}", cacheKey, e);
        }
    }
    
    /**
     * Check if cache exists for given parameters
     * 
     * @param symbol Trading pair
     * @param interval Time interval
     * @param limit Number of candles
     * @return true if cache exists, false otherwise
     */
    public boolean existsInCache(String symbol, String interval, int limit) {
        if (!redisEnabled) {
            return false;
        }
        
        String cacheKey = generateCacheKey(symbol, interval, limit);
        
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.exists(cacheKey);
        } catch (Exception e) {
            log.error("Failed to check cache existence for key: {}", cacheKey, e);
            return false;
        }
    }
    
    /**
     * Invalidate (delete) cached candle data
     * 
     * @param symbol Trading pair
     * @param interval Time interval
     * @param limit Number of candles
     */
    public void invalidateCache(String symbol, String interval, int limit) {
        if (!redisEnabled) {
            return;
        }
        
        String cacheKey = generateCacheKey(symbol, interval, limit);
        
        try (Jedis jedis = jedisPool.getResource()) {
            Long deleted = jedis.del(cacheKey);
            if (deleted > 0) {
                log.info("Invalidated cache for key: {}", cacheKey);
            }
        } catch (Exception e) {
            log.error("Failed to invalidate cache for key: {}", cacheKey, e);
        }
    }
    
    /**
     * Get remaining TTL for cached data
     * 
     * @param symbol Trading pair
     * @param interval Time interval
     * @param limit Number of candles
     * @return Remaining TTL in seconds, -1 if key doesn't exist, -2 if no expiration
     */
    public long getCacheTTL(String symbol, String interval, int limit) {
        if (!redisEnabled) {
            return -1;
        }
        
        String cacheKey = generateCacheKey(symbol, interval, limit);
        
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.ttl(cacheKey);
        } catch (Exception e) {
            log.error("Failed to get TTL for key: {}", cacheKey, e);
            return -1;
        }
    }
}
