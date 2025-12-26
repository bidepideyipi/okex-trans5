package com.supermancell.server.websocket;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;

class SubscriptionRefreshTaskTest {

    /**
     * 配置变更后能正确退订旧项、订阅新项
     */
    @Test
    void refreshSubscriptionsShouldApplyLoadedConfig() {
        SubscriptionConfigLoader loader = Mockito.mock(SubscriptionConfigLoader.class);
        OkexWebSocketClient client = Mockito.mock(OkexWebSocketClient.class);

        SubscriptionConfig config = new SubscriptionConfig(
                Arrays.asList("BTC-USDT-SWAP"),
                Arrays.asList("1m")
        );
        Mockito.when(loader.loadCurrentConfig()).thenReturn(config);

        SubscriptionRefreshTask task = new SubscriptionRefreshTask(loader, client);
        task.refreshSubscriptions();

        Mockito.verify(client, Mockito.times(1)).applySubscriptions(config);
    }

    /**
     * 配置为空时，不执行任何操作
     */
    @Test
    void refreshSubscriptionsShouldSkipWhenConfigIsNull() {
        SubscriptionConfigLoader loader = Mockito.mock(SubscriptionConfigLoader.class);
        OkexWebSocketClient client = Mockito.mock(OkexWebSocketClient.class);

        Mockito.when(loader.loadCurrentConfig()).thenReturn(null);

        SubscriptionRefreshTask task = new SubscriptionRefreshTask(loader, client);
        task.refreshSubscriptions();

        Mockito.verifyNoInteractions(client);
    }
}
