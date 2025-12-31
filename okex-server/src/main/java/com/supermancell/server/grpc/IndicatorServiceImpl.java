package com.supermancell.server.grpc;

import com.okex.common.proto.IndicatorServiceGrpc;
import com.okex.common.proto.IndicatorServiceProto.*;
import com.supermancell.common.model.IndicatorResult;
import com.supermancell.server.service.CalculationEngine;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;

/**
 * gRPC Service Implementation for Technical Indicator Calculations
 * 
 * Provides 9 RPC methods:
 * 1. calculateRSI - Single RSI calculation
 * 2. calculateRSIBatch - Batch RSI calculation with streaming
 * 3. calculateBOLL - Single Bollinger Bands calculation
 * 4. calculateBOLLBatch - Batch BOLL calculation with streaming
 * 5. calculateMACD - Single MACD calculation
 * 6. calculateMACDBatch - Batch MACD calculation with streaming
 * 7. calculatePinbar - Single Pinbar pattern detection
 * 8. calculatePinbarBatch - Batch Pinbar detection with streaming
 * 9. streamIndicators - Real-time indicator streaming
 */
@GrpcService
public class IndicatorServiceImpl extends IndicatorServiceGrpc.IndicatorServiceImplBase {
    
    private static final Logger log = LoggerFactory.getLogger(IndicatorServiceImpl.class);
    
    private final CalculationEngine calculationEngine;
    
    @Autowired
    public IndicatorServiceImpl(CalculationEngine calculationEngine) {
        this.calculationEngine = calculationEngine;
    }
    
    /**
     * Calculate RSI (Relative Strength Index)
     */
    @Override
    public void calculateRSI(RSIRequest request, StreamObserver<IndicatorResponse> responseObserver) {
        try {
            log.debug("gRPC calculateRSI: symbol={}, interval={}, period={}", 
                    request.getBase().getSymbol(), request.getBase().getInterval(), request.getPeriod());
            
            // Validate request
            if (!request.hasBase() || request.getBase().getSymbol().isEmpty()) {
                responseObserver.onNext(buildErrorResponse("Invalid request: symbol is required", IndicatorType.RSI));
                responseObserver.onCompleted();
                return;
            }
            
            // Call calculation engine
            IndicatorResult result = calculationEngine.calculateRSI(
                    request.getBase().getSymbol(),
                    request.getBase().getInterval(),
                    request.getPeriod(),
                    request.getBase().getLimit()
            );
            
            // Build response
            IndicatorResponse response = buildRSIResponse(request.getBase(), result);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            log.error("Failed to calculate RSI", e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Internal error: " + e.getMessage())
                    .asRuntimeException());
        }
    }
    
    /**
     * Calculate RSI for multiple requests with streaming response
     */
    @Override
    public void calculateRSIBatch(BatchRSIRequest request, StreamObserver<IndicatorResponse> responseObserver) {
        try {
            log.debug("gRPC calculateRSIBatch: {} requests", request.getRequestsCount());
            
            for (RSIRequest rsiRequest : request.getRequestsList()) {
                try {
                    IndicatorResult result = calculationEngine.calculateRSI(
                            rsiRequest.getBase().getSymbol(),
                            rsiRequest.getBase().getInterval(),
                            rsiRequest.getPeriod(),
                            rsiRequest.getBase().getLimit()
                    );
                    
                    responseObserver.onNext(buildRSIResponse(rsiRequest.getBase(), result));
                    
                } catch (Exception e) {
                    log.error("Failed to calculate RSI for symbol: {}", rsiRequest.getBase().getSymbol(), e);
                    responseObserver.onNext(buildErrorResponse(
                            "Failed: " + e.getMessage(), IndicatorType.RSI));
                }
            }
            
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            log.error("Failed to calculate RSI batch", e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Batch error: " + e.getMessage())
                    .asRuntimeException());
        }
    }
    
    /**
     * Calculate Bollinger Bands
     */
    @Override
    public void calculateBOLL(BOLLRequest request, StreamObserver<IndicatorResponse> responseObserver) {
        try {
            log.debug("gRPC calculateBOLL: symbol={}, interval={}, period={}, stdDev={}", 
                    request.getBase().getSymbol(), request.getBase().getInterval(), 
                    request.getPeriod(), request.getStdDev());
            
            if (!request.hasBase() || request.getBase().getSymbol().isEmpty()) {
                responseObserver.onNext(buildErrorResponse("Invalid request: symbol is required", IndicatorType.BOLL));
                responseObserver.onCompleted();
                return;
            }
            
            IndicatorResult result = calculationEngine.calculateBOLL(
                    request.getBase().getSymbol(),
                    request.getBase().getInterval(),
                    request.getPeriod(),
                    request.getStdDev(),
                    request.getBase().getLimit()
            );
            
            responseObserver.onNext(buildBOLLResponse(request.getBase(), result));
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            log.error("Failed to calculate BOLL", e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Internal error: " + e.getMessage())
                    .asRuntimeException());
        }
    }
    
    /**
     * Calculate Bollinger Bands for multiple requests with streaming
     */
    @Override
    public void calculateBOLLBatch(BatchBOLLRequest request, StreamObserver<IndicatorResponse> responseObserver) {
        try {
            log.debug("gRPC calculateBOLLBatch: {} requests", request.getRequestsCount());
            
            for (BOLLRequest bollRequest : request.getRequestsList()) {
                try {
                    IndicatorResult result = calculationEngine.calculateBOLL(
                            bollRequest.getBase().getSymbol(),
                            bollRequest.getBase().getInterval(),
                            bollRequest.getPeriod(),
                            bollRequest.getStdDev(),
                            bollRequest.getBase().getLimit()
                    );
                    
                    responseObserver.onNext(buildBOLLResponse(bollRequest.getBase(), result));
                    
                } catch (Exception e) {
                    log.error("Failed to calculate BOLL for symbol: {}", bollRequest.getBase().getSymbol(), e);
                    responseObserver.onNext(buildErrorResponse(
                            "Failed: " + e.getMessage(), IndicatorType.BOLL));
                }
            }
            
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            log.error("Failed to calculate BOLL batch", e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Batch error: " + e.getMessage())
                    .asRuntimeException());
        }
    }
    
    /**
     * Calculate MACD
     */
    @Override
    public void calculateMACD(MACDRequest request, StreamObserver<IndicatorResponse> responseObserver) {
        try {
            log.debug("gRPC calculateMACD: symbol={}, interval={}, fast={}, slow={}, signal={}", 
                    request.getBase().getSymbol(), request.getBase().getInterval(),
                    request.getFastPeriod(), request.getSlowPeriod(), request.getSignalPeriod());
            
            if (!request.hasBase() || request.getBase().getSymbol().isEmpty()) {
                responseObserver.onNext(buildErrorResponse("Invalid request: symbol is required", IndicatorType.MACD));
                responseObserver.onCompleted();
                return;
            }
            
            IndicatorResult result = calculationEngine.calculateMACD(
                    request.getBase().getSymbol(),
                    request.getBase().getInterval(),
                    request.getFastPeriod(),
                    request.getSlowPeriod(),
                    request.getSignalPeriod(),
                    request.getBase().getLimit()
            );
            
            responseObserver.onNext(buildMACDResponse(request.getBase(), result));
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            log.error("Failed to calculate MACD", e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Internal error: " + e.getMessage())
                    .asRuntimeException());
        }
    }
    
    /**
     * Calculate MACD for multiple requests with streaming
     */
    @Override
    public void calculateMACDBatch(BatchMACDRequest request, StreamObserver<IndicatorResponse> responseObserver) {
        try {
            log.debug("gRPC calculateMACDBatch: {} requests", request.getRequestsCount());
            
            for (MACDRequest macdRequest : request.getRequestsList()) {
                try {
                    IndicatorResult result = calculationEngine.calculateMACD(
                            macdRequest.getBase().getSymbol(),
                            macdRequest.getBase().getInterval(),
                            macdRequest.getFastPeriod(),
                            macdRequest.getSlowPeriod(),
                            macdRequest.getSignalPeriod(),
                            macdRequest.getBase().getLimit()
                    );
                    
                    responseObserver.onNext(buildMACDResponse(macdRequest.getBase(), result));
                    
                } catch (Exception e) {
                    log.error("Failed to calculate MACD for symbol: {}", macdRequest.getBase().getSymbol(), e);
                    responseObserver.onNext(buildErrorResponse(
                            "Failed: " + e.getMessage(), IndicatorType.MACD));
                }
            }
            
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            log.error("Failed to calculate MACD batch", e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Batch error: " + e.getMessage())
                    .asRuntimeException());
        }
    }
    
    /**
     * Calculate Pinbar pattern
     */
    @Override
    public void calculatePinbar(PinbarRequest request, StreamObserver<IndicatorResponse> responseObserver) {
        try {
            log.debug("gRPC calculatePinbar: symbol={}, interval={}, bodyRatio={}, wickRatio={}", 
                    request.getBase().getSymbol(), request.getBase().getInterval(),
                    request.getBodyRatioThreshold(), request.getWickRatioThreshold());
            
            if (!request.hasBase() || request.getBase().getSymbol().isEmpty()) {
                responseObserver.onNext(buildErrorResponse("Invalid request: symbol is required", IndicatorType.PINBAR));
                responseObserver.onCompleted();
                return;
            }
            
            IndicatorResult result = calculationEngine.calculatePinbar(
                    request.getBase().getSymbol(),
                    request.getBase().getInterval(),
                    request.getBodyRatioThreshold(),
                    request.getWickRatioThreshold(),
                    request.getBase().getLimit()
            );
            
            responseObserver.onNext(buildPinbarResponse(request.getBase(), result));
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            log.error("Failed to calculate Pinbar", e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Internal error: " + e.getMessage())
                    .asRuntimeException());
        }
    }
    
    /**
     * Calculate Pinbar for multiple requests with streaming
     */
    @Override
    public void calculatePinbarBatch(BatchPinbarRequest request, StreamObserver<IndicatorResponse> responseObserver) {
        try {
            log.debug("gRPC calculatePinbarBatch: {} requests", request.getRequestsCount());
            
            for (PinbarRequest pinbarRequest : request.getRequestsList()) {
                try {
                    IndicatorResult result = calculationEngine.calculatePinbar(
                            pinbarRequest.getBase().getSymbol(),
                            pinbarRequest.getBase().getInterval(),
                            pinbarRequest.getBodyRatioThreshold(),
                            pinbarRequest.getWickRatioThreshold(),
                            pinbarRequest.getBase().getLimit()
                    );
                    
                    responseObserver.onNext(buildPinbarResponse(pinbarRequest.getBase(), result));
                    
                } catch (Exception e) {
                    log.error("Failed to calculate Pinbar for symbol: {}", pinbarRequest.getBase().getSymbol(), e);
                    responseObserver.onNext(buildErrorResponse(
                            "Failed: " + e.getMessage(), IndicatorType.PINBAR));
                }
            }
            
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            log.error("Failed to calculate Pinbar batch", e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Batch error: " + e.getMessage())
                    .asRuntimeException());
        }
    }
    
    /**
     * Stream real-time indicators
     * Note: This is a placeholder implementation. In production, this would connect
     * to a real-time data stream and calculate indicators continuously.
     */
    @Override
    public void streamIndicators(StreamRequest request, StreamObserver<IndicatorResponse> responseObserver) {
        try {
            log.debug("gRPC streamIndicators: symbol={}, interval={}, indicators={}", 
                    request.getSymbol(), request.getInterval(), request.getIndicatorsList());
            
            // For now, calculate once for each requested indicator type
            // In production, this would be a continuous stream
            for (IndicatorType indicatorType : request.getIndicatorsList()) {
                try {
                    IndicatorResult result = null;
                    
                    switch (indicatorType) {
                        case RSI:
                            result = calculationEngine.calculateRSI(
                                    request.getSymbol(), request.getInterval(), 14, 100);
                            responseObserver.onNext(buildRSIResponse(
                                    BaseRequest.newBuilder()
                                            .setSymbol(request.getSymbol())
                                            .setInterval(request.getInterval())
                                            .setLimit(100)
                                            .build(),
                                    result));
                            break;
                            
                        case BOLL:
                            result = calculationEngine.calculateBOLL(
                                    request.getSymbol(), request.getInterval(), 20, 2.0, 100);
                            responseObserver.onNext(buildBOLLResponse(
                                    BaseRequest.newBuilder()
                                            .setSymbol(request.getSymbol())
                                            .setInterval(request.getInterval())
                                            .setLimit(100)
                                            .build(),
                                    result));
                            break;
                            
                        case MACD:
                            result = calculationEngine.calculateMACD(
                                    request.getSymbol(), request.getInterval(), 12, 26, 9, 100);
                            responseObserver.onNext(buildMACDResponse(
                                    BaseRequest.newBuilder()
                                            .setSymbol(request.getSymbol())
                                            .setInterval(request.getInterval())
                                            .setLimit(100)
                                            .build(),
                                    result));
                            break;
                            
                        case PINBAR:
                            result = calculationEngine.calculatePinbar(
                                    request.getSymbol(), request.getInterval(), 0.2, 0.6, 10);
                            responseObserver.onNext(buildPinbarResponse(
                                    BaseRequest.newBuilder()
                                            .setSymbol(request.getSymbol())
                                            .setInterval(request.getInterval())
                                            .setLimit(10)
                                            .build(),
                                    result));
                            break;
                            
                        default:
                            log.warn("Unknown indicator type: {}", indicatorType);
                    }
                    
                } catch (Exception e) {
                    log.error("Failed to calculate indicator: {}", indicatorType, e);
                    responseObserver.onNext(buildErrorResponse(
                            "Failed: " + e.getMessage(), indicatorType));
                }
            }
            
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            log.error("Failed to stream indicators", e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Stream error: " + e.getMessage())
                    .asRuntimeException());
        }
    }
    
    // ==================== Response Builders ====================
    
    private IndicatorResponse buildRSIResponse(BaseRequest base, IndicatorResult result) {
        IndicatorResponse.Builder builder = IndicatorResponse.newBuilder()
                .setSuccess(result.getValue() != null)
                .setSymbol(base.getSymbol())
                .setIndicatorType(IndicatorType.RSI)
                .setInterval(base.getInterval())
                .setTimestamp(Instant.now().toEpochMilli())
                .setDataPoints(result.getDataPoints() != null ? result.getDataPoints() : 0)
                .setFromCache(false); // TODO: Get from result
        
        if (result.getValue() != null) {
            builder.setRsiValue(result.getValue());
        } else {
            builder.setErrorMessage("Calculation failed");
        }
        
        return builder.build();
    }
    
    private IndicatorResponse buildBOLLResponse(BaseRequest base, IndicatorResult result) {
        IndicatorResponse.Builder builder = IndicatorResponse.newBuilder()
                .setSuccess(result.getValue() != null)
                .setSymbol(base.getSymbol())
                .setIndicatorType(IndicatorType.BOLL)
                .setInterval(base.getInterval())
                .setTimestamp(Instant.now().toEpochMilli())
                .setDataPoints(result.getDataPoints() != null ? result.getDataPoints() : 0)
                .setFromCache(false);
        
        if (result.getValue() != null && result.getValues() != null) {
            builder.setBollUpper(result.getValues().getOrDefault("upper", 0.0));
            builder.setBollMiddle(result.getValue());
            builder.setBollLower(result.getValues().getOrDefault("lower", 0.0));
        } else {
            builder.setErrorMessage("Calculation failed");
        }
        
        return builder.build();
    }
    
    private IndicatorResponse buildMACDResponse(BaseRequest base, IndicatorResult result) {
        IndicatorResponse.Builder builder = IndicatorResponse.newBuilder()
                .setSuccess(result.getValue() != null)
                .setSymbol(base.getSymbol())
                .setIndicatorType(IndicatorType.MACD)
                .setInterval(base.getInterval())
                .setTimestamp(Instant.now().toEpochMilli())
                .setDataPoints(result.getDataPoints() != null ? result.getDataPoints() : 0)
                .setFromCache(false);
        
        if (result.getValue() != null && result.getValues() != null) {
            builder.setMacdLine(result.getValue());
            builder.setMacdSignal(result.getValues().getOrDefault("signal", 0.0));
            builder.setMacdHistogram(result.getValues().getOrDefault("histogram", 0.0));
        } else {
            builder.setErrorMessage("Calculation failed");
        }
        
        return builder.build();
    }
    
    private IndicatorResponse buildPinbarResponse(BaseRequest base, IndicatorResult result) {
        IndicatorResponse.Builder builder = IndicatorResponse.newBuilder()
                .setSuccess(result.getValue() != null)
                .setSymbol(base.getSymbol())
                .setIndicatorType(IndicatorType.PINBAR)
                .setInterval(base.getInterval())
                .setTimestamp(Instant.now().toEpochMilli())
                .setDataPoints(result.getDataPoints() != null ? result.getDataPoints() : 0)
                .setFromCache(false);
        
        if (result.getValue() != null && result.getValues() != null) {
            builder.setIsPinbar(result.getValue() == 1.0);
            builder.setIsBullish(result.getValues().getOrDefault("is_bullish", 0.0) == 1.0);
            builder.setBodyRatio(result.getValues().getOrDefault("body_ratio", 0.0));
            builder.setUpperWickRatio(result.getValues().getOrDefault("upper_wick_ratio", 0.0));
            builder.setLowerWickRatio(result.getValues().getOrDefault("lower_wick_ratio", 0.0));
        } else {
            builder.setErrorMessage("Calculation failed");
        }
        
        return builder.build();
    }
    
    private IndicatorResponse buildErrorResponse(String errorMessage, IndicatorType type) {
        return IndicatorResponse.newBuilder()
                .setSuccess(false)
                .setErrorMessage(errorMessage)
                .setIndicatorType(type)
                .setTimestamp(Instant.now().toEpochMilli())
                .setDataPoints(0)
                .setFromCache(false)
                .build();
    }
}
