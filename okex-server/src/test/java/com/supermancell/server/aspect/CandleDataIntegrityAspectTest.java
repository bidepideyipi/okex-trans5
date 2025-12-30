package com.supermancell.server.aspect;

import com.supermancell.common.model.Candle;
import com.supermancell.server.cache.CandleCacheService;
import com.supermancell.server.client.OkexRestClient;
import com.supermancell.server.repository.CandleRepository;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CandleDataIntegrityAspect
 */
class CandleDataIntegrityAspectTest {
    
    @Mock
    private OkexRestClient okexRestClient;
    
    @Mock
    private CandleRepository candleRepository;
    
    @Mock
    private CandleCacheService candleCacheService;
    
    @Mock
    private ProceedingJoinPoint joinPoint;
    
    private CandleDataIntegrityAspect aspect;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        aspect = new CandleDataIntegrityAspect(okexRestClient, candleRepository, candleCacheService);
        
        // Set default configuration
        ReflectionTestUtils.setField(aspect, "integrityCheckEnabled", true);
        ReflectionTestUtils.setField(aspect, "strictMode", false);
        ReflectionTestUtils.setField(aspect, "fetchLimit", 300);
    }
    
    @Test
    void testCheckCandleDataIntegrity_DisabledCheck() throws Throwable {
        // Given: Integrity check is disabled
        ReflectionTestUtils.setField(aspect, "integrityCheckEnabled", false);
        
        Object[] args = {"BTC-USDT-SWAP", "1m", 100};
        when(joinPoint.getArgs()).thenReturn(args);
        when(joinPoint.proceed()).thenReturn(new ArrayList<>());
        
        // When: Aspect is invoked
        aspect.checkCandleDataIntegrity(joinPoint);
        
        // Then: Should proceed without validation
        verify(joinPoint, times(1)).proceed();
        verify(okexRestClient, never()).getCandles(anyString(), anyString(), anyInt());
    }
    
    @Test
    void testCheckCandleDataIntegrity_CompleteAndContinuousData() throws Throwable {
        // Given: Complete and continuous data
        String symbol = "BTC-USDT-SWAP";
        String interval = "1m";
        int limit = 10;
        
        List<Candle> completeCandles = createContinuousCandles(symbol, interval, 10);
        
        Object[] args = {symbol, interval, limit};
        when(joinPoint.getArgs()).thenReturn(args);
        when(joinPoint.proceed()).thenReturn(completeCandles);
        
        // When: Aspect is invoked
        Object result = aspect.checkCandleDataIntegrity(joinPoint);
        
        // Then: Should return original data without fetching from API
        assertNotNull(result);
        assertEquals(completeCandles, result);
        verify(joinPoint, times(1)).proceed();
        verify(okexRestClient, never()).getCandles(anyString(), anyString(), anyInt());
    }
    
    @Test
    void testCheckCandleDataIntegrity_IncompleteData_NonStrictMode() throws Throwable {
        // Given: Incomplete data and non-strict mode
        String symbol = "ETH-USDT-SWAP";
        String interval = "1H";
        int limit = 100;
        
        // Only 50 candles (incomplete)
        List<Candle> incompleteCandles = createContinuousCandles(symbol, interval, 50);
        
        Object[] args = {symbol, interval, limit};
        when(joinPoint.getArgs()).thenReturn(args);
        when(joinPoint.proceed()).thenReturn(incompleteCandles);
        
        // When: Aspect is invoked
        Object result = aspect.checkCandleDataIntegrity(joinPoint);
        
        // Then: Should return incomplete data with warning
        assertNotNull(result);
        assertEquals(incompleteCandles, result);
        verify(okexRestClient, never()).getCandles(anyString(), anyString(), anyInt());
    }
    
    @Test
    void testCheckCandleDataIntegrity_IncompleteData_StrictMode() throws Throwable {
        // Given: Incomplete data and strict mode
        ReflectionTestUtils.setField(aspect, "strictMode", true);
        
        String symbol = "BTC-USDT-SWAP";
        String interval = "1m";
        int limit = 100;
        
        // Only 50 candles (incomplete)
        List<Candle> incompleteCandles = createContinuousCandles(symbol, interval, 50);
        List<Candle> completeCandles = createContinuousCandles(symbol, interval, 100);
        
        Object[] args = {symbol, interval, limit};
        when(joinPoint.getArgs()).thenReturn(args);
        when(joinPoint.proceed()).thenReturn(incompleteCandles);
        when(okexRestClient.getCandles(symbol, interval, limit)).thenReturn(completeCandles);
        
        // When: Aspect is invoked
        Object result = aspect.checkCandleDataIntegrity(joinPoint);
        
        // Then: Should fetch complete data from API
        assertNotNull(result);
        assertEquals(completeCandles, result);
        verify(okexRestClient, times(1)).getCandles(symbol, interval, limit);
        verify(candleRepository, times(1)).saveBatch(completeCandles);
    }
    
    @Test
    void testCheckCandleDataIntegrity_DiscontinuousData_StrictMode() throws Throwable {
        // Given: Discontinuous data and strict mode
        ReflectionTestUtils.setField(aspect, "strictMode", true);
        
        String symbol = "ETH-USDT-SWAP";
        String interval = "1H";
        int limit = 10;
        
        // Create discontinuous candles (with gaps)
        List<Candle> discontinuousCandles = createDiscontinuousCandles(symbol, interval, 10);
        List<Candle> completeCandles = createContinuousCandles(symbol, interval, 10);
        
        Object[] args = {symbol, interval, limit};
        when(joinPoint.getArgs()).thenReturn(args);
        when(joinPoint.proceed()).thenReturn(discontinuousCandles);
        when(okexRestClient.getCandles(symbol, interval, limit)).thenReturn(completeCandles);
        
        // When: Aspect is invoked
        Object result = aspect.checkCandleDataIntegrity(joinPoint);
        
        // Then: Should fetch complete data from API
        assertNotNull(result);
        assertEquals(completeCandles, result);
        verify(okexRestClient, times(1)).getCandles(symbol, interval, limit);
    }
    
    @Test
    void testCheckCandleDataIntegrity_EmptyData_StrictMode() throws Throwable {
        // Given: Empty data and strict mode
        ReflectionTestUtils.setField(aspect, "strictMode", true);
        
        String symbol = "BTC-USDT-SWAP";
        String interval = "1m";
        int limit = 100;
        
        List<Candle> emptyList = new ArrayList<>();
        List<Candle> completeCandles = createContinuousCandles(symbol, interval, 100);
        
        Object[] args = {symbol, interval, limit};
        when(joinPoint.getArgs()).thenReturn(args);
        when(joinPoint.proceed()).thenReturn(emptyList);
        when(okexRestClient.getCandles(symbol, interval, limit)).thenReturn(completeCandles);
        
        // When: Aspect is invoked
        Object result = aspect.checkCandleDataIntegrity(joinPoint);
        
        // Then: Should fetch complete data from API
        assertNotNull(result);
        assertEquals(completeCandles, result);
        verify(okexRestClient, times(1)).getCandles(symbol, interval, limit);
    }
    
    @Test
    void testCheckCandleDataIntegrity_NinetyPercentComplete() throws Throwable {
        // Given: 90% complete data (should pass completeness check)
        String symbol = "BTC-USDT-SWAP";
        String interval = "1m";
        int limit = 100;
        
        // 90 candles (90% of required 100)
        List<Candle> ninetyPercentCandles = createContinuousCandles(symbol, interval, 90);
        
        Object[] args = {symbol, interval, limit};
        when(joinPoint.getArgs()).thenReturn(args);
        when(joinPoint.proceed()).thenReturn(ninetyPercentCandles);
        
        // When: Aspect is invoked
        Object result = aspect.checkCandleDataIntegrity(joinPoint);
        
        // Then: Should pass completeness check
        assertNotNull(result);
        assertEquals(ninetyPercentCandles, result);
        verify(okexRestClient, never()).getCandles(anyString(), anyString(), anyInt());
    }
    
    @Test
    void testCheckCandleDataIntegrity_ApiFailure() throws Throwable {
        // Given: Strict mode, incomplete data, and API failure
        ReflectionTestUtils.setField(aspect, "strictMode", true);
        
        String symbol = "BTC-USDT-SWAP";
        String interval = "1m";
        int limit = 100;
        
        List<Candle> incompleteCandles = createContinuousCandles(symbol, interval, 50);
        
        Object[] args = {symbol, interval, limit};
        when(joinPoint.getArgs()).thenReturn(args);
        when(joinPoint.proceed()).thenReturn(incompleteCandles);
        when(okexRestClient.getCandles(symbol, interval, limit)).thenReturn(new ArrayList<>());
        
        // When: Aspect is invoked
        Object result = aspect.checkCandleDataIntegrity(joinPoint);
        
        // Then: Should return empty list due to API failure
        assertNotNull(result);
        assertTrue(((List<?>) result).isEmpty());
        verify(okexRestClient, times(1)).getCandles(symbol, interval, limit);
    }
    
    // Helper methods
    
    /**
     * Create continuous candles with proper time intervals
     */
    private List<Candle> createContinuousCandles(String symbol, String interval, int count) {
        List<Candle> candles = new ArrayList<>();
        Instant baseTime = Instant.now().minus(count, getChronoUnit(interval));
        
        for (int i = 0; i < count; i++) {
            Candle candle = new Candle();
            candle.setSymbol(symbol);
            candle.setInterval(interval);
            candle.setTimestamp(baseTime.plus(i, getChronoUnit(interval)));
            candle.setOpen(40000.0 + i);
            candle.setHigh(40100.0 + i);
            candle.setLow(39900.0 + i);
            candle.setClose(40050.0 + i);
            candle.setVolume(1000.0);
            candle.setConfirm("1");
            candle.setCreatedAt(Instant.now());
            candles.add(candle);
        }
        
        return candles;
    }
    
    /**
     * Create discontinuous candles with gaps
     */
    private List<Candle> createDiscontinuousCandles(String symbol, String interval, int count) {
        List<Candle> candles = new ArrayList<>();
        Instant baseTime = Instant.now().minus(count * 2, getChronoUnit(interval));
        
        for (int i = 0; i < count; i++) {
            Candle candle = new Candle();
            candle.setSymbol(symbol);
            candle.setInterval(interval);
            // Create gaps by skipping time intervals
            candle.setTimestamp(baseTime.plus(i * 2, getChronoUnit(interval)));
            candle.setOpen(40000.0 + i);
            candle.setHigh(40100.0 + i);
            candle.setLow(39900.0 + i);
            candle.setClose(40050.0 + i);
            candle.setVolume(1000.0);
            candle.setConfirm("1");
            candle.setCreatedAt(Instant.now());
            candles.add(candle);
        }
        
        return candles;
    }
    
    /**
     * Get ChronoUnit for interval
     */
    private ChronoUnit getChronoUnit(String interval) {
        if (interval.contains("m")) {
            return ChronoUnit.MINUTES;
        } else if (interval.contains("H")) {
            return ChronoUnit.HOURS;
        } else if (interval.contains("D")) {
            return ChronoUnit.DAYS;
        }
        return ChronoUnit.MINUTES;
    }
}
