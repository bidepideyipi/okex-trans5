package com.supermancell.server.aspect;

import com.supermancell.common.model.Candle;
import com.supermancell.server.cache.CandleCacheService;
import com.supermancell.server.client.OkexRestClient;
import com.supermancell.server.repository.CandleRepository;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AOP Aspect for checking and ensuring candle data integrity
 * Intercepts technical indicator calculation methods to validate data completeness and continuity
 */
@Aspect
@Component
public class CandleDataIntegrityAspect {
    
    private static final Logger log = LoggerFactory.getLogger(CandleDataIntegrityAspect.class);
    
    private final OkexRestClient okexRestClient;
    private final CandleRepository candleRepository;
    private final CandleCacheService candleCacheService;
    
    @Value("${candle.integrity.check.enabled:true}")
    private boolean integrityCheckEnabled;
    
    @Value("${candle.integrity.check.strict:false}")
    private boolean strictMode;
    
    @Value("${candle.integrity.fetch.limit:300}")
    private int fetchLimit;
    
    @Value("${candle.integrity.cache.expire-seconds:0}")
    private int cacheExpireSeconds;
    
    // Time interval to expected gap in seconds
    private static final Map<String, Long> INTERVAL_GAPS = new HashMap<>();
    
    //系统只会订阅1m和1H两，其它时间维度后期可以通过聚合算法合成
    static {
        INTERVAL_GAPS.put("1m", 60L);
        INTERVAL_GAPS.put("1H", 3600L);
    }
    
    public CandleDataIntegrityAspect(OkexRestClient okexRestClient, 
                                     CandleRepository candleRepository,
                                     CandleCacheService candleCacheService) {
        this.okexRestClient = okexRestClient;
        this.candleRepository = candleRepository;
        this.candleCacheService = candleCacheService;
    }
    
    /**
     * Intercept methods that query candles from repository
     * Pointcut: CandleRepository.findCandles(String symbol, String interval, int limit)
     */
    @Around("execution(* com.supermancell.server.repository.CandleRepository.findCandles(..))")
    public Object checkCandleDataIntegrity(ProceedingJoinPoint joinPoint) throws Throwable {
        
        if (!integrityCheckEnabled) {
            log.debug("Candle integrity check is disabled, proceeding with original query");
            return joinPoint.proceed();
        }
        
        // Extract method arguments
        Object[] args = joinPoint.getArgs();
        String symbol = (String) args[0];
        String interval = (String) args[1];
        int limit = (int) args[2];
        
        log.debug("Checking candle data integrity for symbol={}, interval={}, limit={}", 
                symbol, interval, limit);
        
        // Step 1: Check Redis cache first
        List<Candle> cachedCandles = candleCacheService.getCachedCandles(symbol, interval, limit);
        if (cachedCandles != null && !cachedCandles.isEmpty()) {
            log.info("Returning {} candles from Redis cache (TTL: {}s)", 
                    cachedCandles.size(), 
                    candleCacheService.getCacheTTL(symbol, interval, limit));
            return cachedCandles;
        }
        
        // Step 2: Cache miss - query MongoDB
        @SuppressWarnings("unchecked")
        List<Candle> candles = (List<Candle>) joinPoint.proceed();
        
        // Step 3: Validate data completeness and continuity
        boolean isComplete = checkDataCompleteness(candles, limit);
        boolean isContinuous = checkTimeContinuity(candles, interval);
        
        if (isComplete && isContinuous) {
            log.debug("Candle data is complete and continuous");
            
            // Step 4: Cache validated data if caching is enabled (N > 0)
            if (cacheExpireSeconds > 0) {
                candleCacheService.cacheCandles(symbol, interval, limit, candles, cacheExpireSeconds);
            }
            
            return candles;
        }
        
        // Data is incomplete or discontinuous
        log.warn("Candle data integrity issue detected - complete: {}, continuous: {}", 
                isComplete, isContinuous);
        
        if (strictMode) {
            log.info("Strict mode enabled, fetching complete data from OKEx REST API");
            List<Candle> completeCandles = fetchCompleteDataFromApi(symbol, interval, limit);
            
            // Cache the complete data fetched from API
            if (!completeCandles.isEmpty() && cacheExpireSeconds > 0) {
                candleCacheService.cacheCandles(symbol, interval, limit, completeCandles, cacheExpireSeconds);
            }
            
            return completeCandles;
        } else {
            log.warn("Non-strict mode, returning potentially incomplete data");
            return candles;
        }
    }
    
    /**
     * Check if data quantity meets requirements
     * 
     * @param candles List of candles from database
     * @param requiredLimit Required number of candles
     * @return true if data is complete, false otherwise
     */
    private boolean checkDataCompleteness(List<Candle> candles, int requiredLimit) {
        if (candles == null || candles.isEmpty()) {
            log.warn("No candle data available");
            return false;
        }
        
        int actualCount = candles.size();
        
        // Allow some tolerance for very recent data
        // Consider complete if we have at least 90% of required data
        int minAcceptable = (int) (requiredLimit * 0.9);
        
        if (actualCount < minAcceptable) {
            log.warn("Insufficient candle data: required={}, actual={}, min_acceptable={}", 
                    requiredLimit, actualCount, minAcceptable);
            return false;
        }
        
        log.debug("Data completeness check passed: required={}, actual={}", 
                requiredLimit, actualCount);
        return true;
    }
    
    /**
     * Check if timestamps are continuous based on interval
     * 
     * @param candles List of candles to check
     * @param interval Time interval (e.g., 1m, 1H)
     * @return true if timestamps are continuous, false otherwise
     */
    private boolean checkTimeContinuity(List<Candle> candles, String interval) {
        if (candles == null || candles.size() < 2) {
            // Cannot check continuity with less than 2 candles
            return true;
        }
        
        Long expectedGapSeconds = INTERVAL_GAPS.get(interval);
        if (expectedGapSeconds == null) {
            log.warn("Unknown interval: {}, skipping continuity check", interval);
            return true;
        }
        
        // Check gaps between consecutive candles
        int gapCount = 0;
        int totalChecks = 0;
        
        for (int i = 1; i < candles.size(); i++) {
            Candle prev = candles.get(i - 1);
            Candle curr = candles.get(i);
            
            if (prev.getTimestamp() == null || curr.getTimestamp() == null) {
                continue;
            }
            
            long actualGapSeconds = Duration.between(prev.getTimestamp(), curr.getTimestamp()).getSeconds();
            
            // Allow some tolerance (±10 seconds) for timestamp variations
            long tolerance = 10L;
            long minGap = expectedGapSeconds - tolerance;
            long maxGap = expectedGapSeconds + tolerance;
            
            if (actualGapSeconds < minGap || actualGapSeconds > maxGap) {
                log.debug("Time gap anomaly detected: expected={}s, actual={}s, between {} and {}", 
                        expectedGapSeconds, actualGapSeconds, prev.getTimestamp(), curr.getTimestamp());
                gapCount++;
            }
            
            totalChecks++;
        }
        
        // Allow up to 5% gaps for real-world scenarios
        double gapRatio = totalChecks > 0 ? (double) gapCount / totalChecks : 0;
        boolean isContinuous = gapRatio <= 0.05;
        
        if (!isContinuous) {
            log.warn("Time continuity check failed: gaps={}/{} ({:.2f}%)", 
                    gapCount, totalChecks, gapRatio * 100);
        } else {
            log.debug("Time continuity check passed: gaps={}/{}", gapCount, totalChecks);
        }
        
        return isContinuous;
    }
    
    /**
     * Fetch complete data from OKEx REST API and update MongoDB
     * 
     * @param symbol Trading pair
     * @param interval Time interval
     * @param limit Number of candles needed
     * @return Complete list of candles
     */
    private List<Candle> fetchCompleteDataFromApi(String symbol, String interval, int limit) {
        log.info("Fetching complete candle data from OKEx REST API: symbol={}, interval={}, limit={}", 
                symbol, interval, limit);
        
        try {
            // Fetch from REST API (max 300)
            int fetchCount = Math.min(limit, fetchLimit);
            List<Candle> completeCandles = okexRestClient.getCandles(symbol, interval, fetchCount);
            
            if (completeCandles.isEmpty()) {
                log.error("Failed to fetch candles from REST API");
                return completeCandles;
            }
            
            log.info("Fetched {} candles from REST API, updating MongoDB", completeCandles.size());
            
            // Update MongoDB with complete data
            candleRepository.saveBatch(completeCandles);
            
            log.info("Successfully updated MongoDB with complete candle data");
            
            return completeCandles;
            
        } catch (Exception e) {
            log.error("Failed to fetch complete data from REST API", e);
            return java.util.Collections.emptyList();
        }
    }
}
