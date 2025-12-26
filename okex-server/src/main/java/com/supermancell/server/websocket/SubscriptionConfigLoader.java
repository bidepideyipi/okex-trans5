package com.supermancell.server.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.env.Environment;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@Component
public class SubscriptionConfigLoader {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionConfigLoader.class);

    private final Environment environment;

    @Value("${subscription.refresh-interval-ms:60000}")
    private long refreshIntervalMs;

    public SubscriptionConfigLoader(Environment environment) {
        this.environment = environment;
    }

    /**
     * 加载当前订阅配置。优先从工作目录下的 application.yml 读取，
     * 如果不存在则回退到 Spring Environment 中的配置（即 classpath:application.yml）。
     */
    public SubscriptionConfig loadCurrentConfig() {
        SubscriptionConfig fromFile = loadFromExternalApplicationYaml();
        if (fromFile != null) {
            return fromFile;
        }
        return loadFromEnvironment();
    }

    public long getRefreshIntervalMs() {
        return refreshIntervalMs;
    }

    private SubscriptionConfig loadFromExternalApplicationYaml() {
        File file = new File("application.yml");
        if (!file.exists() || !file.isFile()) {
            return null;
        }
        try {
            YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
            yaml.setResources(new FileSystemResource(file));
            Properties properties = yaml.getObject();
            if (properties == null) {
                return null;
            }
            List<String> symbols = readList(properties, "subscription.symbols");
            List<String> intervals = readList(properties, "subscription.intervals");
            return new SubscriptionConfig(symbols, intervals);
        } catch (Exception e) {
            log.warn("Failed to load subscription config from external application.yml, falling back to Environment", e);
            return null;
        }
    }

    private List<String> readList(Properties properties, String prefix) {
        List<String> list = new ArrayList<>();
        int index = 0;
        while (true) {
            String key = prefix + "[" + index + "]";
            if (!properties.containsKey(key)) {
                break;
            }
            String value = properties.getProperty(key);
            if (value != null && !value.trim().isEmpty()) {
                list.add(value.trim());
            }
            index++;
        }
        return list;
    }

    @SuppressWarnings("unchecked")
    private SubscriptionConfig loadFromEnvironment() {
        List<String> symbols = environment.getProperty("subscription.symbols", List.class);
        List<String> intervals = environment.getProperty("subscription.intervals", List.class);
        return new SubscriptionConfig(symbols, intervals);
    }
}
