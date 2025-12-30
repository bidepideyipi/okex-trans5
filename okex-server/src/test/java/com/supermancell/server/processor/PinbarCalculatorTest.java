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
 * Unit tests for PinbarCalculator
 */
class PinbarCalculatorTest {
    
    private PinbarCalculator calculator;
    
    @BeforeEach
    void setUp() {
        calculator = new PinbarCalculator();
        ReflectionTestUtils.setField(calculator, "defaultBodyRatioThreshold", 0.2);
        ReflectionTestUtils.setField(calculator, "defaultWickRatioThreshold", 0.6);
    }
    
    @Test
    void testGetName() {
        assertEquals("PINBAR", calculator.getName());
    }
    
    @Test
    void testCalculatePinbar_WithNullCandles() {
        IndicatorParams params = new IndicatorParams();
        
        IndicatorResult result = calculator.calculate(null, params);
        
        assertNotNull(result);
        assertNull(result.getValue());
        assertEquals(-1.0, result.getValues().get("error"));
    }
    
    @Test
    void testCalculatePinbar_WithEmptyCandles() {
        List<Candle> candles = new ArrayList<>();
        IndicatorParams params = new IndicatorParams();
        
        IndicatorResult result = calculator.calculate(candles, params);
        
        assertNotNull(result);
        assertNull(result.getValue());
        assertEquals(-1.0, result.getValues().get("error"));
    }

    /**
     *  锤头线 (Bullish Hammer)：这根K线拥有一个很小的实体（可阳可阴），位于价格的顶部；
     *  一条很短或没有的上影线；以及一条长长的下影线，其长度通常至少是实体高度的两倍。
     *  它的市场故事是：价格在开盘后一度遭遇强烈抛压，大幅下跌，但此时买方力量强势介入，将价格重新推高至开盘价附近收盘。
     *  这表明市场在低位获得了强有力的支撑，是下跌趋势可能终结的强烈信号。
     *  下影线越长，说明多方的反击越有力，信号的可靠性也越高。
     */
    @Test
    void testCalculatePinbar_BullishHammer() {
        // Create a bullish pinbar (hammer): small body, long lower wick
        List<Candle> candles = new ArrayList<>();
        
        // Add some regular candles
        for (int i = 0; i < 5; i++) {
            candles.add(createRegularCandle(100.0, i));
        }
        
        // Add bullish pinbar as last candle
        Candle hammer = new Candle();
        hammer.setSymbol("BTC-USDT-SWAP");
        hammer.setTimestamp(Instant.now());
        hammer.setInterval("1m");
        hammer.setHigh(100.0);    // Top of body
        hammer.setOpen(99.5);     // Body top
        hammer.setClose(99.8);    // Body bottom (slightly higher)
        hammer.setLow(95.0);      // Long lower wick
        hammer.setVolume(1000.0);
        hammer.setConfirm("1");
        candles.add(hammer);
        
        IndicatorParams params = new IndicatorParams();
        params.addParameter("bodyRatioThreshold", 0.2);
        params.addParameter("wickRatioThreshold", 0.6);
        
        IndicatorResult result = calculator.calculate(candles, params);
        
        assertNotNull(result);
        assertEquals(1.0, result.getValue(), "Should detect pinbar");
        assertEquals(1.0, result.getValues().get("is_pinbar"));
        assertEquals(1.0, result.getValues().get("is_bullish"), "Should be bullish pinbar");
        assertEquals(0.0, result.getValues().get("is_bearish"));
        
        // Verify ratios
        double bodyRatio = result.getValues().get("body_ratio");
        double lowerWickRatio = result.getValues().get("lower_wick_ratio");
        
        assertTrue(bodyRatio < 0.2, "Body ratio should be small");
        assertTrue(lowerWickRatio > 0.6, "Lower wick ratio should be large");
    }

    /**
     *  射击之星 (Bearish Shooting Star)：这根K线与锤头线形态相似但意义相反，它也拥有一个很小的实体（可阳可阴），位于价格的底部；
     *  一条很短或没有的下影线；以及一条长长的上影线，其长度至少是实体高度的两倍。
     *  它的市场故事是：在上涨趋势中，买方继续推动价格创出新高，但高位遭遇强大的卖压，导致价格大幅回落，最终收盘价回到开盘价附近。
     *  这像是多头力量耗尽、空头开始发力的标志，预示着上涨趋势可能反转
     */
    @Test
    void testCalculatePinbar_BearishShootingStar() {
        // Create a bearish pinbar (shooting star): small body, long upper wick
        List<Candle> candles = new ArrayList<>();
        
        // Add some regular candles
        for (int i = 0; i < 5; i++) {
            candles.add(createRegularCandle(100.0, i));
        }
        
        // Add bearish pinbar as last candle
        Candle shootingStar = new Candle();
        shootingStar.setSymbol("BTC-USDT-SWAP");
        shootingStar.setTimestamp(Instant.now());
        shootingStar.setInterval("1m");
        shootingStar.setHigh(105.0);  // Long upper wick
        shootingStar.setOpen(100.2);  // Body top
        shootingStar.setClose(99.8);  // Body bottom
        shootingStar.setLow(99.5);    // Bottom of body
        shootingStar.setVolume(1000.0);
        shootingStar.setConfirm("1");
        candles.add(shootingStar);
        
        IndicatorParams params = new IndicatorParams();
        params.addParameter("bodyRatioThreshold", 0.2);
        params.addParameter("wickRatioThreshold", 0.6);
        
        IndicatorResult result = calculator.calculate(candles, params);
        
        assertNotNull(result);
        assertEquals(1.0, result.getValue(), "Should detect pinbar");
        assertEquals(1.0, result.getValues().get("is_pinbar"));
        assertEquals(0.0, result.getValues().get("is_bullish"));
        assertEquals(1.0, result.getValues().get("is_bearish"), "Should be bearish pinbar");
        
        // Verify ratios
        double bodyRatio = result.getValues().get("body_ratio");
        double upperWickRatio = result.getValues().get("upper_wick_ratio");
        
        assertTrue(bodyRatio < 0.2, "Body ratio should be small");
        assertTrue(upperWickRatio > 0.6, "Upper wick ratio should be large");
    }
    
    @Test
    void testCalculatePinbar_NoPinbarDetected() {
        // Create a regular candle (not a pinbar): normal body, balanced wicks
        List<Candle> candles = new ArrayList<>();
        
        Candle regular = new Candle();
        regular.setSymbol("BTC-USDT-SWAP");
        regular.setTimestamp(Instant.now());
        regular.setInterval("1m");
        regular.setHigh(102.0);
        regular.setOpen(100.0);
        regular.setClose(101.5);   // Large body
        regular.setLow(99.5);
        regular.setVolume(1000.0);
        regular.setConfirm("1");
        candles.add(regular);
        
        IndicatorParams params = new IndicatorParams();
        params.addParameter("bodyRatioThreshold", 0.2);
        params.addParameter("wickRatioThreshold", 0.6);
        
        IndicatorResult result = calculator.calculate(candles, params);
        
        assertNotNull(result);
        assertEquals(0.0, result.getValue(), "Should not detect pinbar");
        assertEquals(0.0, result.getValues().get("is_pinbar"));
        assertEquals(0.0, result.getValues().get("is_bullish"));
        assertEquals(0.0, result.getValues().get("is_bearish"));
    }
    
    @Test
    void testCalculatePinbar_DojiCandle() {
        // Create a doji candle (no range): should not be detected as pinbar
        List<Candle> candles = new ArrayList<>();
        
        Candle doji = new Candle();
        doji.setSymbol("BTC-USDT-SWAP");
        doji.setTimestamp(Instant.now());
        doji.setInterval("1m");
        doji.setHigh(100.0);
        doji.setOpen(100.0);
        doji.setClose(100.0);
        doji.setLow(100.0);
        doji.setVolume(1000.0);
        doji.setConfirm("1");
        candles.add(doji);
        
        IndicatorParams params = new IndicatorParams();
        
        IndicatorResult result = calculator.calculate(candles, params);
        
        assertNotNull(result);
        assertEquals(0.0, result.getValue(), "Doji should not be detected as pinbar");
        assertEquals(0.0, result.getValues().get("is_pinbar"));
    }
    
    @Test
    void testCalculatePinbar_WithCustomThresholds() {
        List<Candle> candles = new ArrayList<>();
        
        // Create a moderate pinbar
        Candle pinbar = new Candle();
        pinbar.setSymbol("BTC-USDT-SWAP");
        pinbar.setTimestamp(Instant.now());
        pinbar.setInterval("1m");
        pinbar.setHigh(100.0);
        pinbar.setOpen(99.0);
        pinbar.setClose(99.2);
        pinbar.setLow(96.0);  // Moderate lower wick
        pinbar.setVolume(1000.0);
        pinbar.setConfirm("1");
        candles.add(pinbar);
        
        // With stricter thresholds
        IndicatorParams strictParams = new IndicatorParams();
        strictParams.addParameter("bodyRatioThreshold", 0.1);  // Stricter
        strictParams.addParameter("wickRatioThreshold", 0.8);  // Stricter
        
        IndicatorResult strictResult = calculator.calculate(candles, strictParams);
        
        // With looser thresholds
        IndicatorParams looseParams = new IndicatorParams();
        looseParams.addParameter("bodyRatioThreshold", 0.3);   // Looser
        looseParams.addParameter("wickRatioThreshold", 0.5);   // Looser
        
        IndicatorResult looseResult = calculator.calculate(candles, looseParams);
        
        assertNotNull(strictResult);
        assertNotNull(looseResult);
        
        // The looser threshold should be more likely to detect pinbar
        assertEquals(0.3, looseResult.getValues().get("body_threshold"));
        assertEquals(0.5, looseResult.getValues().get("wick_threshold"));
    }
    
    @Test
    void testCalculatePinbar_WithDefaultThresholds() {
        List<Candle> candles = new ArrayList<>();
        candles.add(createRegularCandle(100.0, 0));
        
        IndicatorParams params = new IndicatorParams();
        // Don't specify thresholds, should use defaults
        
        IndicatorResult result = calculator.calculate(candles, params);
        
        assertNotNull(result);
        assertEquals(0.2, result.getValues().get("body_threshold"));
        assertEquals(0.6, result.getValues().get("wick_threshold"));
    }
    
    @Test
    void testCalculatePinbar_ResultStructure() {
        List<Candle> candles = new ArrayList<>();
        candles.add(createRegularCandle(100.0, 0));
        
        IndicatorParams params = new IndicatorParams();
        
        IndicatorResult result = calculator.calculate(candles, params);
        
        assertNotNull(result);
        assertNotNull(result.getValue());
        assertNotNull(result.getValues());
        assertTrue(result.getValues().containsKey("is_pinbar"));
        assertTrue(result.getValues().containsKey("is_bullish"));
        assertTrue(result.getValues().containsKey("is_bearish"));
        assertTrue(result.getValues().containsKey("body_ratio"));
        assertTrue(result.getValues().containsKey("upper_wick_ratio"));
        assertTrue(result.getValues().containsKey("lower_wick_ratio"));
        assertTrue(result.getValues().containsKey("body_threshold"));
        assertTrue(result.getValues().containsKey("wick_threshold"));
        assertNotNull(result.getTimestamp());
        assertEquals(1, result.getDataPoints());
    }
    
    @Test
    void testCalculatePinbar_RatiosAddUp() {
        List<Candle> candles = new ArrayList<>();
        candles.add(createRegularCandle(100.0, 0));
        
        IndicatorParams params = new IndicatorParams();
        
        IndicatorResult result = calculator.calculate(candles, params);
        
        assertNotNull(result);
        double bodyRatio = result.getValues().get("body_ratio");
        double upperWickRatio = result.getValues().get("upper_wick_ratio");
        double lowerWickRatio = result.getValues().get("lower_wick_ratio");
        
        // Body + upper wick + lower wick should approximately equal 1.0
        double sum = bodyRatio + upperWickRatio + lowerWickRatio;
        assertEquals(1.0, sum, 0.01, "Ratios should sum to approximately 1.0");
    }
    
    @Test
    void testCalculatePinbar_AnalyzesLastCandle() {
        // Add multiple candles, only last should be analyzed
        List<Candle> candles = new ArrayList<>();
        
        // Add regular candles
        for (int i = 0; i < 5; i++) {
            candles.add(createRegularCandle(100.0, i + 1));
        }
        
        // Add pinbar as last candle
        Candle pinbar = new Candle();
        pinbar.setSymbol("BTC-USDT-SWAP");
        pinbar.setTimestamp(Instant.now());
        pinbar.setInterval("1m");
        pinbar.setHigh(100.0);
        pinbar.setOpen(99.5);
        pinbar.setClose(99.8);
        pinbar.setLow(95.0);  // Bullish pinbar
        pinbar.setVolume(1000.0);
        pinbar.setConfirm("1");
        candles.add(pinbar);
        
        IndicatorParams params = new IndicatorParams();
        
        IndicatorResult result = calculator.calculate(candles, params);
        
        assertNotNull(result);
        assertEquals(1.0, result.getValue(), "Should detect pinbar in last candle");
        assertEquals(6, result.getDataPoints(), "Should report total candles analyzed");
    }
    
    /**
     * Helper method to create a regular (non-pinbar) candle
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
