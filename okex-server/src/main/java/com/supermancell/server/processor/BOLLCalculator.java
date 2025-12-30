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
 * BOLL (Bollinger Bands) Calculator
 * 
 * Bollinger Bands consist of three lines:
 * - Middle Band: Simple Moving Average (SMA) of closing prices
 * - Upper Band: Middle Band + (Standard Deviation × Multiplier)
 * - Lower Band: Middle Band - (Standard Deviation × Multiplier)
 * 
 * Standard interpretation:
 * - Price touching upper band: Potential overbought
 * - Price touching lower band: Potential oversold
 * - Band squeeze: Low volatility (potential breakout coming)
 * - Band expansion: High volatility
 */
@Component
public class BOLLCalculator implements TechnicalIndicator {
    
    private static final Logger log = LoggerFactory.getLogger(BOLLCalculator.class);
    
    private static final String PARAM_PERIOD = "period";
    private static final String PARAM_STD_DEV = "stdDev";
    
    @Value("${indicator.boll.default-period:20}")
    private int defaultPeriod;
    
    @Value("${indicator.boll.default-std-dev:2.0}")
    private double defaultStdDev;
    
    @Override
    public String getName() {
        return "BOLL";
    }
    
    @Override
    public IndicatorResult calculate(List<Candle> candles, IndicatorParams params) {
        // Validate input
        if (candles == null || candles.isEmpty()) {
            log.warn("BOLL calculation failed: candles list is null or empty");
            return createErrorResult("Candles list is null or empty");
        }
        
        // Get parameters
        int period = defaultPeriod;
        double stdDevMultiplier = defaultStdDev;
        
        if (params != null) {
            if (params.hasParameter(PARAM_PERIOD)) {
                Integer paramPeriod = params.getParameter(PARAM_PERIOD, Integer.class);
                if (paramPeriod != null && paramPeriod > 0) {
                    period = paramPeriod;
                }
            }
            
            if (params.hasParameter(PARAM_STD_DEV)) {
                Double paramStdDev = params.getParameter(PARAM_STD_DEV, Double.class);
                if (paramStdDev != null && paramStdDev > 0) {
                    stdDevMultiplier = paramStdDev;
                }
            }
        }
        
        // Check if we have enough data points
        if (candles.size() < period) {
            log.warn("BOLL calculation failed: insufficient data points. Required: {}, Actual: {}", 
                    period, candles.size());
            return createErrorResult(String.format(
                    "Insufficient data points. Required: %d, Actual: %d", 
                    period, candles.size()));
        }
        
        try {
            // Calculate Bollinger Bands
            BollingerBands bands = calculateBollingerBands(candles, period, stdDevMultiplier);
            
            // Create result
            IndicatorResult result = new IndicatorResult();
            result.setValue(bands.middle); // Middle band as primary value
            
            // Add all band values
            Map<String, Double> values = new HashMap<>();
            values.put("upper", bands.upper);
            values.put("middle", bands.middle);
            values.put("lower", bands.lower);
            values.put("bandwidth", bands.bandwidth);
            values.put("percent_b", bands.percentB);
            values.put("period", (double) period);
            values.put("std_dev", stdDevMultiplier);
            result.setValues(values);
            
            // Set metadata
            Candle lastCandle = candles.get(candles.size() - 1);
            result.setTimestamp(lastCandle.getTimestamp().toString());
            result.setDataPoints(candles.size());
            
            log.debug("BOLL calculation completed: upper={}, middle={}, lower={}, period={}, dataPoints={}", 
                    bands.upper, bands.middle, bands.lower, period, candles.size());
            
            return result;
            
        } catch (Exception e) {
            log.error("BOLL calculation error", e);
            return createErrorResult("Calculation error: " + e.getMessage());
        }
    }
    
    /**
     * Calculate Bollinger Bands
     */
    private BollingerBands calculateBollingerBands(List<Candle> candles, int period, double stdDevMultiplier) {
        // Get the most recent 'period' candles
        int startIndex = Math.max(0, candles.size() - period);
        List<Candle> recentCandles = candles.subList(startIndex, candles.size());
        
        // Calculate SMA (Simple Moving Average) - Middle Band
        double sum = 0;
        for (Candle candle : recentCandles) {
            sum += candle.getClose();
        }
        double sma = sum / recentCandles.size();
        
        // Calculate Standard Deviation
        double variance = 0;
        for (Candle candle : recentCandles) {
            double diff = candle.getClose() - sma;
            variance += diff * diff;
        }
        variance /= recentCandles.size();
        double stdDev = Math.sqrt(variance);
        
        // Calculate Upper and Lower Bands
        double upper = sma + (stdDev * stdDevMultiplier);
        double lower = sma - (stdDev * stdDevMultiplier);
        
        // Calculate Bandwidth (volatility measure)
        double bandwidth = ((upper - lower) / sma) * 100.0;
        
        // Calculate %B (position within bands)
        double currentPrice = candles.get(candles.size() - 1).getClose();
        double percentB = (currentPrice - lower) / (upper - lower);
        
        // Round values
        BollingerBands bands = new BollingerBands();
        bands.upper = Math.round(upper * 100.0) / 100.0;
        bands.middle = Math.round(sma * 100.0) / 100.0;
        bands.lower = Math.round(lower * 100.0) / 100.0;
        bands.bandwidth = Math.round(bandwidth * 100.0) / 100.0;
        bands.percentB = Math.round(percentB * 10000.0) / 10000.0;
        
        return bands;
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
        log.error("BOLL Error: {}", errorMessage);
        return result;
    }
    
    /**
     * Internal class to hold Bollinger Bands values
     */
    private static class BollingerBands {
        double upper;
        double middle;
        double lower;
        double bandwidth;
        double percentB;
    }
}
