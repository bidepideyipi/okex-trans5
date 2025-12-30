package com.supermancell.server.processor;

import com.supermancell.common.model.Candle;
import com.supermancell.common.model.IndicatorParams;
import com.supermancell.common.model.IndicatorResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MACDCalculator
 */
class MACDCalculatorTest {
    
    private MACDCalculator calculator;
    
    @BeforeEach
    void setUp() {
        calculator = new MACDCalculator();
        ReflectionTestUtils.setField(calculator, "defaultFastPeriod", 12);
        ReflectionTestUtils.setField(calculator, "defaultSlowPeriod", 26);
        ReflectionTestUtils.setField(calculator, "defaultSignalPeriod", 9);
    }
    
    @Test
    void testGetName() {
        assertEquals("MACD", calculator.getName());
    }
    
    @Test
    void testCalculateMACD_WithNullCandles() {
        IndicatorParams params = new IndicatorParams();
        params.addParameter("fastPeriod", 12);
        params.addParameter("slowPeriod", 26);
        params.addParameter("signalPeriod", 9);
        
        IndicatorResult result = calculator.calculate(null, params);
        
        assertNotNull(result);
        assertNull(result.getValue());
        assertEquals(-1.0, result.getValues().get("error"));
    }
    
    @Test
    void testCalculateMACD_WithEmptyCandles() {
        List<Candle> candles = new ArrayList<>();
        IndicatorParams params = new IndicatorParams();
        
        IndicatorResult result = calculator.calculate(candles, params);
        
        assertNotNull(result);
        assertNull(result.getValue());
        assertEquals(-1.0, result.getValues().get("error"));
    }
    
    @Test
    void testCalculateMACD_WithInsufficientData() {
        List<Candle> candles = createTestCandles(20, 100.0, 1.0); // Less than slowPeriod + signalPeriod
        IndicatorParams params = new IndicatorParams();
        params.addParameter("fastPeriod", 12);
        params.addParameter("slowPeriod", 26);
        params.addParameter("signalPeriod", 9);
        
        IndicatorResult result = calculator.calculate(candles, params);
        
        assertNotNull(result);
        assertNull(result.getValue());
        assertEquals(-1.0, result.getValues().get("error"));
    }
    
    @Test
    void testCalculateMACD_WithInvalidPeriods() {
        List<Candle> candles = createTestCandles(40, 100.0, 1.0);
        IndicatorParams params = new IndicatorParams();
        params.addParameter("fastPeriod", 26); // Fast >= Slow is invalid
        params.addParameter("slowPeriod", 26);
        params.addParameter("signalPeriod", 9);
        
        IndicatorResult result = calculator.calculate(candles, params);
        
        assertNotNull(result);
        assertNull(result.getValue());
        assertEquals(-1.0, result.getValues().get("error"));
    }
    
    @Test
    void testCalculateMACD_WithSufficientData() {
        List<Candle> candles = createTestCandles(50, 100.0, 1.0);
        IndicatorParams params = new IndicatorParams();
        params.addParameter("fastPeriod", 12);
        params.addParameter("slowPeriod", 26);
        params.addParameter("signalPeriod", 9);
        
        IndicatorResult result = calculator.calculate(candles, params);
        
        assertNotNull(result);
        assertNotNull(result.getValue()); // MACD line
        assertNotNull(result.getValues().get("macd"));
        assertNotNull(result.getValues().get("signal"));
        assertNotNull(result.getValues().get("histogram"));
        
        // Histogram = MACD - Signal
        double macd = result.getValues().get("macd");
        double signal = result.getValues().get("signal");
        double histogram = result.getValues().get("histogram");
        
        assertEquals(macd - signal, histogram, 0.1, "Histogram should equal MACD - Signal");
    }
    
    @Test
    void testCalculateMACD_WithUpwardTrend() {
        // Create upward trending prices
        List<Candle> candles = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            double price = 100.0 + (i * 0.5); // Steady upward trend
            candles.add(createCandle("BTC-USDT-SWAP", price, i));
        }
        
        IndicatorParams params = new IndicatorParams();
        params.addParameter("fastPeriod", 12);
        params.addParameter("slowPeriod", 26);
        params.addParameter("signalPeriod", 9);
        
        IndicatorResult result = calculator.calculate(candles, params);
        
        assertNotNull(result);
        double macd = result.getValues().get("macd");
        double histogram = result.getValues().get("histogram");
        
        // In upward trend, MACD should be positive
        assertTrue(macd > 0, "MACD should be positive in upward trend");
    }
    
    @Test
    void testCalculateMACD_WithDownwardTrend() {
        // Create downward trending prices
        List<Candle> candles = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            double price = 150.0 - (i * 0.5); // Steady downward trend
            candles.add(createCandle("BTC-USDT-SWAP", price, i));
        }
        
        IndicatorParams params = new IndicatorParams();
        params.addParameter("fastPeriod", 12);
        params.addParameter("slowPeriod", 26);
        params.addParameter("signalPeriod", 9);
        
        IndicatorResult result = calculator.calculate(candles, params);
        
        assertNotNull(result);
        double macd = result.getValues().get("macd");
        
        // In downward trend, MACD should be negative
        assertTrue(macd < 0, "MACD should be negative in downward trend");
    }
    
    @Test
    void testCalculateMACD_WithCustomParameters() {
        List<Candle> candles = createTestCandles(50, 100.0, 1.0);
        
        IndicatorParams params = new IndicatorParams();
        params.addParameter("fastPeriod", 8);
        params.addParameter("slowPeriod", 17);
        params.addParameter("signalPeriod", 5);
        
        IndicatorResult result = calculator.calculate(candles, params);
        
        assertNotNull(result);
        assertNotNull(result.getValue());
        assertEquals(8.0, result.getValues().get("fast_period"));
        assertEquals(17.0, result.getValues().get("slow_period"));
        assertEquals(5.0, result.getValues().get("signal_period"));
    }
    
    @Test
    void testCalculateMACD_WithDefaultParameters() {
        List<Candle> candles = createTestCandles(50, 100.0, 1.0);
        
        IndicatorParams params = new IndicatorParams();
        // Don't specify parameters, should use defaults
        
        IndicatorResult result = calculator.calculate(candles, params);
        
        assertNotNull(result);
        assertNotNull(result.getValue());
        assertEquals(12.0, result.getValues().get("fast_period"));
        assertEquals(26.0, result.getValues().get("slow_period"));
        assertEquals(9.0, result.getValues().get("signal_period"));
    }
    
    @Test
    void testCalculateMACD_ResultStructure() {
        List<Candle> candles = createTestCandles(50, 100.0, 1.0);
        
        IndicatorParams params = new IndicatorParams();
        params.addParameter("fastPeriod", 12);
        params.addParameter("slowPeriod", 26);
        params.addParameter("signalPeriod", 9);
        
        IndicatorResult result = calculator.calculate(candles, params);
        
        assertNotNull(result);
        assertNotNull(result.getValue());
        assertNotNull(result.getValues());
        assertTrue(result.getValues().containsKey("macd"));
        assertTrue(result.getValues().containsKey("signal"));
        assertTrue(result.getValues().containsKey("histogram"));
        assertTrue(result.getValues().containsKey("fast_period"));
        assertTrue(result.getValues().containsKey("slow_period"));
        assertTrue(result.getValues().containsKey("signal_period"));
        assertNotNull(result.getTimestamp());
        assertEquals(50, result.getDataPoints());
    }
    
    @Test
    void testCalculateMACD_HistogramSign() {
        List<Candle> candles = createTestCandles(50, 100.0, 1.0);
        
        IndicatorParams params = new IndicatorParams();
        params.addParameter("fastPeriod", 12);
        params.addParameter("slowPeriod", 26);
        params.addParameter("signalPeriod", 9);
        
        IndicatorResult result = calculator.calculate(candles, params);
        
        assertNotNull(result);
        double macd = result.getValues().get("macd");
        double signal = result.getValues().get("signal");
        double histogram = result.getValues().get("histogram");
        
        // Histogram sign should match MACD vs Signal relationship
        if (macd > signal) {
            assertTrue(histogram > 0, "Histogram should be positive when MACD > Signal");
        } else if (macd < signal) {
            assertTrue(histogram < 0, "Histogram should be negative when MACD < Signal");
        }
    }
    
    @Test
    void testCalculateMACD_WithMinimumData() {
        // Test with exactly the minimum required data points
        List<Candle> candles = createTestCandles(35, 100.0, 1.0); // 26 + 9 = 35
        
        IndicatorParams params = new IndicatorParams();
        params.addParameter("fastPeriod", 12);
        params.addParameter("slowPeriod", 26);
        params.addParameter("signalPeriod", 9);
        
        IndicatorResult result = calculator.calculate(candles, params);
        
        assertNotNull(result);
        assertNotNull(result.getValue());
        // Should complete without error
    }
    
    /**
     * Helper method to create test candles
     */
    private List<Candle> createTestCandles(int count, double basePrice, double volatility) {
        List<Candle> candles = new ArrayList<>();
        double currentPrice = basePrice;
        
        for (int i = 0; i < count; i++) {
            double change = (Math.random() - 0.5) * 2.0 * volatility;
            currentPrice += change;
            candles.add(createCandle("BTC-USDT-SWAP", currentPrice, i));
        }
        
        return candles;
    }
    
    /**
     * Helper method to create a single test candle
     */
    private Candle createCandle(String symbol, double price, int minutesAgo) {
        Candle candle = new Candle();
        candle.setSymbol(symbol);
        candle.setTimestamp(Instant.now().minusSeconds(minutesAgo * 60L));
        candle.setInterval("1m");
        candle.setOpen(price);
        candle.setHigh(price + 0.5);
        candle.setLow(price - 0.5);
        candle.setClose(price);
        candle.setVolume(1000.0);
        candle.setConfirm("1");
        return candle;
    }
}
