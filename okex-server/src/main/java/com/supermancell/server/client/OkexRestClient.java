package com.supermancell.server.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supermancell.common.model.Candle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * OKEx REST API client for fetching candle data
 * API Endpoint: GET /api/v5/market/candles
 */
@Component
public class OkexRestClient {
    
    private static final Logger log = LoggerFactory.getLogger(OkexRestClient.class);
    
    private final ObjectMapper objectMapper;
    
    @Value("${okex.rest.api.url:https://www.okx.com}")
    private String okexRestApiUrl;
    
    @Value("${okex.rest.api.timeout:10000}")
    private int connectionTimeout;
    
    public OkexRestClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    /**
     * Fetch candle data from OKEx REST API
     * 
     * @param symbol Trading pair (e.g., BTC-USDT-SWAP)
     * @param interval Time interval (e.g., 1m, 1H)
     * @param limit Number of candles to fetch (max 300)
     * @return List of candles in chronological order (oldest first)
     */
    public List<Candle> getCandles(String symbol, String interval, int limit) {
        List<Candle> candles = new ArrayList<>();
        
        try {
            // Validate and limit parameters
            if (limit > 300) {
                log.warn("Limit {} exceeds maximum 300, using 300 instead", limit);
                limit = 300;
            }
            
            // Convert interval to OKEx bar format
            String bar = convertIntervalToBar(interval);
            
            // Build request URL
            String urlStr = String.format("%s/api/v5/market/candles?instId=%s&bar=%s&limit=%d",
                    okexRestApiUrl, symbol, bar, limit);
            
            log.info("Fetching candles from OKEx REST API: symbol={}, interval={}, limit={}", 
                    symbol, interval, limit);
            
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(connectionTimeout);
            conn.setReadTimeout(connectionTimeout);
            conn.setRequestProperty("Content-Type", "application/json");
            
            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                log.error("OKEx REST API returned error code: {}", responseCode);
                return candles;
            }
            
            // Read response
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            
            // Parse JSON response
            JsonNode root = objectMapper.readTree(response.toString());
            String code = root.path("code").asText();
            
            if (!"0".equals(code)) {
                String msg = root.path("msg").asText();
                log.error("OKEx REST API returned error: code={}, msg={}", code, msg);
                return candles;
            }
            
            // Parse candle data
            JsonNode dataArray = root.path("data");
            if (dataArray.isArray()) {
                for (JsonNode candleNode : dataArray) {
                    Candle candle = parseCandle(candleNode, symbol, interval);
                    if (candle != null) {
                        candles.add(candle);
                    }
                }
            }
            
            // Reverse to get chronological order (oldest first)
            // OKEx returns data in reverse chronological order (newest first)
            java.util.Collections.reverse(candles);
            
            log.info("Successfully fetched {} candles from OKEx REST API", candles.size());
            
        } catch (Exception e) {
            log.error("Failed to fetch candles from OKEx REST API", e);
        }
        
        return candles;
    }
    
    /**
     * Parse single candle from JSON array
     * OKEx candle format: [timestamp, open, high, low, close, volume, volCcy, volCcyQuote, confirm]
     * 
     * @param candleNode JSON array node
     * @param symbol Trading pair
     * @param interval Time interval
     * @return Candle object or null if parsing fails
     */
    private Candle parseCandle(JsonNode candleNode, String symbol, String interval) {
        try {
            if (!candleNode.isArray() || candleNode.size() < 9) {
                log.warn("Invalid candle data format: {}", candleNode);
                return null;
            }
            
            // Parse timestamp (milliseconds)
            long timestampMs = candleNode.get(0).asLong();
            Instant timestamp = Instant.ofEpochMilli(timestampMs);
            
            // Parse OHLCV data
            double open = candleNode.get(1).asDouble();
            double high = candleNode.get(2).asDouble();
            double low = candleNode.get(3).asDouble();
            double close = candleNode.get(4).asDouble();
            double volume = candleNode.get(5).asDouble();
            
            // Parse confirm flag
            String confirm = candleNode.get(8).asText();
            
            // Create Candle object
            Candle candle = new Candle();
            candle.setSymbol(symbol);
            candle.setTimestamp(timestamp);
            candle.setInterval(interval);
            candle.setOpen(open);
            candle.setHigh(high);
            candle.setLow(low);
            candle.setClose(close);
            candle.setVolume(volume);
            candle.setConfirm(confirm);
            candle.setCreatedAt(Instant.now());
            
            return candle;
            
        } catch (Exception e) {
            log.error("Failed to parse candle data", e);
            return null;
        }
    }
    
    /**
     * Convert system interval format to OKEx bar format
     * System format: 1m, 5m, 15m, 1H, 4H, 1D
     * OKEx format: 1m, 5m, 15m, 1H, 4H, 1D (same format)
     * 
     * @param interval System interval
     * @return OKEx bar format
     */
    private String convertIntervalToBar(String interval) {
        // Currently, our system interval format matches OKEx bar format
        // This method exists for future compatibility if formats diverge
        return interval;
    }
}
