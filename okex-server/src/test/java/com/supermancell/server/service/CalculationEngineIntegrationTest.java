package com.supermancell.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.supermancell.common.model.Candle;
import com.supermancell.common.model.IndicatorResult;
import com.supermancell.server.aspect.CandleDataIntegrityAspect;
import com.supermancell.server.cache.CandleCacheService;
import com.supermancell.server.client.OkexRestClient;
import com.supermancell.server.processor.BOLLCalculator;
import com.supermancell.server.processor.MACDCalculator;
import com.supermancell.server.processor.PinbarCalculator;
import com.supermancell.server.processor.RSICalculator;
import com.supermancell.server.repository.CandleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import redis.clients.jedis.JedisPool;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for CalculationEngine
 * 
 * Tests the complete flow:
 * Client → CalculationEngine → CandleRepository → AOP Aspect → 
 * Data Integrity Check → Redis Cache → MongoDB → Calculator → Result
 */
@ExtendWith(MockitoExtension.class)
class CalculationEngineIntegrationTest {
    
    @Mock
    private CandleRepository candleRepository;
    
    @Mock
    private CandleCacheService candleCacheService;
    
    @Mock
    private OkexRestClient okexRestClient;
    
    @Mock
    private JedisPool jedisPool;
    
    private CalculationEngine calculationEngine;
    private RSICalculator rsiCalculator;
    private BOLLCalculator bollCalculator;
    private MACDCalculator macdCalculator;
    private PinbarCalculator pinbarCalculator;
    
    @BeforeEach
    void setUp() {
        // Create real calculator instances
        rsiCalculator = new RSICalculator();
        bollCalculator = new BOLLCalculator();
        macdCalculator = new MACDCalculator();
        pinbarCalculator = new PinbarCalculator();
        
        // Set default values using reflection
        ReflectionTestUtils.setField(rsiCalculator, "defaultPeriod", 14);
        ReflectionTestUtils.setField(bollCalculator, "defaultPeriod", 20);
        ReflectionTestUtils.setField(bollCalculator, "defaultStdDev", 2.0);
        ReflectionTestUtils.setField(macdCalculator, "defaultFastPeriod", 12);
        ReflectionTestUtils.setField(macdCalculator, "defaultSlowPeriod", 26);
        ReflectionTestUtils.setField(macdCalculator, "defaultSignalPeriod", 9);
        ReflectionTestUtils.setField(pinbarCalculator, "defaultBodyRatioThreshold", 0.2);
        ReflectionTestUtils.setField(pinbarCalculator, "defaultWickRatioThreshold", 0.6);
        
        // Create CalculationEngine with real calculators and mocked dependencies
        calculationEngine = new CalculationEngine(
            candleRepository,
            candleCacheService,
            rsiCalculator,
            bollCalculator,
            macdCalculator,
            pinbarCalculator
        );
    }
    
    @Test
    void testCalculateRSI_EndToEnd_WithCacheHit() {
        // Setup: Mock cache hit scenario
        String symbol = "BTC-USDT-SWAP";
        String interval = "1m";
        int period = 14;
        int limit = 100;
        
        IndicatorResult cachedResult = new IndicatorResult();
        cachedResult.setValue(65.5);
        
        when(candleCacheService.getIndicatorResult(anyString())).thenReturn(cachedResult);
        
        // Execute
        IndicatorResult result = calculationEngine.calculateRSI(symbol, interval, period, limit);
        
        // Verify
        assertNotNull(result);
        assertEquals(65.5, result.getValue());
        
        // Verify cache was checked
        verify(candleCacheService).getIndicatorResult(anyString());
        
        // Verify repository was NOT called (cache hit)
        verify(candleRepository, never()).findCandles(anyString(), anyString(), anyInt());
    }
    
    @Test
    void testCalculateRSI_EndToEnd_WithCacheMiss() {
        // Setup: Mock cache miss scenario
        String symbol = "BTC-USDT-SWAP";
        String interval = "1m";
        int period = 14;
        int limit = 100;
        
        // Cache miss
        when(candleCacheService.getIndicatorResult(anyString())).thenReturn(null);
        
        // Mock candle data from repository
        List<Candle> candles = createTestCandles(100, 100.0, 1.0);
        when(candleRepository.findCandles(symbol, interval, limit)).thenReturn(candles);
        
        // Execute
        IndicatorResult result = calculationEngine.calculateRSI(symbol, interval, period, limit);
        
        // Verify
        assertNotNull(result);
        assertNotNull(result.getValue());
        assertTrue(result.getValue() >= 0 && result.getValue() <= 100, "RSI should be 0-100");
        assertEquals(100, result.getDataPoints());
        
        // Verify flow: cache check → repository → calculation → cache save
        verify(candleCacheService).getIndicatorResult(anyString());
        verify(candleRepository).findCandles(symbol, interval, limit);
        verify(candleCacheService).cacheIndicatorResult(anyString(), any(IndicatorResult.class));
    }
    
    @Test
    void testCalculateBOLL_EndToEnd() {
        // Setup
        String symbol = "ETH-USDT-SWAP";
        String interval = "1H";
        int period = 20;
        double stdDev = 2.0;
        int limit = 50;
        
        when(candleCacheService.getIndicatorResult(anyString())).thenReturn(null);
        
        List<Candle> candles = createTestCandles(50, 2000.0, 5.0);
        when(candleRepository.findCandles(symbol, interval, limit)).thenReturn(candles);
        
        // Execute
        IndicatorResult result = calculationEngine.calculateBOLL(symbol, interval, period, stdDev, limit);
        
        // Verify
        assertNotNull(result);
        assertNotNull(result.getValue()); // Middle band
        
        double upper = result.getValues().get("upper");
        double middle = result.getValues().get("middle");
        double lower = result.getValues().get("lower");
        
        assertTrue(upper > middle, "Upper band should be > middle");
        assertTrue(middle > lower, "Middle band should be > lower");
        
        verify(candleRepository).findCandles(symbol, interval, limit);
        verify(candleCacheService).cacheIndicatorResult(anyString(), any(IndicatorResult.class));
    }
    
    @Test
    void testCalculateMACD_EndToEnd() {
        // Setup
        String symbol = "BTC-USDT-SWAP";
        String interval = "1m";
        int fastPeriod = 12;
        int slowPeriod = 26;
        int signalPeriod = 9;
        int limit = 50;
        
        when(candleCacheService.getIndicatorResult(anyString())).thenReturn(null);
        
        List<Candle> candles = createTestCandles(50, 50000.0, 100.0);
        when(candleRepository.findCandles(symbol, interval, limit)).thenReturn(candles);
        
        // Execute
        IndicatorResult result = calculationEngine.calculateMACD(
            symbol, interval, fastPeriod, slowPeriod, signalPeriod, limit
        );
        
        // Verify
        assertNotNull(result);
        assertNotNull(result.getValue()); // MACD line
        
        double macd = result.getValues().get("macd");
        double signal = result.getValues().get("signal");
        double histogram = result.getValues().get("histogram");
        
        // Histogram = MACD - Signal
        assertEquals(macd - signal, histogram, 0.01);
        
        verify(candleRepository).findCandles(symbol, interval, limit);
        verify(candleCacheService).cacheIndicatorResult(anyString(), any(IndicatorResult.class));
    }
    
    @Test
    void testCalculatePinbar_EndToEnd() {
        // Setup
        String symbol = "BTC-USDT-SWAP";
        String interval = "1m";
        double bodyRatioThreshold = 0.2;
        double wickRatioThreshold = 0.6;
        int limit = 10;
        
        when(candleCacheService.getIndicatorResult(anyString())).thenReturn(null);
        
        // Create candles with a pinbar at the end
        List<Candle> candles = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            candles.add(createRegularCandle(100.0, i));
        }
        
        // Add bullish pinbar
        Candle pinbar = new Candle();
        pinbar.setSymbol(symbol);
        pinbar.setTimestamp(Instant.now());
        pinbar.setInterval(interval);
        pinbar.setHigh(100.0);
        pinbar.setOpen(99.5);
        pinbar.setClose(99.8);
        pinbar.setLow(95.0); // Long lower wick
        pinbar.setVolume(1000.0);
        pinbar.setConfirm("1");
        candles.add(pinbar);
        
        when(candleRepository.findCandles(symbol, interval, limit)).thenReturn(candles);
        
        // Execute
        IndicatorResult result = calculationEngine.calculatePinbar(
            symbol, interval, bodyRatioThreshold, wickRatioThreshold, limit
        );
        
        // Verify
        assertNotNull(result);
        assertEquals(1.0, result.getValue(), "Should detect pinbar");
        assertEquals(1.0, result.getValues().get("is_pinbar"));
        assertEquals(1.0, result.getValues().get("is_bullish"));
        
        verify(candleRepository).findCandles(symbol, interval, limit);
        verify(candleCacheService).cacheIndicatorResult(anyString(), any(IndicatorResult.class));
    }
    
    @Test
    void testCalculateRSI_WithEmptyRepository() {
        // Setup: Repository returns empty list
        String symbol = "BTC-USDT-SWAP";
        String interval = "1m";
        
        when(candleCacheService.getIndicatorResult(anyString())).thenReturn(null);
        when(candleRepository.findCandles(anyString(), anyString(), anyInt()))
            .thenReturn(new ArrayList<>());
        
        // Execute
        IndicatorResult result = calculationEngine.calculateRSI(symbol, interval, 14, 100);
        
        // Verify
        assertNotNull(result);
        assertNull(result.getValue());
        assertEquals(-1.0, result.getValues().get("error"));
        
        // Verify no cache save for error result
        verify(candleCacheService, never()).cacheIndicatorResult(anyString(), any(IndicatorResult.class));
    }
    
    @Test
    void testCalculateBOLL_WithInsufficientData() {
        // Setup: Repository returns insufficient data
        String symbol = "BTC-USDT-SWAP";
        String interval = "1m";
        int period = 20;
        
        when(candleCacheService.getIndicatorResult(anyString())).thenReturn(null);
        
        // Only 15 candles, need 20
        List<Candle> candles = createTestCandles(15, 100.0, 1.0);
        when(candleRepository.findCandles(anyString(), anyString(), anyInt())).thenReturn(candles);
        
        // Execute
        IndicatorResult result = calculationEngine.calculateBOLL(symbol, interval, period, 2.0, 100);
        
        // Verify
        assertNotNull(result);
        assertNull(result.getValue());
        assertEquals(-1.0, result.getValues().get("error"));
    }
    
    @Test
    void testMultipleIndicators_SameData() {
        // Setup: Same candle data for different indicators
        String symbol = "BTC-USDT-SWAP";
        String interval = "1m";
        int limit = 100;
        
        when(candleCacheService.getIndicatorResult(anyString())).thenReturn(null);
        
        List<Candle> candles = createTestCandles(100, 100.0, 1.0);
        when(candleRepository.findCandles(symbol, interval, limit)).thenReturn(candles);
        
        // Execute multiple indicator calculations
        IndicatorResult rsiResult = calculationEngine.calculateRSI(symbol, interval, 14, limit);
        IndicatorResult bollResult = calculationEngine.calculateBOLL(symbol, interval, 20, 2.0, limit);
        
        // Verify all succeeded
        assertNotNull(rsiResult);
        assertNotNull(rsiResult.getValue());
        
        assertNotNull(bollResult);
        assertNotNull(bollResult.getValue());
        
        // Verify repository was called for each (since no cache)
        verify(candleRepository, times(2)).findCandles(symbol, interval, limit);
        
        // Verify both results were cached
        verify(candleCacheService, times(2)).cacheIndicatorResult(anyString(), any(IndicatorResult.class));
    }
    
    @Test
    void testCacheKeyUniqueness() {
        // Setup: Different parameters should create different cache keys
        String symbol = "BTC-USDT-SWAP";
        String interval = "1m";
        
        when(candleCacheService.getIndicatorResult(anyString())).thenReturn(null);
        
        List<Candle> candles = createTestCandles(100, 100.0, 1.0);
        when(candleRepository.findCandles(anyString(), anyString(), anyInt())).thenReturn(candles);
        
        // Calculate RSI with different periods
        calculationEngine.calculateRSI(symbol, interval, 14, 100);
        calculationEngine.calculateRSI(symbol, interval, 20, 100);
        
        // Verify cache was checked twice (different keys)
        verify(candleCacheService, times(2)).getIndicatorResult(anyString());
        
        // Verify both results were cached (different keys)
        verify(candleCacheService, times(2)).cacheIndicatorResult(anyString(), any(IndicatorResult.class));
    }
    
    @Test
    void testCalculationEngine_ErrorHandling() {
        // Setup: Repository throws exception
        String symbol = "BTC-USDT-SWAP";
        String interval = "1m";
        
        when(candleCacheService.getIndicatorResult(anyString())).thenReturn(null);
        when(candleRepository.findCandles(anyString(), anyString(), anyInt()))
            .thenThrow(new RuntimeException("Database connection failed"));
        
        // Execute
        IndicatorResult result = calculationEngine.calculateRSI(symbol, interval, 14, 100);
        
        // Verify graceful error handling
        assertNotNull(result);
        assertNull(result.getValue());
        assertEquals(-1.0, result.getValues().get("error"));
    }
    
    @Test
    void testCalculationEngine_NullCacheService() {
        // Setup: Test behavior when cache service returns null (cache disabled)
        String symbol = "BTC-USDT-SWAP";
        String interval = "1m";
        
        when(candleCacheService.getIndicatorResult(anyString())).thenReturn(null);
        
        List<Candle> candles = createTestCandles(100, 100.0, 1.0);
        when(candleRepository.findCandles(anyString(), anyString(), anyInt())).thenReturn(candles);
        
        // Execute
        IndicatorResult result = calculationEngine.calculateRSI(symbol, interval, 14, 100);
        
        // Verify calculation still works
        assertNotNull(result);
        assertNotNull(result.getValue());
        
        verify(candleRepository).findCandles(symbol, interval, 100);
    }
    
    /**
     * Helper: Create test candles with controlled volatility
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
     * Helper: Create a single candle
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
    
    /**
     * Helper: Create a regular candle
     */
    private Candle createRegularCandle(double price, int minutesAgo) {
        Candle candle = new Candle();
        candle.setSymbol("BTC-USDT-SWAP");
        candle.setTimestamp(Instant.now().minusSeconds(minutesAgo * 60L));
        candle.setInterval("1m");
        candle.setOpen(price);
        candle.setHigh(price + 1.0);
        candle.setLow(price - 1.0);
        candle.setClose(price + 0.5);
        candle.setVolume(1000.0);
        candle.setConfirm("1");
        return candle;
    }
}
