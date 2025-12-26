package com.supermancell.server.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supermancell.common.model.Candle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class OkexMessageParser {

    private static final Logger log = LoggerFactory.getLogger(OkexMessageParser.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Parse OKEx WebSocket message and extract single candle.
     * 
     * OKEx message format (array data):
     * {
     *   "arg": {
     *     "channel": "candle1m",
     *     "instId": "BTC-USDT-SWAP"
     *   },
     *   "data": [
     *     [
     *       "1703505600000",
     *       "42000",
     *       "42100",
     *       "41950",
     *       "42050",
     *       "1250.8",
     *       "1250.8",
     *       "52650400",
     *       "1"
     *     ]
     *   ]
     * }
     * 
     * Data array format: [ts, o, h, l, c, vol, volCcy, volCcyQuote, confirm]
     * 
     * @param message Raw WebSocket message
     * @return Parsed Candle object, null if parsing fails or no data
     */
    public Candle parseCandle(String message) {
        
        try {
            JsonNode root = objectMapper.readTree(message);
            
            // Check if this is a candle data message
            if (!root.has("arg") || !root.has("data")) {
                return null;
            }
            
            JsonNode arg = root.get("arg");
            if (!arg.has("channel") || !arg.has("instId")) {
                return null;
            }
            
            String channel = arg.get("channel").asText();
            if (!channel.startsWith("candle")) {
                return null;
            }
            
            String symbol = arg.get("instId").asText();
            String interval = extractInterval(channel);
            
            JsonNode dataArray = root.get("data");
            if (!dataArray.isArray() || dataArray.size() == 0) {
                return null;
            }
            
            // OKEx always sends only one candle record per message
            JsonNode data = dataArray.get(0);
            return parseCandleArray(data, symbol, interval);
            
        } catch (Exception e) {
            log.error("Failed to parse OKEx message: {}", message, e);
            return null;
        }
    }

    /**
     * Parse candle from array format: [ts, o, h, l, c, vol, volCcy, volCcyQuote, confirm]
     */
    private Candle parseCandleArray(JsonNode data, String symbol, String interval) {
        if (!data.isArray() || data.size() < 6) {
            return null;
        }

        Candle candle = new Candle();
        candle.setSymbol(symbol);
        candle.setInterval(interval);
        
        // Parse timestamp (milliseconds)
        long timestampMs = data.get(0).asLong();
        candle.setTimestamp(Instant.ofEpochMilli(timestampMs));
        
        // Parse OHLCV data
        candle.setOpen(parseDouble(data.get(1).asText()));
        candle.setHigh(parseDouble(data.get(2).asText()));
        candle.setLow(parseDouble(data.get(3).asText()));
        candle.setClose(parseDouble(data.get(4).asText()));
        candle.setVolume(parseDouble(data.get(5).asText()));
        candle.setConfirm(data.get(8).asText());
        candle.setCreatedAt(Instant.now());
        
        return candle;
    }

    /**
     * Extract interval from channel name.
     * Examples: candle1m -> 1m, candle1H -> 1H
     */
    private String extractInterval(String channel) {
        if (channel.startsWith("candle")) {
            return channel.substring(6); // Remove "candle" prefix
        }
        return channel;
    }

    private double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            log.warn("Failed to parse double value: {}", value);
            return 0.0;
        }
    }
}
