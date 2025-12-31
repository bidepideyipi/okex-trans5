package com.supermancell.client.example;

import com.okex.common.proto.IndicatorServiceProto.*;
import com.supermancell.client.service.IndicatorClientService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import java.util.Arrays;
import java.util.List;

/**
 * gRPC Client Usage Examples
 * 
 * This class demonstrates how to use the IndicatorClientService to:
 * 1. Calculate single indicators (RSI, BOLL, MACD, Pinbar)
 * 2. Perform batch calculations with streaming responses
 * 3. Stream real-time indicators
 * 
 * To run this example:
 * 1. Start the okex-server application (gRPC server on port 50051)
 * 2. Run this client application
 * 3. Observe the gRPC calls and responses in the logs
 */
@SpringBootApplication
@ComponentScan(basePackages = "com.supermancell.client")
public class ClientExample implements CommandLineRunner {
    
    private static final Logger log = LoggerFactory.getLogger(ClientExample.class);
    
    @Autowired
    private IndicatorClientService indicatorClient;
    
    public static void main(String[] args) {
        SpringApplication.run(ClientExample.class, args);
    }
    
    @Override
    public void run(String... args) throws Exception {
        String separator = "================================================================================";
        log.info(separator);
        log.info("Starting gRPC Client Examples");
        log.info(separator);
        
        // Example 1: Calculate RSI
        example1_CalculateRSI();
        
        // Example 2: Calculate Bollinger Bands
        example2_CalculateBOLL();
        
        // Example 3: Calculate MACD
        example3_CalculateMACD();
        
        // Example 4: Detect Pinbar Pattern
        example4_CalculatePinbar();
        
        // Example 5: Batch RSI Calculation
        example5_BatchRSI();
        
        // Example 6: Stream Multiple Indicators
        example6_StreamIndicators();
        
        log.info(separator);
        log.info("All examples completed!");
        log.info(separator);
    }
    
    /**
     * Example 1: Calculate RSI for BTC-USDT-SWAP
     */
    private void example1_CalculateRSI() {
        String separator = "================================================================================";
        log.info("\n" + separator);
        log.info("Example 1: Calculate RSI (Relative Strength Index)");
        log.info(separator);
        
        String symbol = "BTC-USDT-SWAP";
        String interval = "1m";
        int period = 14;
        int limit = 100;
        
        log.info("Requesting RSI for {} with period={}, limit={}", symbol, period, limit);
        
        IndicatorResponse response = indicatorClient.calculateRSI(symbol, interval, period, limit);
        
        if (response.getSuccess()) {
            log.info("✅ RSI Calculation Success!");
            log.info("   Symbol: {}", response.getSymbol());
            log.info("   Interval: {}", response.getInterval());
            log.info("   RSI Value: {}", response.getRsiValue());
            log.info("   Data Points: {}", response.getDataPoints());
            log.info("   From Cache: {}", response.getFromCache());
            log.info("   Timestamp: {}", response.getTimestamp());
        } else {
            log.error("❌ RSI Calculation Failed: {}", response.getErrorMessage());
        }
    }
    
    /**
     * Example 2: Calculate Bollinger Bands for ETH-USDT-SWAP
     */
    private void example2_CalculateBOLL() {
        String separator = "================================================================================";
        log.info("\n" + separator);
        log.info("Example 2: Calculate Bollinger Bands");
        log.info(separator);
        
        String symbol = "ETH-USDT-SWAP";
        String interval = "1H";
        int period = 20;
        double stdDev = 2.0;
        int limit = 100;
        
        log.info("Requesting BOLL for {} with period={}, stdDev={}, limit={}", 
                symbol, period, stdDev, limit);
        
        IndicatorResponse response = indicatorClient.calculateBOLL(symbol, interval, period, stdDev, limit);
        
        if (response.getSuccess()) {
            log.info("✅ BOLL Calculation Success!");
            log.info("   Symbol: {}", response.getSymbol());
            log.info("   Interval: {}", response.getInterval());
            log.info("   Upper Band: {}", response.getBollUpper());
            log.info("   Middle Band: {}", response.getBollMiddle());
            log.info("   Lower Band: {}", response.getBollLower());
            log.info("   Data Points: {}", response.getDataPoints());
            log.info("   Timestamp: {}", response.getTimestamp());
        } else {
            log.error("❌ BOLL Calculation Failed: {}", response.getErrorMessage());
        }
    }
    
    /**
     * Example 3: Calculate MACD for BTC-USDT-SWAP
     */
    private void example3_CalculateMACD() {
        String separator = "================================================================================";
        log.info("\n" + separator);
        log.info("Example 3: Calculate MACD (Moving Average Convergence Divergence)");
        log.info(separator);
        
        String symbol = "BTC-USDT-SWAP";
        String interval = "1H";
        int fastPeriod = 12;
        int slowPeriod = 26;
        int signalPeriod = 9;
        int limit = 100;
        
        log.info("Requesting MACD for {} with fast={}, slow={}, signal={}, limit={}", 
                symbol, fastPeriod, slowPeriod, signalPeriod, limit);
        
        IndicatorResponse response = indicatorClient.calculateMACD(
                symbol, interval, fastPeriod, slowPeriod, signalPeriod, limit);
        
        if (response.getSuccess()) {
            log.info("✅ MACD Calculation Success!");
            log.info("   Symbol: {}", response.getSymbol());
            log.info("   Interval: {}", response.getInterval());
            log.info("   MACD Line: {}", response.getMacdLine());
            log.info("   Signal Line: {}", response.getMacdSignal());
            log.info("   Histogram: {}", response.getMacdHistogram());
            log.info("   Data Points: {}", response.getDataPoints());
            log.info("   Timestamp: {}", response.getTimestamp());
        } else {
            log.error("❌ MACD Calculation Failed: {}", response.getErrorMessage());
        }
    }
    
    /**
     * Example 4: Detect Pinbar Pattern
     */
    private void example4_CalculatePinbar() {
        String separator = "================================================================================";
        log.info("\n" + separator);
        log.info("Example 4: Detect Pinbar Candlestick Pattern");
        log.info(separator);
        
        String symbol = "BTC-USDT-SWAP";
        String interval = "1H";
        double bodyRatio = 0.2;
        double wickRatio = 0.6;
        int limit = 10;
        
        log.info("Requesting Pinbar for {} with bodyRatio={}, wickRatio={}, limit={}", 
                symbol, bodyRatio, wickRatio, limit);
        
        IndicatorResponse response = indicatorClient.calculatePinbar(
                symbol, interval, bodyRatio, wickRatio, limit);
        
        if (response.getSuccess()) {
            log.info("✅ Pinbar Detection Success!");
            log.info("   Symbol: {}", response.getSymbol());
            log.info("   Interval: {}", response.getInterval());
            log.info("   Is Pinbar: {}", response.getIsPinbar());
            log.info("   Is Bullish: {}", response.getIsBullish());
            log.info("   Body Ratio: {}", response.getBodyRatio());
            log.info("   Upper Wick Ratio: {}", response.getUpperWickRatio());
            log.info("   Lower Wick Ratio: {}", response.getLowerWickRatio());
            log.info("   Data Points: {}", response.getDataPoints());
            log.info("   Timestamp: {}", response.getTimestamp());
        } else {
            log.error("❌ Pinbar Detection Failed: {}", response.getErrorMessage());
        }
    }
    
    /**
     * Example 5: Batch RSI Calculation for multiple symbols
     */
    private void example5_BatchRSI() {
        String separator = "================================================================================";
        log.info("\n" + separator);
        log.info("Example 5: Batch RSI Calculation");
        log.info(separator);
        
        // Prepare batch requests
        List<RSIRequest> requests = Arrays.asList(
                RSIRequest.newBuilder()
                        .setBase(BaseRequest.newBuilder()
                                .setSymbol("BTC-USDT-SWAP")
                                .setInterval("1m")
                                .setLimit(100)
                                .build())
                        .setPeriod(14)
                        .build(),
                RSIRequest.newBuilder()
                        .setBase(BaseRequest.newBuilder()
                                .setSymbol("ETH-USDT-SWAP")
                                .setInterval("1m")
                                .setLimit(100)
                                .build())
                        .setPeriod(14)
                        .build(),
                RSIRequest.newBuilder()
                        .setBase(BaseRequest.newBuilder()
                                .setSymbol("BTC-USDT-SWAP")
                                .setInterval("1H")
                                .setLimit(100)
                                .build())
                        .setPeriod(14)
                        .build()
        );
        
        log.info("Requesting batch RSI for {} symbols", requests.size());
        
        List<IndicatorResponse> responses = indicatorClient.calculateRSIBatch(requests);
        
        log.info("✅ Received {} batch RSI responses:", responses.size());
        for (int i = 0; i < responses.size(); i++) {
            IndicatorResponse response = responses.get(i);
            if (response.getSuccess()) {
                log.info("   [{}] Symbol: {}, Interval: {}, RSI: {}, DataPoints: {}", 
                        i + 1, response.getSymbol(), response.getInterval(), 
                        response.getRsiValue(), response.getDataPoints());
            } else {
                log.error("   [{}] Failed: {}", i + 1, response.getErrorMessage());
            }
        }
    }
    
    /**
     * Example 6: Stream multiple indicators for a symbol
     */
    private void example6_StreamIndicators() {
        String separator = "================================================================================";
        log.info("\n" + separator);
        log.info("Example 6: Stream Multiple Indicators");
        log.info(separator);
        
        String symbol = "BTC-USDT-SWAP";
        String interval = "1m";
        List<IndicatorType> indicators = Arrays.asList(
                IndicatorType.RSI,
                IndicatorType.BOLL,
                IndicatorType.MACD,
                IndicatorType.PINBAR
        );
        
        log.info("Requesting stream for {} with {} indicators", symbol, indicators.size());
        
        List<IndicatorResponse> responses = indicatorClient.streamIndicators(symbol, interval, indicators);
        
        log.info("✅ Received {} streaming responses:", responses.size());
        for (IndicatorResponse response : responses) {
            if (response.getSuccess()) {
                log.info("   Type: {}, Symbol: {}, Success: {}, DataPoints: {}", 
                        response.getIndicatorType(), response.getSymbol(), 
                        response.getSuccess(), response.getDataPoints());
                
                switch (response.getIndicatorType()) {
                    case RSI:
                        log.info("      RSI Value: {}", response.getRsiValue());
                        break;
                    case BOLL:
                        log.info("      BOLL: Upper={}, Middle={}, Lower={}", 
                                response.getBollUpper(), response.getBollMiddle(), response.getBollLower());
                        break;
                    case MACD:
                        log.info("      MACD: Line={}, Signal={}, Histogram={}", 
                                response.getMacdLine(), response.getMacdSignal(), response.getMacdHistogram());
                        break;
                    case PINBAR:
                        log.info("      Pinbar: isPinbar={}, isBullish={}", 
                                response.getIsPinbar(), response.getIsBullish());
                        break;
                }
            } else {
                log.error("   Type: {}, Failed: {}", response.getIndicatorType(), response.getErrorMessage());
            }
        }
    }
}
