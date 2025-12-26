# OKEx WebSocket 蜡烛数据处理系统 PRD

## 1. 项目概述

### 1.1 项目背景
构建一个基于Java的数据处理系统，通过OKEx WebSocket API订阅交易对蜡烛数据，并存储到Mongodb，支持gRPC调用实时计算技术指标。

### 1.2 技术栈
- **开发语言**: Java 8 (JDK 1.8)
- **数据源**: OKEx WebSocket API
- **数据库**: MongoDB
- **构建工具**: Maven
- **数据处理框架**: 自定义可配置组件系统

## 2. 功能需求

### 2.1 核心功能

#### 2.1.1 WebSocket连接管理
- 建立与OKEx WebSocket的稳定连接
- 实现自动重连机制
- 连接状态监控和异常处理
- 心跳检测机制

#### 2.1.2 数据订阅功能
- 支持多交易对订阅：BTC-USDT-SWAP, ETH-USDT-SWAP
- 支持多时间维度：1m (1分钟), 1H (1小时)
- 可配置的交易对列表
- 可配置的时间维度列表

#### 2.1.3 蜡烛数据处理
- 实时接收和解析蜡烛图数据
- 数据格式标准化
- 数据验证和清洗
- 时间戳统一处理
- 存储到MongoDB

### 2.2 技术指标计算

#### 2.2.1 支持的技术指标
- **RSI (相对强弱指数)**: 周期参数由gRPC客户端指定
- **BOLL (布林带)**: 周期和标准差参数由gRPC客户端指定
- **MACD (移动平均收敛散度)**: 快线、慢线和信号线周期参数由gRPC客户端指定

#### 2.2.2 计算服务特性
- 输入：从MongoDB查询的蜡烛图数据列表
- 输出：实时计算结果，通过gRPC接口返回
- 支持动态参数配置（周期、系数等）
- Redis缓存计算结果，避免重复计算
- 支持单个计算和批量计算
- 支持流式计算结果推送

### 2.3 数据服务功能

#### 2.3.1 gRPC服务接口
- 提供实时技术指标计算服务
- 支持自定义参数的计算请求
- 基于gRPC的高性能RPC调用
- 支持流式和一元调用
- 实时计算结果返回

#### 2.3.2 数据存储和缓存
- MongoDB存储原始蜡烛图数据
- Redis缓存计算结果
- 缓存过期策略管理
- 数据一致性保证

#### 2.3.3 计算服务架构
```
gRPC调用 → 参数解析 → MongoDB查询 → 技术指标计算 → 结果缓存 → 返回响应
```

#### 2.3.4 gRPC服务特性
- 高性能二进制协议传输
- 支持双向流式通信
- 自动代码生成
- 强类型接口定义
- 跨语言客户端支持

#### 2.3.4 MongoDB数据结构设计（仅存储原始数据）
```json
{
  "symbol": "BTC-USDT-SWAP",
  "timestamp": "2023-12-25T10:00:00Z",
  "interval": "1m",
  "open": 42000.0,
  "high": 42100.0,
  "low": 41950.0,
  "close": 42050.0,
  "volume": 1250.8,
  "created_at": "2023-12-25T10:00:01Z"
}
```

#### 2.3.5 Redis缓存结构设计
```
Key: rsi:BTC-USDT-SWAP:1m:14
Value: {"timestamp": "2023-12-25T10:00:00Z", "value": 65.4, "data_points": 100}
TTL: 300秒

Key: boll:BTC-USDT-SWAP:1m:15
Value: {"timestamp": "2023-12-25T10:00:00Z", "upper": 42200.0, "middle": 42000.0, "lower": 41800.0, "data_points": 100}
TTL: 300秒
```

## 3. 非功能需求

### 3.1 性能要求
- 数据处理延迟：< 100ms
- WebSocket消息处理：> 1000条/秒
- MongoDB批量写入：> 500条/次
- 内存占用：< 512MB

### 3.2 可靠性要求
- 系统可用性：99.9%
- 数据完整性：零丢失
- 自动重连：网络中断后5秒内重连
- 异常恢复：组件异常不影响整体运行

### 3.3 可扩展性要求
- 新增技术指标无需代码修改
- 支持新交易对动态添加
- 支持新时间维度配置
- 组件热插拔能力

## 4. 系统架构

### 4.1 整体架构
```
                    ┌─────────────────┐
                    │  gRPC Clients   │
                    │ (外部调用方)     │
                    └─────────┬───────┘
                              │ gRPC调用
                              ▼
                    ┌─────────────────┐
                    │   gRPC Service  │
                    │    Layer        │
                    └─────────┬───────┘
                              │
          ┌───────────────────┼───────────────────┐
          │                   │                   │
          ▼                   ▼                   ▼
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│   Redis     │    │ Calculation │    │  MongoDB    │
│   Cache     │    │  Engine     │    │  Database   │
└─────────────┘    └─────────────┘    └─────────────┘
                              ▲
                              │
                    ┌─────────────────┐
                    │  OKEx WebSocket │
                    │    Data Feed    │
                    └─────────────────┘
```

### 4.2 核心模块

#### 4.2.1 WebSocket客户端模块
- 连接管理
- 消息订阅
- 数据接收
- 错误处理

#### 4.2.2 数据处理引擎
- 数据解析
- 格式转换
- 组件调度
- 结果聚合

#### 4.2.3 计算引擎
- RSI指标计算
- 布林带指标计算
- MACD指标计算
- 数据预处理和验证

#### 4.2.4 gRPC服务层
- Protocol Buffers接口定义
- gRPC服务实现
- 参数验证和解析
- 计算结果格式化
- 流式调用支持
- 响应缓存控制

#### 4.2.5 数据访问层
- MongoDB连接管理
- 蜡烛图数据查询和存储
- Redis缓存操作
- 数据聚合和过滤

## 5. 配置管理

### 5.1 应用配置
```yaml
# WebSocket配置
websocket:
  okex:
    url: "wss://ws.okx.com:8443/ws/v5/public"
    reconnect_interval: 5000
    heartbeat_interval: 30000

# 订阅配置
subscription:
  symbols:
    - "BTC-USDT-SWAP"
    - "ETH-USDT-SWAP"
  intervals:
    - "1m"
    - "1H"

# MongoDB配置
mongodb:
  host: "localhost"
  port: 27017
  database: "okex_data"
  collection: "candles"

# Redis配置
redis:
  host: "localhost"
  port: 6379
  database: 0
  password: null
  timeout: 5000

# gRPC服务配置
grpc:
  server:
    port: 50051
    max_message_size: "4MB"
    deadline_timeout: 30000
  cache:
    default_ttl: 300  # 5分钟
    max_memory: "256MB"


```

### 5.2 运行时配置
- 数据源配置
- 存储策略配置
- 缓存策略配置
- gRPC服务配置

## 6. 接口设计

### 6.1 WebSocket消息格式
```json
// 订阅消息
{
  "op": "subscribe",
  "args": [
    {
      "channel": "candle1m",
      "instId": "BTC-USDT-SWAP"
    }
  ]
}

// 数据推送消息
{
  "arg": {
    "channel": "candle1m",
    "instId": "BTC-USDT-SWAP"
  },
  "data": [
    {
      "ts": "1703505600000",
      "o": "42000",
      "h": "42100",
      "l": "41950",
      "c": "42050",
      "vol": "1250.8",
      "ccy": "USDT",
      "confirm": "1"
    }
  ]
}
```

### 6.2 gRPC接口设计

#### 6.2.1 Protocol Buffers定义
```protobuf
syntax = "proto3";

package okex.indicator;

service IndicatorService {
  // RSI计算
  rpc CalculateRSI(RSIRequest) returns (IndicatorResponse);
  
  // 批量RSI计算
  rpc CalculateRSIBatch(BatchRSIRequest) returns (stream IndicatorResponse);
  
  // 布林带计算
  rpc CalculateBOLL(BOLLRequest) returns (IndicatorResponse);
  
  // 批量布林带计算
  rpc CalculateBOLLBatch(BatchBOLLRequest) returns (stream IndicatorResponse);
  
  // MACD计算
  rpc CalculateMACD(MACDRequest) returns (IndicatorResponse);
  
  // 批量MACD计算
  rpc CalculateMACDBatch(BatchMACDRequest) returns (stream IndicatorResponse);
  
  // 实时指标流
  rpc StreamIndicators(StreamRequest) returns (stream IndicatorResponse);
}

// 基础请求结构
message BaseRequest {
  string symbol = 1;
  string interval = 2;
  int32 limit = 3;
}

// RSI请求
message RSIRequest {
  BaseRequest base = 1;
  int32 period = 2;
}

// 批量RSI请求
message BatchRSIRequest {
  repeated RSIRequest requests = 1;
}

// 布林带请求
message BOLLRequest {
  BaseRequest base = 1;
  int32 period = 2;
  double std_dev = 3;
}

// 批量布林带请求
message BatchBOLLRequest {
  repeated BOLLRequest requests = 1;
}

// MACD请求
message MACDRequest {
  BaseRequest base = 1;
  int32 fast_period = 2;
  int32 slow_period = 3;
  int32 signal_period = 4;
}

// 批量MACD请求
message BatchMACDRequest {
  repeated MACDRequest requests = 1;
}

// 流式请求
message StreamRequest {
  string symbol = 1;
  string interval = 2;
  repeated IndicatorType indicators = 3;
}

enum IndicatorType {
  RSI = 0;
  BOLL = 1;
  MACD = 2;
}

// 指标响应
message IndicatorResponse {
  bool success = 1;
  string error_message = 2;
  string symbol = 3;
  IndicatorType indicator_type = 4;
  string interval = 5;
  int64 timestamp = 6;
  int32 data_points = 7;
  bool from_cache = 8;
  
  // RSI结果
  optional double rsi_value = 10;
  
  // 布林带结果
  optional double boll_upper = 11;
  optional double boll_middle = 12;
  optional double boll_lower = 13;
  
  // MACD结果
  optional double macd_line = 14;
  optional double macd_signal = 15;
  optional double macd_histogram = 16;
}
```

#### 6.2.3 gRPC服务实现
```java
public class IndicatorServiceImpl extends IndicatorServiceGrpc.IndicatorServiceImplBase {
    
    private final CalculationEngine calculationEngine;
    private final RedisCache redisCache;
    private final MongoRepository mongoRepository;
    
    @Override
    public void calculateRSI(RSIRequest request, StreamObserver<IndicatorResponse> responseObserver) {
        try {
            // 检查缓存
            String cacheKey = String.format("rsi:%s:%d", request.getBase().getSymbol(), request.getPeriod());
            IndicatorResponse cachedResult = redisCache.get(cacheKey);
            
            if (cachedResult != null) {
                IndicatorResponse.Builder builder = cachedResult.toBuilder()
                    .setFromCache(true);
                responseObserver.onNext(builder.build());
                responseObserver.onCompleted();
                return;
            }
            
            // 从MongoDB获取数据
            List<Candle> candles = mongoRepository.findCandles(
                request.getBase().getSymbol(),
                request.getBase().getInterval(),
                request.getBase().getLimit()
            );
            
            // 计算RSI
            IndicatorParams params = new IndicatorParams();
            params.addParameter("period", request.getPeriod());
            IndicatorResult result = calculationEngine.calculateRSI(candles, params);
            
            // 构建响应
            IndicatorResponse response = IndicatorResponse.newBuilder()
                .setSuccess(true)
                .setSymbol(request.getBase().getSymbol())
                .setIndicatorType(IndicatorType.RSI)
                .setInterval(request.getBase().getInterval())
                .setTimestamp(System.currentTimeMillis())
                .setDataPoints(candles.size())
                .setRsiValue(result.getValue())
                .setFromCache(false)
                .build();
            
            // 缓存结果
            redisCache.set(cacheKey, response, 300); // 5分钟TTL
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            IndicatorResponse errorResponse = IndicatorResponse.newBuilder()
                .setSuccess(false)
                .setErrorMessage(e.getMessage())
                .build();
            responseObserver.onNext(errorResponse);
            responseObserver.onCompleted();
        }
    }
    
    @Override
    public void calculateRSIBatch(BatchRSIRequest request, StreamObserver<IndicatorResponse> responseObserver) {
        for (RSIRequest rsiRequest : request.getRequestsList()) {
            calculateRSI(rsiRequest, responseObserver);
        }
        responseObserver.onCompleted();
    }
}
```

#### 6.2.4 组件接口
```java
public interface DataProcessor {
    /**
     * 处理蜡烛图数据并计算技术指标
     * @param candles 蜡烛图数据列表
     * @param config 组件配置
     * @return 计算结果
     */
    IndicatorResult process(List<Candle> candles, ProcessorConfig config);
    
    /**
     * 初始化组件
     * @param config 配置参数
     */
    void initialize(ProcessorConfig config);
    
    /**
     * 获取组件名称
     * @return 组件名称
     */
    String getName();
}

public class IndicatorResult {
    private Double value;
    private Map<String, Double> values; // 复合指标如MACD
    private String timestamp;
    private Integer dataPoints;
    // getters/setters
}
```

## 7. 部署要求

### 7.1 环境要求
- Java 1.8+
- MongoDB 3.6+
- Redis 4.0+
- 内存：最小512MB，推荐1GB
- 网络：稳定的互联网连接

### 7.2 启动流程
1. 检查Java环境
2. 验证MongoDB连接
3. 加载配置文件
4. 初始化组件
5. 启动WebSocket连接
6. 开始数据订阅

## 8. 监控和日志

### 8.1 监控指标
- WebSocket连接状态
- 数据接收速率
- gRPC请求处理延迟
- gRPC调用成功率
- 流式调用性能
- Redis缓存命中率
- MongoDB查询性能
- 指标计算时间
- 错误率和异常统计
- 并发连接数

### 8.2 日志记录
- WebSocket连接状态日志
- 数据接收和存储日志
- gRPC请求和响应日志
- 流式调用日志
- Redis缓存操作日志
- MongoDB查询日志
- 指标计算执行日志
- 错误和异常日志
- 性能指标和统计日志

## 9. 测试策略

### 9.1 单元测试
- 技术指标计算逻辑测试（RSI、BOLL、MACD）
- 数据处理和存储测试
- 参数验证测试
- Redis缓存操作测试
- gRPC服务逻辑测试
- Protocol Buffers序列化测试

### 9.2 集成测试
- WebSocket连接和数据接收测试
- gRPC服务接口测试
- 流式调用测试
- 端到端数据流测试
- MongoDB数据存储测试
- Redis缓存功能测试
- 跨语言客户端测试

### 9.3 性能测试
- 高并发gRPC请求测试
- 流式调用性能测试
- 缓存性能测试
- 数据库查询性能测试
- 网络延迟测试
- 长时间运行稳定性测试
- 内存泄漏测试
- 二进制协议性能对比测试

## 10. 交付物

### 10.1 代码交付
- 完整的Java源代码
- Maven构建配置
- 配置文件模板
- 数据库初始化脚本

### 10.2 文档交付
- API文档
- 部署手册
- 运维指南
- 组件开发指南

### 10.3 测试交付
- 单元测试用例
- 集成测试用例
- 性能测试报告

## 11. 风险评估

### 11.1 技术风险
- WebSocket连接稳定性
- gRPC服务性能瓶颈
- 流式调用资源管理
- Redis缓存失效
- MongoDB查询性能
- Protocol Buffers版本兼容性
- 第三方API变更
- 缓存数据一致性

### 11.2 缓解措施
- 实现重连和容错机制
- 负载均衡和集群部署
- 流式调用资源控制
- 多级缓存策略
- 数据库索引优化
- Protocol Buffers向后兼容性
- API版本兼容性处理
- 缓存更新和失效机制

## 12. 项目时间线

### 12.1 开发阶段
- 阶段1（1周）：基础架构搭建
- 阶段2（1周）：WebSocket客户端开发
- 阶段3（1周）：数据处理引擎开发
- 阶段4（1周）：gRPC服务层开发
- 阶段5（1周）：Protocol Buffers定义和代码生成
- 阶段6（1周）：缓存系统集成
- 阶段7（1周）：组件系统开发
- 阶段8（1周）：集成测试和优化

### 12.2 里程碑
- M1：基础框架完成
- M2：WebSocket连接成功
- M3：数据处理流程完成
- M4：gRPC服务完成
- M5：Protocol Buffers集成完成
- M6：缓存系统集成完成
- M7：组件系统完成
- M8：系统集成测试通过