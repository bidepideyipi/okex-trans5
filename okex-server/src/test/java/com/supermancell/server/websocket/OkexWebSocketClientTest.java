package com.supermancell.server.websocket;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Arrays;

class OkexWebSocketClientTest {

    /**
     * WebSocket 订阅 JSON 构造是否正确
     * @throws Exception
     */
    @Test
    void shouldSendSubscribeMessageOnInitialConfig() throws Exception {
        SubscriptionConfigLoader loader = Mockito.mock(SubscriptionConfigLoader.class);
        OkexWebSocketClient client = new OkexWebSocketClient(loader);

        WebSocketSession session = Mockito.mock(WebSocketSession.class);
        Mockito.when(session.isOpen()).thenReturn(true);

        // Avoid real proxy & network
        ReflectionTestUtils.setField(client, "session", session);
        ReflectionTestUtils.setField(client, "useProxy", false);

        SubscriptionConfig config = new SubscriptionConfig(
                Arrays.asList("BTC-USDT-SWAP"),
                Arrays.asList("1m")
        );

        client.applySubscriptions(config);

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        Mockito.verify(session, Mockito.times(1)).sendMessage(captor.capture());

        String payload = captor.getValue().getPayload();
        Assertions.assertTrue(payload.contains("\"op\":\"subscribe\""));
        Assertions.assertTrue(payload.contains("\"channel\":\"candle1m\""));
        Assertions.assertTrue(payload.contains("\"instId\":\"BTC-USDT-SWAP\""));
    }

    /**
     * WebSocket 订阅 JSON 构造是否正确(当订阅变更时)
     * @throws Exception
     */
    @Test
    void shouldSendUnsubscribeAndSubscribeOnConfigChange() throws Exception {
        SubscriptionConfigLoader loader = Mockito.mock(SubscriptionConfigLoader.class);
        OkexWebSocketClient client = new OkexWebSocketClient(loader);

        WebSocketSession session = Mockito.mock(WebSocketSession.class);
        Mockito.when(session.isOpen()).thenReturn(true);

        ReflectionTestUtils.setField(client, "session", session);
        ReflectionTestUtils.setField(client, "useProxy", false);

        SubscriptionConfig oldConfig = new SubscriptionConfig(
                Arrays.asList("BTC-USDT-SWAP", "ETH-USDT-SWAP"),
                Arrays.asList("1m")
        );
        SubscriptionConfig newConfig = new SubscriptionConfig(
                Arrays.asList("BTC-USDT-SWAP"),
                Arrays.asList("1m", "1H")
        );

        // Seed currentConfig to oldConfig
        ReflectionTestUtils.setField(client, "currentConfig", oldConfig);

        client.applySubscriptions(newConfig);

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        Mockito.verify(session, Mockito.atLeastOnce()).sendMessage(captor.capture());

        boolean hasUnsubscribe = captor.getAllValues().stream()
                .map(TextMessage::getPayload)
                .anyMatch(p -> p.contains("\"op\":\"unsubscribe\"")
                        && p.contains("\"instId\":\"ETH-USDT-SWAP\""));

        boolean hasSubscribeNew = captor.getAllValues().stream()
                .map(TextMessage::getPayload)
                .anyMatch(p -> p.contains("\"op\":\"subscribe\"")
                        && p.contains("\"channel\":\"candle1H\"")
                        && p.contains("\"instId\":\"BTC-USDT-SWAP\""));

        Assertions.assertTrue(hasUnsubscribe, "Expected unsubscribe for removed symbol/interval");
        Assertions.assertTrue(hasSubscribeNew, "Expected subscribe for new symbol/interval");
    }
}
