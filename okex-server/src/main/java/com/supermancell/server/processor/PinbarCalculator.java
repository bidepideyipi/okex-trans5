package com.supermancell.server.processor;

import com.supermancell.common.indicator.TechnicalIndicator;
import com.supermancell.common.model.Candle;
import com.supermancell.common.model.IndicatorParams;
import com.supermancell.common.model.IndicatorResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pinbar (Pin Bar / Hammer / Shooting Star) Pattern Calculator
 * 
 * A Pinbar is a candlestick pattern with:
 * - A small body (open and close are close together)
 * - A long wick/tail on one side
 * - Minimal wick on the opposite side
 * 
 * Types:
 * - Bullish Pinbar (Hammer): Long lower wick, small body at top
 *   - Suggests potential upward reversal
 * - Bearish Pinbar (Shooting Star): Long upper wick, small body at bottom
 *   - Suggests potential downward reversal
 * 
 * Calculation:
 * - Body Ratio = |Close - Open| / (High - Low)
 * - Upper Wick Ratio = (High - Max(Open, Close)) / (High - Low)
 * - Lower Wick Ratio = (Min(Open, Close) - Low) / (High - Low)
 */
@Component
public class PinbarCalculator implements TechnicalIndicator {
    
    private static final Logger log = LoggerFactory.getLogger(PinbarCalculator.class);
    
    private static final String PARAM_BODY_RATIO_THRESHOLD = "bodyRatioThreshold";
    private static final String PARAM_WICK_RATIO_THRESHOLD = "wickRatioThreshold";
    
    @Value("${indicator.pinbar.default-body-ratio:0.2}")
    private double defaultBodyRatioThreshold;
    
    @Value("${indicator.pinbar.default-wick-ratio:0.6}")
    private double defaultWickRatioThreshold;
    
    @Override
    public String getName() {
        return "PINBAR";
    }
    
    @Override
    public IndicatorResult calculate(List<Candle> candles, IndicatorParams params) {
        // Validate input
        if (candles == null || candles.isEmpty()) {
            log.warn("Pinbar calculation failed: candles list is null or empty");
            return createErrorResult("Candles list is null or empty");
        }
        
        // Get parameters
        double bodyRatioThreshold = defaultBodyRatioThreshold;
        double wickRatioThreshold = defaultWickRatioThreshold;
        
        if (params != null) {
            if (params.hasParameter(PARAM_BODY_RATIO_THRESHOLD)) {
                Double param = params.getParameter(PARAM_BODY_RATIO_THRESHOLD, Double.class);
                if (param != null && param > 0 && param < 1) {
                    bodyRatioThreshold = param;
                }
            }
            
            if (params.hasParameter(PARAM_WICK_RATIO_THRESHOLD)) {
                Double param = params.getParameter(PARAM_WICK_RATIO_THRESHOLD, Double.class);
                if (param != null && param > 0 && param < 1) {
                    wickRatioThreshold = param;
                }
            }
        }
        
        try {
            // Analyze the most recent candle for pinbar pattern
            Candle lastCandle = candles.get(candles.size() - 1);
            PinbarPattern pattern = analyzePinbarPattern(lastCandle, bodyRatioThreshold, wickRatioThreshold);
            
            // Create result
            IndicatorResult result = new IndicatorResult();
            result.setValue(pattern.isPinbar ? 1.0 : 0.0); // 1 if pinbar detected, 0 otherwise
            
            // Add pattern details
            Map<String, Double> values = new HashMap<>();
            values.put("is_pinbar", pattern.isPinbar ? 1.0 : 0.0);
            values.put("is_bullish", pattern.isBullish ? 1.0 : 0.0);
            values.put("is_bearish", pattern.isBearish ? 1.0 : 0.0);
            values.put("body_ratio", pattern.bodyRatio);
            values.put("upper_wick_ratio", pattern.upperWickRatio);
            values.put("lower_wick_ratio", pattern.lowerWickRatio);
            values.put("body_threshold", bodyRatioThreshold);
            values.put("wick_threshold", wickRatioThreshold);
            result.setValues(values);
            
            // Set metadata
            result.setTimestamp(lastCandle.getTimestamp().toString());
            result.setDataPoints(candles.size());
            
            if (pattern.isPinbar) {
                log.debug("Pinbar detected: type={}, body_ratio={}, upper_wick={}, lower_wick={}", 
                        pattern.isBullish ? "BULLISH" : "BEARISH", 
                        pattern.bodyRatio, pattern.upperWickRatio, pattern.lowerWickRatio);
            } else {
                log.debug("No pinbar pattern detected in latest candle");
            }
            
            return result;
            
        } catch (Exception e) {
            log.error("Pinbar calculation error", e);
            return createErrorResult("Calculation error: " + e.getMessage());
        }
    }
    
    /**
     * Analyze a candle for pinbar pattern
     */
    private PinbarPattern analyzePinbarPattern(Candle candle, double bodyRatioThreshold, double wickRatioThreshold) {
        PinbarPattern pattern = new PinbarPattern();
        
        double open = candle.getOpen();
        double close = candle.getClose();
        double high = candle.getHigh();
        double low = candle.getLow();
        
        // Calculate total range
        double totalRange = high - low;
        
        // Avoid division by zero for doji candles
        if (totalRange == 0) {
            pattern.isPinbar = false;
            return pattern;
        }
        
        // Calculate body size and position
        double bodyTop = Math.max(open, close);
        double bodyBottom = Math.min(open, close);
        double bodySize = Math.abs(close - open);
        
        // Calculate ratios
        pattern.bodyRatio = bodySize / totalRange;
        pattern.upperWickRatio = (high - bodyTop) / totalRange;
        pattern.lowerWickRatio = (bodyBottom - low) / totalRange;
        
        // Round ratios
        pattern.bodyRatio = Math.round(pattern.bodyRatio * 10000.0) / 10000.0;
        pattern.upperWickRatio = Math.round(pattern.upperWickRatio * 10000.0) / 10000.0;
        pattern.lowerWickRatio = Math.round(pattern.lowerWickRatio * 10000.0) / 10000.0;
        
        // Check for bullish pinbar (hammer)
        // Requirements:
        // 1. Small body (body ratio < threshold)
        // 2. Long lower wick (lower wick ratio >= threshold)
        // 3. Small or no upper wick
        if (pattern.bodyRatio <= bodyRatioThreshold && 
            pattern.lowerWickRatio >= wickRatioThreshold &&
            pattern.upperWickRatio < bodyRatioThreshold) {
            
            pattern.isPinbar = true;
            pattern.isBullish = true;
            pattern.isBearish = false;
            return pattern;
        }
        
        // Check for bearish pinbar (shooting star)
        // Requirements:
        // 1. Small body (body ratio < threshold)
        // 2. Long upper wick (upper wick ratio >= threshold)
        // 3. Small or no lower wick
        if (pattern.bodyRatio <= bodyRatioThreshold && 
            pattern.upperWickRatio >= wickRatioThreshold &&
            pattern.lowerWickRatio < bodyRatioThreshold) {
            
            pattern.isPinbar = true;
            pattern.isBullish = false;
            pattern.isBearish = true;
            return pattern;
        }
        
        // No pinbar pattern detected
        pattern.isPinbar = false;
        pattern.isBullish = false;
        pattern.isBearish = false;
        
        return pattern;
    }
    
    /**
     * Create an error result
     */
    private IndicatorResult createErrorResult(String errorMessage) {
        IndicatorResult result = new IndicatorResult();
        result.setValue(null);
        Map<String, Double> values = new HashMap<>();
        values.put("error", -1.0);
        result.setValues(values);
        result.setTimestamp(null);
        result.setDataPoints(0);
        log.error("Pinbar Error: {}", errorMessage);
        return result;
    }
    
    /**
     * Internal class to hold Pinbar pattern analysis
     */
    private static class PinbarPattern {
        boolean isPinbar = false;
        boolean isBullish = false;
        boolean isBearish = false;
        double bodyRatio = 0.0;
        double upperWickRatio = 0.0;
        double lowerWickRatio = 0.0;
    }
}
