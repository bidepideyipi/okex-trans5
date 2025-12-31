package com.supermancell.client.service;

import com.okex.common.proto.IndicatorServiceGrpc;
import com.okex.common.proto.IndicatorServiceProto.*;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * gRPC Client Service for Technical Indicator Calculations
 * 
 * This service provides a convenient wrapper around the gRPC stub,
 * handling connection management and error handling.
 * 
 * Features:
 * - Single indicator calculations (RSI, BOLL, MACD, Pinbar)
 * - Batch calculations with streaming responses
 * - Real-time indicator streaming
 * - Automatic connection management via Spring Boot gRPC starter
 */
@Service
public class IndicatorClientService {
    
    private static final Logger log = LoggerFactory.getLogger(IndicatorClientService.class);
    
    /**
     * Blocking stub for synchronous calls
     * Automatically connected to the configured gRPC server
     */
    @GrpcClient("okex-server")
    private IndicatorServiceGrpc.IndicatorServiceBlockingStub blockingStub;
    
    // ==================== RSI Methods ====================
    
    /**
     * Calculate RSI (Relative Strength Index)
     * 
     * @param symbol Trading pair symbol (e.g., "BTC-USDT-SWAP")
     * @param interval Candle interval (e.g., "1m", "1H")
     * @param period RSI period (typically 14)
     * @param limit Number of candles to fetch
     * @return IndicatorResponse with RSI value
     */
    public IndicatorResponse calculateRSI(String symbol, String interval, int period, int limit) {
        try {
            RSIRequest request = RSIRequest.newBuilder()
                    .setBase(BaseRequest.newBuilder()
                            .setSymbol(symbol)
                            .setInterval(interval)
                            .setLimit(limit)
                            .build())
                    .setPeriod(period)
                    .build();
            
            log.debug("Calling gRPC calculateRSI: symbol={}, interval={}, period={}", symbol, interval, period);
            IndicatorResponse response = blockingStub.calculateRSI(request);
            log.debug("Received RSI response: success={}, value={}", response.getSuccess(), response.getRsiValue());
            
            return response;
            
        } catch (StatusRuntimeException e) {
            log.error("gRPC call failed for calculateRSI", e);
            return buildErrorResponse("gRPC error: " + e.getStatus().getDescription(), IndicatorType.RSI);
        }
    }
    
    /**
     * Calculate RSI for multiple symbols/intervals in batch
     * 
     * @param requests List of RSI requests
     * @return List of indicator responses
     */
    public List<IndicatorResponse> calculateRSIBatch(List<RSIRequest> requests) {
        try {
            BatchRSIRequest batchRequest = BatchRSIRequest.newBuilder()
                    .addAllRequests(requests)
                    .build();
            
            log.debug("Calling gRPC calculateRSIBatch: {} requests", requests.size());
            
            List<IndicatorResponse> responses = new ArrayList<>();
            Iterator<IndicatorResponse> iterator = blockingStub
                    .withDeadline(io.grpc.Deadline.after(30, TimeUnit.SECONDS))
                    .calculateRSIBatch(batchRequest);
            
            while (iterator.hasNext()) {
                responses.add(iterator.next());
            }
            
            log.debug("Received {} RSI batch responses", responses.size());
            return responses;
            
        } catch (StatusRuntimeException e) {
            log.error("gRPC call failed for calculateRSIBatch", e);
            List<IndicatorResponse> errorResponses = new ArrayList<>();
            errorResponses.add(buildErrorResponse("Batch error: " + e.getStatus().getDescription(), IndicatorType.RSI));
            return errorResponses;
        }
    }
    
    // ==================== BOLL Methods ====================
    
    /**
     * Calculate Bollinger Bands
     * 
     * @param symbol Trading pair symbol
     * @param interval Candle interval
     * @param period BOLL period (typically 20)
     * @param stdDev Standard deviation multiplier (typically 2.0)
     * @param limit Number of candles to fetch
     * @return IndicatorResponse with BOLL upper/middle/lower values
     */
    public IndicatorResponse calculateBOLL(String symbol, String interval, int period, double stdDev, int limit) {
        try {
            BOLLRequest request = BOLLRequest.newBuilder()
                    .setBase(BaseRequest.newBuilder()
                            .setSymbol(symbol)
                            .setInterval(interval)
                            .setLimit(limit)
                            .build())
                    .setPeriod(period)
                    .setStdDev(stdDev)
                    .build();
            
            log.debug("Calling gRPC calculateBOLL: symbol={}, interval={}, period={}, stdDev={}", 
                    symbol, interval, period, stdDev);
            IndicatorResponse response = blockingStub.calculateBOLL(request);
            log.debug("Received BOLL response: success={}, upper={}, middle={}, lower={}", 
                    response.getSuccess(), response.getBollUpper(), 
                    response.getBollMiddle(), response.getBollLower());
            
            return response;
            
        } catch (StatusRuntimeException e) {
            log.error("gRPC call failed for calculateBOLL", e);
            return buildErrorResponse("gRPC error: " + e.getStatus().getDescription(), IndicatorType.BOLL);
        }
    }
    
    /**
     * Calculate Bollinger Bands for multiple requests in batch
     */
    public List<IndicatorResponse> calculateBOLLBatch(List<BOLLRequest> requests) {
        try {
            BatchBOLLRequest batchRequest = BatchBOLLRequest.newBuilder()
                    .addAllRequests(requests)
                    .build();
            
            log.debug("Calling gRPC calculateBOLLBatch: {} requests", requests.size());
            
            List<IndicatorResponse> responses = new ArrayList<>();
            Iterator<IndicatorResponse> iterator = blockingStub
                    .withDeadline(io.grpc.Deadline.after(30, TimeUnit.SECONDS))
                    .calculateBOLLBatch(batchRequest);
            
            while (iterator.hasNext()) {
                responses.add(iterator.next());
            }
            
            log.debug("Received {} BOLL batch responses", responses.size());
            return responses;
            
        } catch (StatusRuntimeException e) {
            log.error("gRPC call failed for calculateBOLLBatch", e);
            List<IndicatorResponse> errorResponses = new ArrayList<>();
            errorResponses.add(buildErrorResponse("Batch error: " + e.getStatus().getDescription(), IndicatorType.BOLL));
            return errorResponses;
        }
    }
    
    // ==================== MACD Methods ====================
    
    /**
     * Calculate MACD (Moving Average Convergence Divergence)
     * 
     * @param symbol Trading pair symbol
     * @param interval Candle interval
     * @param fastPeriod Fast EMA period (typically 12)
     * @param slowPeriod Slow EMA period (typically 26)
     * @param signalPeriod Signal line period (typically 9)
     * @param limit Number of candles to fetch
     * @return IndicatorResponse with MACD line, signal, and histogram
     */
    public IndicatorResponse calculateMACD(String symbol, String interval, 
                                          int fastPeriod, int slowPeriod, int signalPeriod, int limit) {
        try {
            MACDRequest request = MACDRequest.newBuilder()
                    .setBase(BaseRequest.newBuilder()
                            .setSymbol(symbol)
                            .setInterval(interval)
                            .setLimit(limit)
                            .build())
                    .setFastPeriod(fastPeriod)
                    .setSlowPeriod(slowPeriod)
                    .setSignalPeriod(signalPeriod)
                    .build();
            
            log.debug("Calling gRPC calculateMACD: symbol={}, interval={}, fast={}, slow={}, signal={}", 
                    symbol, interval, fastPeriod, slowPeriod, signalPeriod);
            IndicatorResponse response = blockingStub.calculateMACD(request);
            log.debug("Received MACD response: success={}, line={}, signal={}, histogram={}", 
                    response.getSuccess(), response.getMacdLine(), 
                    response.getMacdSignal(), response.getMacdHistogram());
            
            return response;
            
        } catch (StatusRuntimeException e) {
            log.error("gRPC call failed for calculateMACD", e);
            return buildErrorResponse("gRPC error: " + e.getStatus().getDescription(), IndicatorType.MACD);
        }
    }
    
    /**
     * Calculate MACD for multiple requests in batch
     */
    public List<IndicatorResponse> calculateMACDBatch(List<MACDRequest> requests) {
        try {
            BatchMACDRequest batchRequest = BatchMACDRequest.newBuilder()
                    .addAllRequests(requests)
                    .build();
            
            log.debug("Calling gRPC calculateMACDBatch: {} requests", requests.size());
            
            List<IndicatorResponse> responses = new ArrayList<>();
            Iterator<IndicatorResponse> iterator = blockingStub
                    .withDeadline(io.grpc.Deadline.after(30, TimeUnit.SECONDS))
                    .calculateMACDBatch(batchRequest);
            
            while (iterator.hasNext()) {
                responses.add(iterator.next());
            }
            
            log.debug("Received {} MACD batch responses", responses.size());
            return responses;
            
        } catch (StatusRuntimeException e) {
            log.error("gRPC call failed for calculateMACDBatch", e);
            List<IndicatorResponse> errorResponses = new ArrayList<>();
            errorResponses.add(buildErrorResponse("Batch error: " + e.getStatus().getDescription(), IndicatorType.MACD));
            return errorResponses;
        }
    }
    
    // ==================== Pinbar Methods ====================
    
    /**
     * Detect Pinbar candlestick pattern
     * 
     * @param symbol Trading pair symbol
     * @param interval Candle interval
     * @param bodyRatioThreshold Body to total height ratio (typically 0.2)
     * @param wickRatioThreshold Wick to total height ratio (typically 0.6)
     * @param limit Number of candles to analyze
     * @return IndicatorResponse with Pinbar detection results
     */
    public IndicatorResponse calculatePinbar(String symbol, String interval, 
                                            double bodyRatioThreshold, double wickRatioThreshold, int limit) {
        try {
            PinbarRequest request = PinbarRequest.newBuilder()
                    .setBase(BaseRequest.newBuilder()
                            .setSymbol(symbol)
                            .setInterval(interval)
                            .setLimit(limit)
                            .build())
                    .setBodyRatioThreshold(bodyRatioThreshold)
                    .setWickRatioThreshold(wickRatioThreshold)
                    .build();
            
            log.debug("Calling gRPC calculatePinbar: symbol={}, interval={}, bodyRatio={}, wickRatio={}", 
                    symbol, interval, bodyRatioThreshold, wickRatioThreshold);
            IndicatorResponse response = blockingStub.calculatePinbar(request);
            log.debug("Received Pinbar response: success={}, isPinbar={}, isBullish={}", 
                    response.getSuccess(), response.getIsPinbar(), response.getIsBullish());
            
            return response;
            
        } catch (StatusRuntimeException e) {
            log.error("gRPC call failed for calculatePinbar", e);
            return buildErrorResponse("gRPC error: " + e.getStatus().getDescription(), IndicatorType.PINBAR);
        }
    }
    
    /**
     * Detect Pinbar patterns for multiple requests in batch
     */
    public List<IndicatorResponse> calculatePinbarBatch(List<PinbarRequest> requests) {
        try {
            BatchPinbarRequest batchRequest = BatchPinbarRequest.newBuilder()
                    .addAllRequests(requests)
                    .build();
            
            log.debug("Calling gRPC calculatePinbarBatch: {} requests", requests.size());
            
            List<IndicatorResponse> responses = new ArrayList<>();
            Iterator<IndicatorResponse> iterator = blockingStub
                    .withDeadline(io.grpc.Deadline.after(30, TimeUnit.SECONDS))
                    .calculatePinbarBatch(batchRequest);
            
            while (iterator.hasNext()) {
                responses.add(iterator.next());
            }
            
            log.debug("Received {} Pinbar batch responses", responses.size());
            return responses;
            
        } catch (StatusRuntimeException e) {
            log.error("gRPC call failed for calculatePinbarBatch", e);
            List<IndicatorResponse> errorResponses = new ArrayList<>();
            errorResponses.add(buildErrorResponse("Batch error: " + e.getStatus().getDescription(), IndicatorType.PINBAR));
            return errorResponses;
        }
    }
    
    // ==================== Streaming Methods ====================
    
    /**
     * Stream real-time indicators
     * 
     * @param symbol Trading pair symbol
     * @param interval Candle interval
     * @param indicatorTypes List of indicator types to stream
     * @return List of indicator responses
     */
    public List<IndicatorResponse> streamIndicators(String symbol, String interval, List<IndicatorType> indicatorTypes) {
        try {
            StreamRequest request = StreamRequest.newBuilder()
                    .setSymbol(symbol)
                    .setInterval(interval)
                    .addAllIndicators(indicatorTypes)
                    .build();
            
            log.debug("Calling gRPC streamIndicators: symbol={}, interval={}, indicators={}", 
                    symbol, interval, indicatorTypes);
            
            List<IndicatorResponse> responses = new ArrayList<>();
            Iterator<IndicatorResponse> iterator = blockingStub
                    .withDeadline(io.grpc.Deadline.after(60, TimeUnit.SECONDS))
                    .streamIndicators(request);
            
            while (iterator.hasNext()) {
                IndicatorResponse response = iterator.next();
                responses.add(response);
                log.debug("Received stream response: type={}, success={}", 
                        response.getIndicatorType(), response.getSuccess());
            }
            
            log.debug("Stream completed with {} responses", responses.size());
            return responses;
            
        } catch (StatusRuntimeException e) {
            log.error("gRPC call failed for streamIndicators", e);
            List<IndicatorResponse> errorResponses = new ArrayList<>();
            errorResponses.add(buildErrorResponse("Stream error: " + e.getStatus().getDescription(), IndicatorType.RSI));
            return errorResponses;
        }
    }
    
    // ==================== Helper Methods ====================
    
    /**
     * Build an error response
     */
    private IndicatorResponse buildErrorResponse(String errorMessage, IndicatorType type) {
        return IndicatorResponse.newBuilder()
                .setSuccess(false)
                .setErrorMessage(errorMessage)
                .setIndicatorType(type)
                .setTimestamp(System.currentTimeMillis())
                .setDataPoints(0)
                .setFromCache(false)
                .build();
    }
}
