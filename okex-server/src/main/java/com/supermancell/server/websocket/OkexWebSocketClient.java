package com.supermancell.server.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.supermancell.common.model.Candle;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.client.WebSocketClient;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OkexWebSocketClient {

    private static final Logger log = LoggerFactory.getLogger(OkexWebSocketClient.class);

    private final WebSocketClient webSocketClient = new StandardWebSocketClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SubscriptionConfigLoader subscriptionConfigLoader;
    private final OkexMessageParser messageParser;
    private final CandleBatchWriter candleBatchWriter;

    @Value("${websocket.okex.url}")
    private String okexWebSocketUrl;

    @Value("${websocket.okex.proxy.enabled:true}")
    private boolean useProxy;

    @Value("${websocket.okex.proxy.type:socks5}")
    private String proxyType;

    @Value("${websocket.okex.proxy.host:127.0.0.1}")
    private String proxyHost;

    @Value("${websocket.okex.proxy.port:4781}")
    private int proxyPort;

    private volatile WebSocketSession session;
    private volatile SubscriptionConfig currentConfig;

    @PostConstruct
    public void init() {
        // 启动时加载一次配置并建立订阅
        SubscriptionConfig config = subscriptionConfigLoader.loadCurrentConfig();
        if (config != null) {
            applySubscriptions(config);
        }
    }

    /**
     * 每次配置变化时调用，自动对比差异并发送订阅/取消订阅消息。
     */
    public synchronized void applySubscriptions(SubscriptionConfig newConfig) {
        Objects.requireNonNull(newConfig, "newConfig must not be null");
        ensureConnected();

        if (currentConfig == null) {
            // 首次订阅
            sendSubscribeMessages(buildAllPairs(newConfig));
            currentConfig = newConfig;
            log.info("Initial subscriptions applied: {}", newConfig);
            return;
        }

        Set<SubscriptionPair> oldSet = buildAllPairs(currentConfig);
        Set<SubscriptionPair> newSet = buildAllPairs(newConfig);

        Set<SubscriptionPair> toUnsubscribe = new HashSet<>(oldSet);
        toUnsubscribe.removeAll(newSet);

        Set<SubscriptionPair> toSubscribe = new HashSet<>(newSet);
        toSubscribe.removeAll(oldSet);

        if (!toUnsubscribe.isEmpty()) {
            sendUnsubscribeMessages(toUnsubscribe);
        }
        if (!toSubscribe.isEmpty()) {
            sendSubscribeMessages(toSubscribe);
        }

        currentConfig = newConfig;
        log.info("Updated subscriptions. New config: {}", newConfig);
    }

    private void ensureConnected() {
        configureProxyIfNeeded();
        try {
            if (session != null && session.isOpen()) {
                return;
            }
            WebSocketHandler handler = new OkexWebSocketHandler();
            this.session = webSocketClient.doHandshake(handler, okexWebSocketUrl).get();
            log.info("Connected to OKEx WebSocket: {}", okexWebSocketUrl);
        } catch (Exception e) {
            log.error("Failed to connect to OKEx WebSocket", e);
            throw new IllegalStateException("Cannot connect to OKEx WebSocket", e);
        }
    }

    private void configureProxyIfNeeded() {
        if (!useProxy) {
            System.clearProperty("socksProxyHost");
            System.clearProperty("socksProxyPort");
            log.info("WebSocket proxy disabled");
            return;
        }

        if (!"socks5".equalsIgnoreCase(proxyType) && !"socks".equalsIgnoreCase(proxyType)) {
            log.warn("Unsupported proxy type {} for WebSocket, proxy will not be used", proxyType);
            System.clearProperty("socksProxyHost");
            System.clearProperty("socksProxyPort");
            return;
        }

        System.setProperty("socksProxyHost", proxyHost);
        System.setProperty("socksProxyPort", String.valueOf(proxyPort));
        log.info("WebSocket proxy enabled: {}://{}:{}", proxyType, proxyHost, proxyPort);
    }

    private Set<SubscriptionPair> buildAllPairs(SubscriptionConfig config) {
        Set<SubscriptionPair> result = new HashSet<>();
        List<String> symbols = config.getSymbols();
        List<String> intervals = config.getIntervals();
        for (String symbol : symbols) {
            for (String interval : intervals) {
                result.add(new SubscriptionPair(symbol, interval));
            }
        }
        return result;
    }

    private void sendSubscribeMessages(Set<SubscriptionPair> subscriptions) {
        if (subscriptions.isEmpty()) {
            return;
        }
        sendOpWithArgs("subscribe", subscriptions);
    }

    private void sendUnsubscribeMessages(Set<SubscriptionPair> subscriptions) {
        if (subscriptions.isEmpty()) {
            return;
        }
        sendOpWithArgs("unsubscribe", subscriptions);
    }

    private void sendOpWithArgs(String op, Set<SubscriptionPair> subscriptions) {
        try {
            WebSocketSession currentSession = this.session;
            if (currentSession == null || !currentSession.isOpen()) {
                log.warn("WebSocket session is not open, cannot send {} message", op);
                return;
            }
            JsonNode root = buildSubscriptionMessage(op, subscriptions);
            String payload = objectMapper.writeValueAsString(root);
            currentSession.sendMessage(new TextMessage(payload));
            log.info("Sent {} message: {}", op, payload);
        } catch (IOException e) {
            log.error("Failed to send {} message", op, e);
        }
    }

    private JsonNode buildSubscriptionMessage(String op, Set<SubscriptionPair> subscriptions) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("op", op);
        ArrayNode argsArray = objectMapper.createArrayNode();
        for (SubscriptionPair pair : subscriptions) {
            String channel = "candle" + pair.interval;
            ObjectNode arg = objectMapper.createObjectNode();
            arg.put("channel", channel);
            arg.put("instId", pair.symbol);
            argsArray.add(arg);
        }
        root.set("args", argsArray);
        return root;
    }

    private static class SubscriptionPair {
        private final String symbol;
        private final String interval;

        SubscriptionPair(String symbol, String interval) {
            this.symbol = symbol;
            this.interval = interval;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SubscriptionPair that = (SubscriptionPair) o;
            return Objects.equals(symbol, that.symbol) && Objects.equals(interval, that.interval);
        }

        @Override
        public int hashCode() {
            return Objects.hash(symbol, interval);
        }

        @Override
        public String toString() {
            return "SubscriptionPair{" +
                    "symbol='" + symbol + '\'' +
                    ", interval='" + interval + '\'' +
                    '}';
        }
    }

    /**
     * WebSocketHandler that parses candle messages and saves to MongoDB.
     */
    private class OkexWebSocketHandler implements WebSocketHandler {

        @Override
        public void afterConnectionEstablished(WebSocketSession session) {
            log.info("OKEx WebSocket connection established");
        }

        @Override
        public void handleMessage(WebSocketSession session, org.springframework.web.socket.WebSocketMessage<?> message) {
            if (message instanceof TextMessage) {
                String payload = ((TextMessage) message).getPayload();
                log.debug("Received message: {}", payload);

                // Parse and buffer candles for batch write
                try {
                    Candle candle = messageParser.parseCandle(payload);
                    if (candle != null) {
                        candleBatchWriter.addCandle(candle);
                    }
                } catch (Exception e) {
                    log.error("Failed to parse candle from message: {}", payload, e);
                }
            } else {
                log.debug("Received non-text message: {}", message);
            }
        }

        @Override
        public void handleTransportError(WebSocketSession session, Throwable exception) {
            log.error("WebSocket transport error", exception);
        }

        @Override
        public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus closeStatus) {
            log.info("WebSocket connection closed: {}", closeStatus);
        }

        @Override
        public boolean supportsPartialMessages() {
            return false;
        }
    }
}
