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
 * Unit tests for BOLLCalculator (Bollinger Bands)
 */
class BOLLCalculatorTest {
    
    private BOLLCalculator calculator;
    
    @BeforeEach
    void setUp() {
        calculator = new BOLLCalculator();
        ReflectionTestUtils.setField(calculator, "defaultPeriod", 20);
        ReflectionTestUtils.setField(calculator, "defaultStdDev", 2.0);
    }
    
    @Test
    void testGetName() {
        assertEquals("BOLL", calculator.getName());
    }
    
    @Test
    void testCalculateBOLL_WithNullCandles() {
        IndicatorParams params = new IndicatorParams();
        params.addParameter("period", 20);
        params.addParameter("stdDev", 2.0);
        
        IndicatorResult result = calculator.calculate(null, params);
        
        assertNotNull(result);
        assertNull(result.getValue());
        assertEquals(-1.0, result.getValues().get("error"));
    }
    
    @Test
    void testCalculateBOLL_WithEmptyCandles() {
        List<Candle> candles = new ArrayList<>();
        IndicatorParams params = new IndicatorParams();
        params.addParameter("period", 20);
        
        IndicatorResult result = calculator.calculate(candles, params);
        
        assertNotNull(result);
        assertNull(result.getValue());
        assertEquals(-1.0, result.getValues().get("error"));
    }

    /**
     *  测试期望数量不一致的情况
     */
    @Test
    void testCalculateBOLL_WithInsufficientData() {
        List<Candle> candles = createTestCandles(15, 100.0, 0.5); // Less than period
        IndicatorParams params = new IndicatorParams();
        params.addParameter("period", 20);
        
        IndicatorResult result = calculator.calculate(candles, params);
        
        assertNotNull(result);
        assertNull(result.getValue());
        assertEquals(-1.0, result.getValues().get("error"));
    }
    
    @Test
    void testCalculateBOLL_WithSufficientData() {
        List<Candle> candles = createTestCandles(30, 100.0, 1.0);
        IndicatorParams params = new IndicatorParams();
        params.addParameter("period", 20);
        params.addParameter("stdDev", 2.0);
        
        IndicatorResult result = calculator.calculate(candles, params);
        
        assertNotNull(result);
        assertNotNull(result.getValue()); // Middle band
        assertNotNull(result.getValues().get("upper"));
        assertNotNull(result.getValues().get("middle"));
        assertNotNull(result.getValues().get("lower"));
        assertNotNull(result.getValues().get("bandwidth"));
        assertNotNull(result.getValues().get("percent_b"));
        
        // Upper band should be greater than middle, middle greater than lower
        double upper = result.getValues().get("upper");
        double middle = result.getValues().get("middle");
        double lower = result.getValues().get("lower");
        
        assertTrue(upper > middle, "Upper band should be > middle band");
        assertTrue(middle > lower, "Middle band should be > lower band");
    }

    /**
     *  计算中轨的SMA
     */
    @Test
    void testCalculateBOLL_MiddleBandIsSMA() {
        // Create candles with known values to verify SMA calculation
        List<Candle> candles = new ArrayList<>();
        double[] prices = {100, 102, 101, 103, 104, 105, 104, 106, 107, 108,
                          109, 110, 111, 112, 113, 114, 115, 116, 117, 118};
        
        for (int i = 0; i < prices.length; i++) {
            candles.add(createCandle("BTC-USDT-SWAP", prices[i], i));
        }
        
        IndicatorParams params = new IndicatorParams();
        params.addParameter("period", 20);
        params.addParameter("stdDev", 2.0);
        
        IndicatorResult result = calculator.calculate(candles, params);
        
        assertNotNull(result);
        double middle = result.getValues().get("middle");
        
        // Calculate expected SMA
        double sum = 0;
        for (double price : prices) {
            sum += price;
        }
        double expectedSMA = sum / prices.length;
        
        assertEquals(expectedSMA, middle, 0.1, "Middle band should equal SMA");
    }
    
    @Test
    void testCalculateBOLL_WithLowVolatility() {
        // Create candles with very small price changes (low volatility)
        List<Candle> candles = createTestCandles(25, 100.0, 0.1);
        
        IndicatorParams params = new IndicatorParams();
        params.addParameter("period", 20);
        params.addParameter("stdDev", 2.0);
        
        IndicatorResult result = calculator.calculate(candles, params);
        
        assertNotNull(result);
        double upper = result.getValues().get("upper");
        double middle = result.getValues().get("middle");
        double lower = result.getValues().get("lower");
        double bandwidth = result.getValues().get("bandwidth");
        
        // With low volatility, bands should be close together
        double bandWidth = upper - lower;
        assertTrue(bandWidth < 2.0, "Band width should be small for low volatility");
        assertTrue(bandwidth < 2.0, "Bandwidth percentage should be small for low volatility");
    }
    
    @Test
    void testCalculateBOLL_WithHighVolatility() {
        // Create candles with large price changes (high volatility)
        List<Candle> candles = createTestCandles(25, 100.0, 5.0);
        
        IndicatorParams params = new IndicatorParams();
        params.addParameter("period", 20);
        params.addParameter("stdDev", 2.0);
        
        IndicatorResult result = calculator.calculate(candles, params);
        
        assertNotNull(result);
        double upper = result.getValues().get("upper");
        double lower = result.getValues().get("lower");
        double bandwidth = result.getValues().get("bandwidth");
        
        // With high volatility, bands should be far apart
        double bandWidth = upper - lower;
        assertTrue(bandWidth > 5.0, "Band width should be large for high volatility");
        assertTrue(bandwidth > 2.0, "Bandwidth percentage should be large for high volatility");
    }
    
    @Test
    void testCalculateBOLL_WithCustomParameters() {
        List<Candle> candles = createTestCandles(30, 100.0, 1.0);
        
        IndicatorParams params = new IndicatorParams();
        params.addParameter("period", 10); // Custom period
        params.addParameter("stdDev", 1.5); // Custom std dev multiplier
        
        IndicatorResult result = calculator.calculate(candles, params);
        
        assertNotNull(result);
        assertNotNull(result.getValue());
        assertEquals(10.0, result.getValues().get("period"));
        assertEquals(1.5, result.getValues().get("std_dev"));
    }
    
    @Test
    void testCalculateBOLL_WithDefaultParameters() {
        List<Candle> candles = createTestCandles(25, 100.0, 1.0);
        
        IndicatorParams params = new IndicatorParams();
        // Don't specify parameters, should use defaults
        
        IndicatorResult result = calculator.calculate(candles, params);
        
        assertNotNull(result);
        assertNotNull(result.getValue());
        assertEquals(20.0, result.getValues().get("period"));
        assertEquals(2.0, result.getValues().get("std_dev"));
    }
    
    @Test
    void testCalculateBOLL_PercentB() {
        List<Candle> candles = createTestCandles(25, 100.0, 1.0);
        
        IndicatorParams params = new IndicatorParams();
        params.addParameter("period", 20);
        params.addParameter("stdDev", 2.0);
        
        IndicatorResult result = calculator.calculate(candles, params);
        
        assertNotNull(result);
        double percentB = result.getValues().get("percent_b");
        
        // %B should typically be between 0 and 1
        assertTrue(percentB >= -0.5 && percentB <= 1.5, 
                "%B should typically be between 0 and 1 (allowing some margin)");
    }
    
    @Test
    void testCalculateBOLL_ResultStructure() {
        List<Candle> candles = createTestCandles(25, 100.0, 1.0);
        
        IndicatorParams params = new IndicatorParams();
        params.addParameter("period", 20);
        params.addParameter("stdDev", 2.0);
        
        IndicatorResult result = calculator.calculate(candles, params);
        
        assertNotNull(result);
        assertNotNull(result.getValue());
        assertNotNull(result.getValues());
        assertTrue(result.getValues().containsKey("upper"));
        assertTrue(result.getValues().containsKey("middle"));
        assertTrue(result.getValues().containsKey("lower"));
        assertTrue(result.getValues().containsKey("bandwidth"));
        assertTrue(result.getValues().containsKey("percent_b"));
        assertTrue(result.getValues().containsKey("period"));
        assertTrue(result.getValues().containsKey("std_dev"));
        assertNotNull(result.getTimestamp());
        assertEquals(25, result.getDataPoints());
    }
    
    /**
     * Helper method to create test candles with controlled volatility
     */
    private List<Candle> createTestCandles(int count, double basePrice, double volatility) {
        List<Candle> candles = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            double change = (Math.random() - 0.5) * 2.0 * volatility;
            double price = basePrice + change;
            candles.add(createCandle("BTC-USDT-SWAP", price, i));
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
