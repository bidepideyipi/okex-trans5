package com.supermancell.server.service;

import com.supermancell.common.indicator.TechnicalIndicator;
import com.supermancell.common.model.Candle;
import com.supermancell.common.model.IndicatorParams;
import com.supermancell.common.model.IndicatorResult;
import com.supermancell.server.cache.CandleCacheService;
import com.supermancell.server.processor.BOLLCalculator;
import com.supermancell.server.processor.MACDCalculator;
import com.supermancell.server.processor.PinbarCalculator;
import com.supermancell.server.processor.RSICalculator;
import com.supermancell.server.repository.CandleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CalculationEngine - Strategy pattern coordinator for technical indicator calculations
 * 
 * This service:
 * 1. Provides a unified interface for all technical indicator calculations
 * 2. Manages calculator instances using strategy pattern
 * 3. Integrates with CandleRepository (which is intercepted by AOP for data integrity)
 * 4. Supports caching of calculation results in Redis
 * 5. Handles parameter validation and error handling
 * 
 * Flow:
 * Client Request → CalculationEngine → CandleRepository.findCandles() 
 * → [AOP Aspect intercepts] → Data Integrity Check → Redis Cache Check 
 * → MongoDB Query → Return Validated Data → Calculator → Result
 */
@Service
public class CalculationEngine {
    
    private static final Logger log = LoggerFactory.getLogger(CalculationEngine.class);
    
    private final CandleRepository candleRepository;
    private final CandleCacheService candleCacheService;
    private final Map<IndicatorType, TechnicalIndicator> calculators;
    
    /**
     * Constructor with dependency injection
     */
    public CalculationEngine(
            CandleRepository candleRepository,
            CandleCacheService candleCacheService,
            RSICalculator rsiCalculator,
            BOLLCalculator bollCalculator,
            MACDCalculator macdCalculator,
            PinbarCalculator pinbarCalculator) {
        
        this.candleRepository = candleRepository;
        this.candleCacheService = candleCacheService;
        
        // Initialize calculator registry using strategy pattern
        this.calculators = new HashMap<>();
        this.calculators.put(IndicatorType.RSI, rsiCalculator);
        this.calculators.put(IndicatorType.BOLL, bollCalculator);
        this.calculators.put(IndicatorType.MACD, macdCalculator);
        this.calculators.put(IndicatorType.PINBAR, pinbarCalculator);
        
        log.info("CalculationEngine initialized with {} calculators", calculators.size());
    }
    
    /**
     * Calculate RSI indicator
     * 
     * @param symbol Trading symbol (e.g., BTC-USDT-SWAP)
     * @param interval Time interval (e.g., 1m, 1H)
     * @param period RSI period (typically 14)
     * @param limit Number of candles to fetch
     * @return IndicatorResult containing RSI value
     */
    public IndicatorResult calculateRSI(String symbol, String interval, int period, int limit) {
        log.debug("Calculating RSI: symbol={}, interval={}, period={}, limit={}", 
                symbol, interval, period, limit);
        
        IndicatorParams params = new IndicatorParams();
        params.addParameter("period", period);
        
        return calculate(IndicatorType.RSI, symbol, interval, limit, params);
    }
    
    /**
     * Calculate Bollinger Bands
     * 
     * @param symbol Trading symbol
     * @param interval Time interval
     * @param period BOLL period (typically 20)
     * @param stdDev Standard deviation multiplier (typically 2.0)
     * @param limit Number of candles to fetch
     * @return IndicatorResult containing upper, middle, lower bands
     */
    public IndicatorResult calculateBOLL(String symbol, String interval, int period, double stdDev, int limit) {
        log.debug("Calculating BOLL: symbol={}, interval={}, period={}, stdDev={}, limit={}", 
                symbol, interval, period, stdDev, limit);
        
        IndicatorParams params = new IndicatorParams();
        params.addParameter("period", period);
        params.addParameter("stdDev", stdDev);
        
        return calculate(IndicatorType.BOLL, symbol, interval, limit, params);
    }
    
    /**
     * Calculate MACD indicator
     * 
     * @param symbol Trading symbol
     * @param interval Time interval
     * @param fastPeriod Fast EMA period (typically 12)
     * @param slowPeriod Slow EMA period (typically 26)
     * @param signalPeriod Signal line period (typically 9)
     * @param limit Number of candles to fetch
     * @return IndicatorResult containing MACD line, signal line, histogram
     */
    public IndicatorResult calculateMACD(String symbol, String interval, 
                                         int fastPeriod, int slowPeriod, int signalPeriod, int limit) {
        log.debug("Calculating MACD: symbol={}, interval={}, fast={}, slow={}, signal={}, limit={}", 
                symbol, interval, fastPeriod, slowPeriod, signalPeriod, limit);
        
        IndicatorParams params = new IndicatorParams();
        params.addParameter("fastPeriod", fastPeriod);
        params.addParameter("slowPeriod", slowPeriod);
        params.addParameter("signalPeriod", signalPeriod);
        
        return calculate(IndicatorType.MACD, symbol, interval, limit, params);
    }
    
    /**
     * Detect Pinbar pattern
     * 
     * @param symbol Trading symbol
     * @param interval Time interval
     * @param bodyRatioThreshold Body ratio threshold (typically 0.2)
     * @param wickRatioThreshold Wick ratio threshold (typically 0.6)
     * @param limit Number of candles to fetch
     * @return IndicatorResult indicating if pinbar detected and pattern details
     */
    public IndicatorResult calculatePinbar(String symbol, String interval, 
                                           double bodyRatioThreshold, double wickRatioThreshold, int limit) {
        log.debug("Calculating Pinbar: symbol={}, interval={}, bodyRatio={}, wickRatio={}, limit={}", 
                symbol, interval, bodyRatioThreshold, wickRatioThreshold, limit);
        
        IndicatorParams params = new IndicatorParams();
        params.addParameter("bodyRatioThreshold", bodyRatioThreshold);
        params.addParameter("wickRatioThreshold", wickRatioThreshold);
        
        return calculate(IndicatorType.PINBAR, symbol, interval, limit, params);
    }
    
    /**
     * Generic calculation method using strategy pattern
     * 
     * @param type Indicator type
     * @param symbol Trading symbol
     * @param interval Time interval
     * @param limit Number of candles
     * @param params Calculation parameters
     * @return IndicatorResult
     */
    private IndicatorResult calculate(IndicatorType type, String symbol, String interval, 
                                      int limit, IndicatorParams params) {
        try {
            // Step 1: Get calculator for the indicator type
            TechnicalIndicator calculator = calculators.get(type);
            if (calculator == null) {
                log.error("No calculator found for indicator type: {}", type);
                return createErrorResult("Unsupported indicator type: " + type);
            }
            
            // Step 2: Check Redis cache for indicator result
            String cacheKey = buildIndicatorCacheKey(type, symbol, interval, params);
            IndicatorResult cachedResult = candleCacheService.getIndicatorResult(cacheKey);
            if (cachedResult != null) {
                log.debug("Indicator result cache hit: {}", cacheKey);
                return cachedResult;
            }
            
            // Step 3: Fetch candles from repository
            // Note: This call is intercepted by CandleDataIntegrityAspect
            // The aspect will check Redis cache, validate data, and fetch from OKEx API if needed
            List<Candle> candles = candleRepository.findCandles(symbol, interval, limit);
            
            if (candles == null || candles.isEmpty()) {
                log.warn("No candles available for calculation: symbol={}, interval={}", symbol, interval);
                return createErrorResult("No candle data available");
            }
            
            // Step 4: Calculate indicator
            IndicatorResult result = calculator.calculate(candles, params);
            
            // Step 5: Cache the result if calculation succeeded
            if (result != null && result.getValue() != null) {
                candleCacheService.cacheIndicatorResult(cacheKey, result);
                log.debug("Cached indicator result: {}", cacheKey);
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("Calculation error for {}: symbol={}, interval={}", type, symbol, interval, e);
            return createErrorResult("Calculation error: " + e.getMessage());
        }
    }
    
    /**
     * Build cache key for indicator result
     */
    private String buildIndicatorCacheKey(IndicatorType type, String symbol, String interval, IndicatorParams params) {
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append("indicator:").append(type.name().toLowerCase())
                  .append(":").append(symbol)
                  .append(":").append(interval);
        
        // Add relevant parameters to cache key
        Map<String, Object> allParams = params.getAllParameters();
        for (Map.Entry<String, Object> entry : allParams.entrySet()) {
            keyBuilder.append(":").append(entry.getKey()).append("=").append(entry.getValue());
        }
        
        return keyBuilder.toString();
    }
    
    /**
     * Create error result
     */
    private IndicatorResult createErrorResult(String errorMessage) {
        IndicatorResult result = new IndicatorResult();
        result.setValue(null);
        Map<String, Double> values = new HashMap<>();
        values.put("error", -1.0);
        result.setValues(values);
        result.setTimestamp(null);
        result.setDataPoints(0);
        return result;
    }
    
    /**
     * Indicator type enum
     */
    public enum IndicatorType {
        RSI,
        BOLL,
        MACD,
        PINBAR
    }
}
