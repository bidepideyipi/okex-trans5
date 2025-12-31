# OKEx Technical Indicator System

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-8-blue.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7.17-green.svg)](https://spring.io/projects/spring-boot)
[![gRPC](https://img.shields.io/badge/gRPC-1.58.0-brightgreen.svg)](https://grpc.io/)

A production-ready cryptocurrency technical indicator calculation system with real-time data collection, two-tier caching, and dual API interfaces (REST + gRPC).

## Features

- ✅ **Real-time Data Collection**: WebSocket connection to OKEx with auto-reconnect
- ✅ **Technical Indicators**: RSI, BOLL, MACD, Pinbar with full test coverage
- ✅ **Two-tier Caching**: Redis caching for indicator results and candle data
- ✅ **Dual API**: REST API (port 8080) and gRPC API (port 50051)
- ✅ **Data Integrity**: AOP-based validation with REST API fallback
- ✅ **Frontend Dashboard**: Vue 3 real-time monitoring dashboard
- ✅ **Production Ready**: 87 passing tests, 100% completion

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

---

## Architecture

For detailed architecture documentation, see [ARCHITECTURE.md](doc/ARCHITECTURE.md) and [PROJECT_STATUS.md](doc/PROJECT_STATUS.md).

### System Components

1. **okex-common**: Shared models, Protocol Buffers definitions
2. **okex-server**: Backend service (REST + gRPC + WebSocket)
3. **okex-client**: gRPC client SDK with usage examples
4. **okex-dashboard**: Vue 3 frontend dashboard

### Technology Stack

- **Backend**: Java 8, Spring Boot 2.7.17, gRPC 1.58.0
- **Database**: MongoDB (candle data storage)
- **Cache**: Redis (Jedis 4.4.3)
- **Frontend**: Vue 3.5.24, Vite 7.2.5, Chart.js 4.5.1
- **Communication**: WebSocket, REST API, gRPC

---

## API Documentation

### REST API (Port 8080)

- `GET /api/indicators/rsi` - Calculate RSI
- `GET /api/indicators/boll` - Calculate Bollinger Bands
- `GET /api/indicators/macd` - Calculate MACD
- `GET /api/indicators/pinbar` - Detect Pinbar pattern
- `GET /api/candles` - Retrieve candle data
- `GET /api/subscriptions` - List active subscriptions
- `POST /api/subscriptions/update` - Update subscriptions

### gRPC API (Port 50051)

9 RPC methods available:
- Single calculations: `calculateRSI`, `calculateBOLL`, `calculateMACD`, `calculatePinbar`
- Batch operations: `calculateRSIBatch`, `calculateBOLLBatch`, `calculateMACDBatch`, `calculatePinbarBatch`
- Streaming: `streamIndicators`

See [ClientExample.java](okex-client/src/main/java/com/supermancell/client/example/ClientExample.java) for usage examples.

---

## Testing

Run tests with:

```bash
mvn test
```

**Test Coverage**:
- 87 passing tests (100% success rate)
- Unit tests for all calculators (48 tests)
- Integration tests (11 tests)
- AOP aspect tests (16 tests)
- WebSocket and REST client tests (12 tests)

---

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

### MIT License Summary

- ✅ **Commercial use** - You can use this software for commercial purposes
- ✅ **Modification** - You can modify the source code
- ✅ **Distribution** - You can distribute the software
- ✅ **Private use** - You can use the software privately
- ⚠️ **Liability** - The software is provided "as is", without warranty
- ⚠️ **Warranty** - No warranty is provided

---

## Disclaimer

**Important**: This software is for educational and research purposes only. 

- ⚠️ **Trading Risk**: Cryptocurrency trading involves substantial risk of loss. Past performance does not guarantee future results.
- ⚠️ **No Financial Advice**: This software does not provide financial, investment, or trading advice.
- ⚠️ **Use at Your Own Risk**: The authors and contributors are not responsible for any financial losses incurred from using this software.
- ⚠️ **No Warranty**: The software is provided "as is" without any warranty of any kind.

Always conduct your own research and consult with a qualified financial advisor before making any investment decisions.

---

## Acknowledgments

- [OKX Exchange](https://www.okx.com/) for providing WebSocket and REST APIs
- [Spring Boot](https://spring.io/projects/spring-boot) for the application framework
- [gRPC](https://grpc.io/) for high-performance RPC communication
- [Vue.js](https://vuejs.org/) for the frontend framework

---

## Contact

For questions or suggestions, please open an issue on GitHub.

**Project Status**: Production Ready (100% Complete)
