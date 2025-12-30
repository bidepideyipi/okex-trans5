package com.supermancell.server.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.supermancell.common.model.Candle;
import com.supermancell.server.service.SystemMetricsService;
import com.supermancell.server.service.WebSocketStatusService;
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
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
public class OkexWebSocketClient {

    private static final Logger log = LoggerFactory.getLogger(OkexWebSocketClient.class);

    private final WebSocketClient webSocketClient;
    private final ObjectMapper objectMapper;
    private final SubscriptionConfigLoader subscriptionConfigLoader;
    private final OkexMessageParser messageParser;
    private final CandleBatchWriter candleBatchWriter;
    private final WebSocketStatusService statusService;
    private final SystemMetricsService metricsService;
    
    // Explicit constructor
    public OkexWebSocketClient(
            SubscriptionConfigLoader subscriptionConfigLoader,
            OkexMessageParser messageParser,
            CandleBatchWriter candleBatchWriter,
            WebSocketStatusService statusService,
            SystemMetricsService metricsService) {
        this.webSocketClient = new StandardWebSocketClient();
        this.objectMapper = new ObjectMapper();
        this.subscriptionConfigLoader = subscriptionConfigLoader;
        this.messageParser = messageParser;
        this.candleBatchWriter = candleBatchWriter;
        this.statusService = statusService;
        this.metricsService = metricsService;
    }

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

    @Value("${websocket.okex.initialReconnectInterval:1000}")
    private long initialReconnectIntervalMs;

    @Value("${websocket.okex.maxReconnectAttempts:10}")
    private int maxReconnectAttempts;

    @Value("${websocket.okex.heartbeat_interval:30000}")
    private long heartbeatIntervalMs;

    @Value("${websocket.okex.heartbeatTimeout:60000}")
    private long heartbeatTimeoutMs;

    private volatile WebSocketSession session;
    private volatile SubscriptionConfig currentConfig;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private volatile long lastMessageTimestamp = System.currentTimeMillis();
    private volatile int reconnectAttempts = 0;
    private volatile boolean heartbeatStarted = false;

    @PostConstruct
    public void init() {
        // Initialize status service
        statusService.updateUrl(okexWebSocketUrl);
        statusService.updateConnectionStatus("DISCONNECTED");
        
        // Start heartbeat monitoring
        startHeartbeat();
        
        // Asynchronously attempt initial connection and subscription
        // Don't block application startup if WebSocket connection fails
        scheduler.execute(() -> {
            try {
                Thread.sleep(1000); // Give application time to fully initialize
                SubscriptionConfig config = subscriptionConfigLoader.loadCurrentConfig();
                if (config != null) {
                    applySubscriptions(config);
                }
            } catch (Exception e) {
                log.warn("Initial WebSocket connection failed, will retry automatically", e);
                // The reconnect mechanism will handle retries
            }
        });
    }

    @PreDestroy
    public void shutdown() {
        scheduler.shutdownNow();
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
            
            statusService.updateConnectionStatus("CONNECTING");
            
            WebSocketHandler handler = new OkexWebSocketHandler();
            this.session = webSocketClient.doHandshake(handler, okexWebSocketUrl).get();
            log.info("Connected to OKEx WebSocket: {}", okexWebSocketUrl);
            onConnected();
        } catch (Exception e) {
            log.error("Failed to connect to OKEx WebSocket", e);
            statusService.updateConnectionStatus("ERROR");
            scheduleReconnect();
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

    private void onConnected() {
        reconnectAttempts = 0;
        lastMessageTimestamp = System.currentTimeMillis();
        
        // Update status service
        statusService.updateConnectionStatus("CONNECTED");
        statusService.updateReconnectAttempts(0);
        
        if (currentConfig != null) {
            // After reconnect, re-subscribe all current pairs
            sendSubscribeMessages(buildAllPairs(currentConfig));
        }
    }

    private void onConnectionLost(String reason, Throwable exception) {
        log.warn("WebSocket connection lost: {}", reason, exception);
        
        // Update status service
        statusService.updateConnectionStatus("DISCONNECTED");
        
        WebSocketSession currentSession = this.session;
        if (currentSession != null) {
            try {
                currentSession.close();
            } catch (IOException e) {
                log.warn("Error while closing WebSocket session", e);
            }
        }
        this.session = null;
        scheduleReconnect();
    }

    private void startHeartbeat() {
        if (heartbeatStarted) {
            return;
        }
        heartbeatStarted = true;
        scheduler.scheduleAtFixedRate(() -> {
            try {
                WebSocketSession currentSession = this.session;
                if (currentSession == null || !currentSession.isOpen()) {
                    return;
                }
                // Send OKEx heartbeat ping
                try {
                    currentSession.sendMessage(new TextMessage("ping"));
                    log.debug("Sent ping to OKEx WebSocket");
                } catch (IOException e) {
                    log.warn("Failed to send ping to OKEx WebSocket", e);
                }

                long now = System.currentTimeMillis();
                if (now - lastMessageTimestamp > heartbeatTimeoutMs) {
                    log.warn("WebSocket heartbeat timeout, triggering reconnect");
                    onConnectionLost("heartbeat-timeout", null);
                }
            } catch (Exception e) {
                log.error("Heartbeat check failed", e);
            }
        }, heartbeatIntervalMs, heartbeatIntervalMs, TimeUnit.MILLISECONDS);
    }

    private void scheduleReconnect() {
        if (maxReconnectAttempts <= 0) {
            return;
        }
        reconnectAttempts++;
        if (reconnectAttempts > maxReconnectAttempts) {
            log.error("Max reconnect attempts ({}) exceeded, will not attempt further reconnects", maxReconnectAttempts);
            statusService.updateConnectionStatus("ERROR");
            return;
        }
        long fib = fibonacci(reconnectAttempts);
        long delay = initialReconnectIntervalMs * fib;
        log.warn("Scheduling reconnect attempt {} in {} ms (fib={} )", reconnectAttempts, delay, fib);
        
        // Update status service
        statusService.updateConnectionStatus("RECONNECTING");
        statusService.updateReconnectAttempts(reconnectAttempts);
        statusService.updateCurrentReconnectDelay(delay);
        
        // Track reconnection start time
        long reconnectStartTime = System.currentTimeMillis();
        
        scheduler.schedule(() -> {
            try {
                ensureConnected();
                // Record successful reconnection
                long duration = System.currentTimeMillis() - reconnectStartTime;
                statusService.recordReconnection(
                    "scheduled-reconnect",
                    reconnectAttempts,
                    true,
                    duration,
                    null
                );
            } catch (Exception e) {
                log.error("Reconnect attempt {} failed", reconnectAttempts, e);
                // Record failed reconnection
                statusService.recordReconnection(
                    "scheduled-reconnect",
                    reconnectAttempts,
                    false,
                    null,
                    e.getMessage()
                );
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    private long fibonacci(int n) {
        if (n <= 1) {
            return 1;
        }
        long a = 1;
        long b = 1;
        for (int i = 2; i <= n; i++) {
            long c = a + b;
            a = b;
            b = c;
        }
        return b;
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
            lastMessageTimestamp = System.currentTimeMillis();
            
            // Update status service
            statusService.updateConnectionStatus("CONNECTED");
            statusService.updateLastMessageTime();
        }

        @Override
        public void handleMessage(WebSocketSession session, org.springframework.web.socket.WebSocketMessage<?> message) {
            if (message instanceof TextMessage) {
                String payload = ((TextMessage) message).getPayload();
                log.debug("Received message: {}", payload);
                lastMessageTimestamp = System.currentTimeMillis();
                
                // Update metrics and status
                statusService.updateLastMessageTime();
                metricsService.incrementMessageCount();
                metricsService.recordDataProcessed(payload.length());

                // OKEx heartbeat pong handling
                if ("pong".equalsIgnoreCase(payload.trim())) {
                    log.debug("Received pong from OKEx WebSocket");
                    return;
                }

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
            
            // Record transport error in reconnection history
            statusService.recordReconnection(
                "transport-error",
                reconnectAttempts,
                false,
                null,
                exception.getMessage()
            );
            
            onConnectionLost("transport-error", exception);
        }

        @Override
        public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus closeStatus) {
            log.info("WebSocket connection closed: {}", closeStatus);
            
            // Record connection closure in reconnection history
            statusService.recordReconnection(
                "connection-closed",
                reconnectAttempts,
                false,
                null,
                closeStatus.toString()
            );
            
            onConnectionLost("connection-closed", null);
        }

        @Override
        public boolean supportsPartialMessages() {
            return false;
        }
    }
}
