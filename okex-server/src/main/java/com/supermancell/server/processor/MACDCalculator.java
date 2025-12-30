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
 * MACD (Moving Average Convergence Divergence) Calculator
 * 
 * MACD consists of three components:
 * - MACD Line: 12-period EMA - 26-period EMA
 * - Signal Line: 9-period EMA of MACD Line
 * - Histogram: MACD Line - Signal Line
 * 
 * Standard interpretation:
 * - MACD crosses above signal: Bullish signal (potential buy)
 * - MACD crosses below signal: Bearish signal (potential sell)
 * - Histogram > 0: Bullish momentum
 * - Histogram < 0: Bearish momentum
 */
@Component
public class MACDCalculator implements TechnicalIndicator {
    
    private static final Logger log = LoggerFactory.getLogger(MACDCalculator.class);
    
    private static final String PARAM_FAST_PERIOD = "fastPeriod";
    private static final String PARAM_SLOW_PERIOD = "slowPeriod";
    private static final String PARAM_SIGNAL_PERIOD = "signalPeriod";
    
    @Value("${indicator.macd.default-fast-period:12}")
    private int defaultFastPeriod;
    
    @Value("${indicator.macd.default-slow-period:26}")
    private int defaultSlowPeriod;
    
    @Value("${indicator.macd.default-signal-period:9}")
    private int defaultSignalPeriod;
    
    @Override
    public String getName() {
        return "MACD";
    }
    
    @Override
    public IndicatorResult calculate(List<Candle> candles, IndicatorParams params) {
        // Validate input
        if (candles == null || candles.isEmpty()) {
            log.warn("MACD calculation failed: candles list is null or empty");
            return createErrorResult("Candles list is null or empty");
        }
        
        // Get parameters
        int fastPeriod = defaultFastPeriod;
        int slowPeriod = defaultSlowPeriod;
        int signalPeriod = defaultSignalPeriod;
        
        if (params != null) {
            if (params.hasParameter(PARAM_FAST_PERIOD)) {
                Integer param = params.getParameter(PARAM_FAST_PERIOD, Integer.class);
                if (param != null && param > 0) {
                    fastPeriod = param;
                }
            }
            
            if (params.hasParameter(PARAM_SLOW_PERIOD)) {
                Integer param = params.getParameter(PARAM_SLOW_PERIOD, Integer.class);
                if (param != null && param > 0) {
                    slowPeriod = param;
                }
            }
            
            if (params.hasParameter(PARAM_SIGNAL_PERIOD)) {
                Integer param = params.getParameter(PARAM_SIGNAL_PERIOD, Integer.class);
                if (param != null && param > 0) {
                    signalPeriod = param;
                }
            }
        }
        
        // Validate periods
        if (fastPeriod >= slowPeriod) {
            log.warn("MACD calculation failed: fast period must be less than slow period");
            return createErrorResult("Fast period must be less than slow period");
        }
        
        // Check if we have enough data points
        int minRequired = slowPeriod + signalPeriod;
        if (candles.size() < minRequired) {
            log.warn("MACD calculation failed: insufficient data points. Required: {}, Actual: {}", 
                    minRequired, candles.size());
            return createErrorResult(String.format(
                    "Insufficient data points. Required: %d, Actual: %d", 
                    minRequired, candles.size()));
        }
        
        try {
            // Calculate MACD
            MACDValues macdValues = calculateMACD(candles, fastPeriod, slowPeriod, signalPeriod);
            
            // Create result
            IndicatorResult result = new IndicatorResult();
            result.setValue(macdValues.macdLine); // MACD line as primary value
            
            // Add all MACD values
            Map<String, Double> values = new HashMap<>();
            values.put("macd", macdValues.macdLine);
            values.put("signal", macdValues.signalLine);
            values.put("histogram", macdValues.histogram);
            values.put("fast_period", (double) fastPeriod);
            values.put("slow_period", (double) slowPeriod);
            values.put("signal_period", (double) signalPeriod);
            result.setValues(values);
            
            // Set metadata
            Candle lastCandle = candles.get(candles.size() - 1);
            result.setTimestamp(lastCandle.getTimestamp().toString());
            result.setDataPoints(candles.size());
            
            log.debug("MACD calculation completed: macd={}, signal={}, histogram={}, dataPoints={}", 
                    macdValues.macdLine, macdValues.signalLine, macdValues.histogram, candles.size());
            
            return result;
            
        } catch (Exception e) {
            log.error("MACD calculation error", e);
            return createErrorResult("Calculation error: " + e.getMessage());
        }
    }
    
    /**
     * Calculate MACD values
     */
    private MACDValues calculateMACD(List<Candle> candles, int fastPeriod, int slowPeriod, int signalPeriod) {
        // Calculate fast EMA
        double fastEMA = calculateEMA(candles, fastPeriod);
        
        // Calculate slow EMA
        double slowEMA = calculateEMA(candles, slowPeriod);
        
        // Calculate MACD line (fast EMA - slow EMA)
        double macdLine = fastEMA - slowEMA;
        
        // Calculate signal line (EMA of MACD line)
        // For simplicity, we'll use a simplified signal calculation
        // In production, you'd maintain a history of MACD values
        double signalLine = calculateMACDSignal(candles, fastPeriod, slowPeriod, signalPeriod);
        
        // Calculate histogram (MACD - Signal)
        double histogram = macdLine - signalLine;
        
        // Round values
        MACDValues values = new MACDValues();
        values.macdLine = Math.round(macdLine * 100.0) / 100.0;
        values.signalLine = Math.round(signalLine * 100.0) / 100.0;
        values.histogram = Math.round(histogram * 100.0) / 100.0;
        
        return values;
    }
    
    /**
     * Calculate Exponential Moving Average (EMA)
     * 
     * @param candles List of candles
     * @param period EMA period
     * @return EMA value
     */
    private double calculateEMA(List<Candle> candles, int period) {
        if (candles.size() < period) {
            throw new IllegalArgumentException("Not enough data for EMA calculation");
        }
        
        // Calculate initial SMA
        double sum = 0;
        for (int i = 0; i < period; i++) {
            sum += candles.get(i).getClose();
        }
        double ema = sum / period;
        
        // Calculate multiplier
        double multiplier = 2.0 / (period + 1);
        
        // Calculate EMA for remaining periods
        for (int i = period; i < candles.size(); i++) {
            double closePrice = candles.get(i).getClose();
            ema = ((closePrice - ema) * multiplier) + ema;
        }
        
        return ema;
    }
    
    /**
     * Calculate MACD signal line
     * This is a simplified version that calculates signal based on recent MACD values
     */
    private double calculateMACDSignal(List<Candle> candles, int fastPeriod, int slowPeriod, int signalPeriod) {
        // Calculate MACD values for recent periods
        int requiredPoints = Math.max(slowPeriod + signalPeriod, candles.size());
        int startPoint = Math.max(slowPeriod, candles.size() - requiredPoints);
        
        double[] macdHistory = new double[candles.size() - startPoint];
        
        for (int i = startPoint; i < candles.size(); i++) {
            List<Candle> subset = candles.subList(0, i + 1);
            double fastEMA = calculateEMA(subset, fastPeriod);
            double slowEMA = calculateEMA(subset, slowPeriod);
            macdHistory[i - startPoint] = fastEMA - slowEMA;
        }
        
        // Calculate EMA of MACD values (signal line)
        if (macdHistory.length < signalPeriod) {
            // Not enough history, return simple average
            double sum = 0;
            for (double value : macdHistory) {
                sum += value;
            }
            return sum / macdHistory.length;
        }
        
        // Calculate signal EMA
        double sum = 0;
        for (int i = 0; i < signalPeriod; i++) {
            sum += macdHistory[i];
        }
        double signal = sum / signalPeriod;
        
        double multiplier = 2.0 / (signalPeriod + 1);
        for (int i = signalPeriod; i < macdHistory.length; i++) {
            signal = ((macdHistory[i] - signal) * multiplier) + signal;
        }
        
        return signal;
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
        log.error("MACD Error: {}", errorMessage);
        return result;
    }
    
    /**
     * Internal class to hold MACD values
     */
    private static class MACDValues {
        double macdLine;
        double signalLine;
        double histogram;
    }
}
