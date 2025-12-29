## WebSocket Client Debug Guide

### 1. Prerequisites
- **Java**: JDK 8+ (project编译目标为 1.8)
- **Maven**: 已安装并在 `PATH` 中
- **Network**: 能访问 `wss://ws.okx.com:8443/ws/v5/public`

> 当前为了专注调试 WebSocket 客户端，`okex-server` 中已禁用 MongoDB 自动配置（见 `spring.autoconfigure.exclude`）。后续接入 MongoDB 时可以根据需要调整。

依赖MongoDB做为存储，开发环境需要启动MongoDB服务，启动示例：
```shell
cd /usr/local/mongodb/bin
./mongod --dbpath /usr/local/var/mongodb --logpath /usr/local/var/log/mongodb/mong.log --fork
```
依赖Redis做为缓存，开发环境需要启动Redis服务，启动示例：
```shell
redis-server &
```

### 2. 配置订阅列表

在 `okex-server/src/main/resources/application.yml` 中已有默认配置：

```yaml
subscription:
  symbols:
    - BTC-USDT-SWAP
    - ETH-USDT-SWAP
  intervals:
    - 1m
    - 1H
  refresh-interval-ms: 60000
```

- **symbols**: 订阅的交易对列表
- **intervals**: K 线时间维度列表
- **refresh-interval-ms**: 定时刷新配置的间隔（毫秒），默认 60 秒

> 实际部署时建议在可执行 jar 同级目录放一个外部 `application.yml`，修改该文件即可在运行时动态调整订阅，无需重启。

### 3. 启动 WebSocket 客户端（okex-server）

在项目根目录执行：

```bash
cd /Users/anthony/Documents/github/okex-trans-5
mvn -pl okex-server spring-boot:run
```

预期日志（示例）：

```text
... OkexServerApplication : Starting OkexServerApplication ...
... OkexWebSocketClient  : OKEx WebSocket connection established
... OkexWebSocketClient  : Connected to OKEx WebSocket: wss://ws.okx.com:8443/ws/v5/public
... OkexWebSocketClient  : Initial subscriptions applied: SubscriptionConfig{symbols=[BTC-USDT-SWAP, ETH-USDT-SWAP], intervals=[1m, 1H]}
... GrpcServerLifecycle  : gRPC Server started, listening on port: 50051
```

如果看到以上日志，说明：
- Spring Boot 服务启动成功（HTTP 8080 + gRPC 50051）
- 已成功连接 OKEx WebSocket
- 根据当前订阅配置完成了初始订阅

### 4. 调试动态订阅刷新

#### 4.1 修改订阅配置

1. 在运行中的环境下，编辑外部 `application.yml`（推荐放在可执行 jar 同级目录）。例如：
   ```yaml
   subscription:
     symbols:
       - BTC-USDT-SWAP
     intervals:
       - 1m
       - 1H
   ```
2. 保存文件后无需重启服务。

#### 4.2 验证自动重订阅

- 等待约一个刷新周期（默认 60 秒），查看日志：

```text
... SubscriptionRefreshTask : refreshSubscriptions ...
... OkexWebSocketClient    : Updated subscriptions. New config: SubscriptionConfig{symbols=[...], intervals=[...]}
... OkexWebSocketClient    : Sent subscribe message: {...}
... OkexWebSocketClient    : Sent unsubscribe message: {...}
```

- 核心验证点：
  - 日志中出现 `Updated subscriptions. New config: ...`
  - 可以看到相应的 `subscribe` / `unsubscribe` 消息被发送

### 5. 常见问题排查

- **无法连接 OKEx WebSocket**
  - 检查本机网络是否能访问 OKEx 域名
  - 确认 `websocket.okex.url` 配置为 `wss://ws.okx.com:8443/ws/v5/public`

- **订阅配置没有生效**
  - 确认修改的是运行目录下的外部 `application.yml`，而不是源码中的 `src/main/resources/application.yml`
  - 确认 `subscription.refresh-interval-ms` 足够小，方便观察（例如 10000 即 10 秒）
  - 查看日志中是否有 `Subscription config is null, skip refresh`，如有说明配置解析失败

- **应用因 MongoDB 报错无法启动**
  - 调试 WebSocket 阶段可以保留 `spring.autoconfigure.exclude` 中对 Mongo 的排除
  - 后续接入 MongoDB 时，增加正确的 Mongo 依赖和配置后，再移除排除项
