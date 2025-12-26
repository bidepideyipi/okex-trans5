package com.supermancell.server.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 集成测试：验证 WebSocket 能成功连接 OKEx 并收到订阅成功响应。
 * 如果直连失败，会自动尝试使用 SOCKS5 代理。
 */
class OkexWebSocketIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(OkexWebSocketIntegrationTest.class);
    private static final String OKEX_WS_URL = "wss://ws.okx.com:8443/ws/v5/business";
    private static final int TIMEOUT_SECONDS = 15;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 验证 WebSocket 是否能成功连接 OKEx 并收到订阅成功响应。
     * @throws Exception
     */
    @Test
    void shouldConnectAndReceiveSubscriptionSuccess() throws Exception {
        AtomicBoolean subscriptionSuccess = new AtomicBoolean(false);
        CountDownLatch latch = new CountDownLatch(1);

        // First try without proxy
        boolean connected = tryConnectAndSubscribe(false, subscriptionSuccess, latch);

        if (!connected) {
            log.warn("Direct connection failed, retrying with SOCKS5 proxy...");
            subscriptionSuccess.set(false);
            CountDownLatch proxyLatch = new CountDownLatch(1);
            connected = tryConnectAndSubscribe(true, subscriptionSuccess, proxyLatch);
            latch.countDown(); // release original latch
            assertTrue(connected, "Failed to connect even with proxy");
            assertTrue(proxyLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS),
                    "Timeout waiting for subscription success with proxy");
        } else {
            assertTrue(latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS),
                    "Timeout waiting for subscription success");
        }

        assertTrue(subscriptionSuccess.get(),
                "Expected to receive subscription success event from OKEx");
    }

    private boolean tryConnectAndSubscribe(
            boolean useProxy,
            AtomicBoolean subscriptionSuccess,
            CountDownLatch latch
    ) {
        if (useProxy) {
            System.setProperty("socksProxyHost", "127.0.0.1");
            System.setProperty("socksProxyPort", "4781");
            log.info("Attempting connection with SOCKS5 proxy: 127.0.0.1:4781");
        } else {
            System.clearProperty("socksProxyHost");
            System.clearProperty("socksProxyPort");
            log.info("Attempting direct connection (no proxy)");
        }

        StandardWebSocketClient client = new StandardWebSocketClient();
        WebSocketHandler handler = new TestWebSocketHandler(subscriptionSuccess, latch);

        try {
            WebSocketSession session = client.doHandshake(handler, OKEX_WS_URL).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            log.info("WebSocket connection established");

            // Send subscribe message
            String subscribeMsg = buildSubscribeMessage();
            session.sendMessage(new TextMessage(subscribeMsg));
            log.info("Sent subscribe message: {}", subscribeMsg);

            return true;
        } catch (Exception e) {
            log.error("Connection failed: {}", e.getMessage());
            return false;
        }
    }

    private String buildSubscribeMessage() throws Exception {
        // OKEx v5 WebSocket Public channel format: candle{bar}
        // bar values: 1m,3m,5m,15m,30m,1H,2H,4H,6H,12H,1D,1W,1M,3M
        return objectMapper.writeValueAsString(
                objectMapper.createObjectNode()
                        .put("op", "subscribe")
                        .set("args", objectMapper.createArrayNode()
                                .add(objectMapper.createObjectNode()
                                        .put("channel", "candle1M")
                                        .put("instId", "BTC-USDT-SWAP")))
        );
    }

    private class TestWebSocketHandler implements WebSocketHandler {
        private final AtomicBoolean subscriptionSuccess;
        private final CountDownLatch latch;

        TestWebSocketHandler(AtomicBoolean subscriptionSuccess, CountDownLatch latch) {
            this.subscriptionSuccess = subscriptionSuccess;
            this.latch = latch;
        }

        @Override
        public void afterConnectionEstablished(WebSocketSession session) {
            log.info("Test handler: connection established");
        }

        @Override
        public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) {
            if (message instanceof TextMessage) {
                String payload = ((TextMessage) message).getPayload();
                log.info("Received message: {}", payload);

                try {
                    JsonNode root = objectMapper.readTree(payload);
                    // OKEx returns: {"event":"subscribe","arg":{"channel":"candle1m","instId":"BTC-USDT-SWAP"}}
                    if (root.has("event") && "subscribe".equals(root.get("event").asText())) {
                        log.info("Subscription success confirmed");
                        subscriptionSuccess.set(true);
                        latch.countDown();
                    }
                    // Also check for error responses
                    if (root.has("event") && "error".equals(root.get("event").asText())) {
                        log.error("OKEx error: {}", root.get("msg").asText());
                        latch.countDown();
                    }
                } catch (Exception e) {
                    log.error("Failed to parse message", e);
                }
            }
        }

        @Override
        public void handleTransportError(WebSocketSession session, Throwable exception) {
            log.error("Test handler: transport error", exception);
            latch.countDown();
        }

        @Override
        public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus closeStatus) {
            log.info("Test handler: connection closed: {}", closeStatus);
            latch.countDown();
        }

        @Override
        public boolean supportsPartialMessages() {
            return false;
        }
    }
}
