package com.supermancell.server.websocket;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时刷新订阅配置（每分钟一次），如果订阅列表有变化则自动重新订阅。
 */
@Component
@RequiredArgsConstructor
public class SubscriptionRefreshTask {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionRefreshTask.class);

    private final SubscriptionConfigLoader configLoader;
    private final OkexWebSocketClient webSocketClient;

    @Scheduled(fixedDelayString = "${subscription.refresh-interval-ms:60000}")
    public void refreshSubscriptions() {
        try {
            SubscriptionConfig config = configLoader.loadCurrentConfig();
            if (config == null) {
                log.warn("Subscription config is null, skip refresh");
                return;
            }
            webSocketClient.applySubscriptions(config);
        } catch (Exception e) {
            log.error("Failed to refresh subscriptions", e);
        }
    }
}
