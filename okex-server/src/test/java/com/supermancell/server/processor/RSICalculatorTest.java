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
 * Unit tests for RSICalculator
 */
class RSICalculatorTest {
    
    private RSICalculator calculator;
    
    @BeforeEach
    void setUp() {
        calculator = new RSICalculator();
        // Set default period using reflection
        ReflectionTestUtils.setField(calculator, "defaultPeriod", 14);
    }
    
    @Test
    void testGetName() {
        assertEquals("RSI", calculator.getName());
    }
    
    @Test
    void testCalculateRSI_WithNullCandles() {
        IndicatorParams params = new IndicatorParams();
        params.addParameter("period", 14);
        
        IndicatorResult result = calculator.calculate(null, params);
        
        assertNotNull(result);
        assertNull(result.getValue());
        assertEquals(-1.0, result.getValues().get("error"));
    }
    
    @Test
    void testCalculateRSI_WithEmptyCandles() {
        List<Candle> candles = new ArrayList<>();
        IndicatorParams params = new IndicatorParams();
        params.addParameter("period", 14);
        
        IndicatorResult result = calculator.calculate(candles, params);
        
        assertNotNull(result);
        assertNull(result.getValue());
        assertEquals(-1.0, result.getValues().get("error"));
    }
    
    @Test
    void testCalculateRSI_WithInsufficientData() {
        List<Candle> candles = createTestCandles(10); // Less than period + 1
        IndicatorParams params = new IndicatorParams();
        params.addParameter("period", 14);
        
        IndicatorResult result = calculator.calculate(candles, params);
        
        assertNotNull(result);
        assertNull(result.getValue());
        assertEquals(-1.0, result.getValues().get("error"));
    }
    
    @Test
    void testCalculateRSI_WithUpwardTrend() {
        // Create candles with upward price movement
        List<Candle> candles = new ArrayList<>();
        double basePrice = 100.0;
        
        for (int i = 0; i < 20; i++) {
            double price = basePrice + (i * 2.0); // Steady increase
            candles.add(createCandle("BTC-USDT-SWAP", price, price + 1, price - 0.5, price + 0.5, i));
        }
        
        IndicatorParams params = new IndicatorParams();
        params.addParameter("period", 14);
        
        IndicatorResult result = calculator.calculate(candles, params);
        
        assertNotNull(result);
        assertNotNull(result.getValue());
        assertTrue(result.getValue() > 50.0, "RSI should be > 50 for upward trend");
        assertTrue(result.getValue() <= 100.0, "RSI should be <= 100");
        assertEquals(20, result.getDataPoints());
    }
    
    @Test
    void testCalculateRSI_WithDownwardTrend() {
        // Create candles with downward price movement
        List<Candle> candles = new ArrayList<>();
        double basePrice = 200.0;
        
        for (int i = 0; i < 20; i++) {
            double price = basePrice - (i * 2.0); // Steady decrease
            candles.add(createCandle("BTC-USDT-SWAP", price, price + 0.5, price - 1, price - 0.5, i));
        }
        
        IndicatorParams params = new IndicatorParams();
        params.addParameter("period", 14);
        
        IndicatorResult result = calculator.calculate(candles, params);
        
        assertNotNull(result);
        assertNotNull(result.getValue());
        assertTrue(result.getValue() < 50.0, "RSI should be < 50 for downward trend");
        assertTrue(result.getValue() >= 0.0, "RSI should be >= 0");
    }
    
    @Test
    void testCalculateRSI_WithSidewaysMovement() {
        // Create candles with minimal price change
        List<Candle> candles = new ArrayList<>();
        double basePrice = 150.0;
        
        for (int i = 0; i < 20; i++) {
            double price = basePrice + (Math.random() - 0.5) * 0.5; // Small random fluctuation
            candles.add(createCandle("BTC-USDT-SWAP", price, price + 0.2, price - 0.2, price + 0.1, i));
        }
        
        IndicatorParams params = new IndicatorParams();
        params.addParameter("period", 14);
        
        IndicatorResult result = calculator.calculate(candles, params);
        
        assertNotNull(result);
        assertNotNull(result.getValue());
        assertTrue(result.getValue() >= 40.0 && result.getValue() <= 60.0, 
                "RSI should be around 50 for sideways movement");
    }
    
    @Test
    void testCalculateRSI_WithCustomPeriod() {
        List<Candle> candles = createTestCandles(30);
        IndicatorParams params = new IndicatorParams();
        params.addParameter("period", 10); // Custom period
        
        IndicatorResult result = calculator.calculate(candles, params);
        
        assertNotNull(result);
        assertNotNull(result.getValue());
        assertEquals(10.0, result.getValues().get("period"));
    }
    
    @Test
    void testCalculateRSI_WithDefaultPeriod() {
        List<Candle> candles = createTestCandles(20);
        IndicatorParams params = new IndicatorParams();
        // Don't specify period, should use default
        
        IndicatorResult result = calculator.calculate(candles, params);
        
        assertNotNull(result);
        assertNotNull(result.getValue());
        assertEquals(14.0, result.getValues().get("period"));
    }
    
    @Test
    void testCalculateRSI_ResultStructure() {
        List<Candle> candles = createTestCandles(20);
        IndicatorParams params = new IndicatorParams();
        params.addParameter("period", 14);
        
        IndicatorResult result = calculator.calculate(candles, params);
        
        assertNotNull(result);
        assertNotNull(result.getValue());
        assertNotNull(result.getValues());
        assertTrue(result.getValues().containsKey("rsi"));
        assertTrue(result.getValues().containsKey("period"));
        assertNotNull(result.getTimestamp());
        assertEquals(20, result.getDataPoints());
    }
    
    @Test
    void testCalculateRSI_AllGains() {
        // All prices going up - should result in RSI = 100
        List<Candle> candles = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            double price = 100.0 + i * 5.0;
            candles.add(createCandle("BTC-USDT-SWAP", price, price, price, price, i));
        }
        
        IndicatorParams params = new IndicatorParams();
        params.addParameter("period", 14);
        
        IndicatorResult result = calculator.calculate(candles, params);
        
        assertNotNull(result);
        assertNotNull(result.getValue());
        assertEquals(100.0, result.getValue(), 0.1, "RSI should be 100 when all gains");
    }
    
    /**
     * Helper method to create test candles with random prices
     */
    private List<Candle> createTestCandles(int count) {
        List<Candle> candles = new ArrayList<>();
        double basePrice = 100.0;
        
        for (int i = 0; i < count; i++) {
            double change = (Math.random() - 0.5) * 5.0;
            double close = basePrice + change;
            candles.add(createCandle("BTC-USDT-SWAP", basePrice, close + 1, close - 1, close, i));
            basePrice = close;
        }
        
        return candles;
    }
    
    /**
     * Helper method to create a single test candle
     */
    private Candle createCandle(String symbol, double open, double high, double low, double close, int minutesAgo) {
        Candle candle = new Candle();
        candle.setSymbol(symbol);
        candle.setTimestamp(Instant.now().minusSeconds(minutesAgo * 60L));
        candle.setInterval("1m");
        candle.setOpen(open);
        candle.setHigh(high);
        candle.setLow(low);
        candle.setClose(close);
        candle.setVolume(1000.0);
        candle.setConfirm("1");
        return candle;
    }
}
