package com.supermancell.server.controller;

import com.supermancell.common.model.Candle;
import com.supermancell.server.dto.ApiResponse;
import com.supermancell.server.repository.CandleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for accessing candlestick (candle) data.
 * <p>
 * This endpoint is primarily used by the okex-dashboard to fetch the
 * latest candles for a given symbol and interval (e.g. for the last 300 candles
 * when clicking from the Active Subscriptions list).
 *
 * The underlying data retrieval goes through {@link CandleRepository}, which is
 * wrapped by {@link com.supermancell.server.aspect.CandleDataIntegrityAspect}.
 * That aspect performs data integrity checks and uses Redis via
 * {@link com.supermancell.server.cache.CandleCacheService}, so this API shares
 * the same Redis caching mechanism as the AOP integrity checks.
 */
@RestController
@RequestMapping("/api/candles")
public class CandleController {

    private static final Logger log = LoggerFactory.getLogger(CandleController.class);

    private final CandleRepository candleRepository;

    public CandleController(CandleRepository candleRepository) {
        this.candleRepository = candleRepository;
    }

    /**
     * Get recent candle data for a given symbol and interval.
     *
     * <p>HTTP: GET /api/candles?symbol=BTC-USDT-SWAP&interval=1m&limit=300</p>
     *
     * @param symbol   Trading pair (e.g. BTC-USDT-SWAP)
     * @param interval Time interval (e.g. 1m, 1H)
     * @param limit    Optional number of records to return (default 300, max 300)
     * @return ApiResponse wrapping a list of {@link Candle} objects in
     * chronological order (oldest first)
     */
    @GetMapping
    public ApiResponse<List<Candle>> getCandles(
            @RequestParam("symbol") String symbol,
            @RequestParam("interval") String interval,
            @RequestParam(name = "limit", defaultValue = "300") int limit
    ) {
        try {
            // Basic validation
            if (symbol == null || symbol.trim().isEmpty()) {
                return ApiResponse.error("Parameter 'symbol' must not be empty");
            }
            if (interval == null || interval.trim().isEmpty()) {
                return ApiResponse.error("Parameter 'interval' must not be empty");
            }
            if (limit <= 0) {
                return ApiResponse.error("Parameter 'limit' must be greater than 0");
            }

            // Enforce maximum of 300, as documented
            if (limit > 300) {
                log.debug("Requested limit {} exceeds maximum 300, capping to 300", limit);
                limit = 300;
            }

            String trimmedSymbol = symbol.trim();
            String trimmedInterval = interval.trim();

            log.debug("Fetching candles for symbol={}, interval={}, limit={}",
                    trimmedSymbol, trimmedInterval, limit);

            // This call is intercepted by CandleDataIntegrityAspect, which will
            // perform data integrity checks and use Redis cache (CandleCacheService).
            List<Candle> candles = candleRepository.findCandles(trimmedSymbol, trimmedInterval, limit);

            log.debug("Fetched {} candles for symbol={}, interval={}",
                    candles != null ? candles.size() : 0, trimmedSymbol, trimmedInterval);

            return ApiResponse.success(candles);
        } catch (Exception e) {
            log.error("Failed to fetch candles", e);
            return ApiResponse.error("Failed to fetch candles: " + e.getMessage());
        }
    }
}
