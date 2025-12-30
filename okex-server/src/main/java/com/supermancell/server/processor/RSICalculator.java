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
 * RSI (Relative Strength Index) Calculator
 * 
 * RSI measures the magnitude of recent price changes to evaluate overbought or oversold conditions.
 * Formula: RSI = 100 - (100 / (1 + RS))
 * where RS = Average Gain / Average Loss over the period
 * 
 * Standard interpretation:
 * - RSI > 70: Overbought condition
 * - RSI < 30: Oversold condition
 * - RSI = 50: Neutral
 */
@Component
public class RSICalculator implements TechnicalIndicator {
    
    private static final Logger log = LoggerFactory.getLogger(RSICalculator.class);
    
    private static final String PARAM_PERIOD = "period";
    private static final int MIN_DATA_POINTS = 2;
    
    @Value("${indicator.rsi.default-period:14}")
    private int defaultPeriod;
    
    @Override
    public String getName() {
        return "RSI";
    }
    
    @Override
    public IndicatorResult calculate(List<Candle> candles, IndicatorParams params) {
        // Validate input
        if (candles == null || candles.isEmpty()) {
            log.warn("RSI calculation failed: candles list is null or empty");
            return createErrorResult("Candles list is null or empty");
        }
        
        // Get period parameter
        int period = defaultPeriod;
        if (params != null && params.hasParameter(PARAM_PERIOD)) {
            Integer paramPeriod = params.getParameter(PARAM_PERIOD, Integer.class);
            if (paramPeriod != null && paramPeriod > 0) {
                period = paramPeriod;
            }
        }
        
        // Check if we have enough data points
        if (candles.size() < period + 1) {
            log.warn("RSI calculation failed: insufficient data points. Required: {}, Actual: {}", 
                    period + 1, candles.size());
            return createErrorResult(String.format(
                    "Insufficient data points. Required: %d, Actual: %d", 
                    period + 1, candles.size()));
        }
        
        try {
            // Calculate RSI
            double rsiValue = calculateRSI(candles, period);
            
            // Create result
            IndicatorResult result = new IndicatorResult();
            result.setValue(rsiValue);
            
            // Add additional values
            Map<String, Double> values = new HashMap<>();
            values.put("rsi", rsiValue);
            values.put("period", (double) period);
            result.setValues(values);
            
            // Set metadata
            Candle lastCandle = candles.get(candles.size() - 1);
            result.setTimestamp(lastCandle.getTimestamp().toString());
            result.setDataPoints(candles.size());
            
            log.debug("RSI calculation completed: value={}, period={}, dataPoints={}", 
                    rsiValue, period, candles.size());
            
            return result;
            
        } catch (Exception e) {
            log.error("RSI calculation error", e);
            return createErrorResult("Calculation error: " + e.getMessage());
        }
    }
    
    /**
     * Calculate RSI using Wilder's smoothing method
     * 
     * @param candles List of candles (must be sorted by timestamp ascending)
     * @param period RSI period (typically 14)
     * @return RSI value (0-100)
     */
    private double calculateRSI(List<Candle> candles, int period) {
        // Calculate price changes
        double[] changes = new double[candles.size() - 1];
        for (int i = 1; i < candles.size(); i++) {
            changes[i - 1] = candles.get(i).getClose() - candles.get(i - 1).getClose();
        }
        
        // Separate gains and losses
        double[] gains = new double[changes.length];
        double[] losses = new double[changes.length];
        
        for (int i = 0; i < changes.length; i++) {
            if (changes[i] > 0) {
                gains[i] = changes[i];
                losses[i] = 0;
            } else {
                gains[i] = 0;
                losses[i] = Math.abs(changes[i]);
            }
        }
        
        // Calculate initial average gain and loss (simple average for first period)
        double avgGain = 0;
        double avgLoss = 0;
        
        for (int i = 0; i < period; i++) {
            avgGain += gains[i];
            avgLoss += losses[i];
        }
        
        avgGain /= period;
        avgLoss /= period;
        
        // Apply Wilder's smoothing for subsequent periods
        for (int i = period; i < changes.length; i++) {
            avgGain = ((avgGain * (period - 1)) + gains[i]) / period;
            avgLoss = ((avgLoss * (period - 1)) + losses[i]) / period;
        }
        
        // Calculate RS and RSI
        if (avgLoss == 0) {
            // No losses means RSI = 100
            return 100.0;
        }
        
        double rs = avgGain / avgLoss;
        double rsi = 100.0 - (100.0 / (1.0 + rs));
        
        // Round to 2 decimal places
        return Math.round(rsi * 100.0) / 100.0;
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
        log.error("RSI Error: {}", errorMessage);
        return result;
    }
}
