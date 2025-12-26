# OKEx WebSocket 数据处理系统架构文档

## 1. 项目结构

### 1.1 Maven多模块工程结构
```
okex-trans-5/
├── pom.xml                           # 父工程POM
├── okex-common/                      # 公共模块
│   ├── pom.xml
│   └── src/main/java/com/okex/common/
│       ├── proto/                    # Protocol Buffers定义
│       ├── model/                    # 数据模型
│       ├── util/                     # 工具类
│       └── exception/                # 异常定义
├── okex-server/                      # 服务端模块
│   ├── pom.xml
│   └── src/main/java/com/okex/server/
│       ├── grpc/                     # gRPC服务实现
│       ├── service/                  # 业务服务
│       ├── processor/                # 技术指标计算器
│       ├── storage/                  # 数据存储
│       ├── config/                   # 配置管理
│       └── websocket/                # WebSocket客户端
└── okex-client/                      # 客户端模块
    ├── pom.xml
    └── src/main/java/com/okex/client/
        ├── grpc/                     # gRPC客户端
        ├── service/                  # 客户端服务
        └── example/                  # 使用示例
```

## 2. 模块详细设计

### 2.1 父工程 (okex-trans-5)

#### 2.1.1 POM配置
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.okex</groupId>
    <artifactId>okex-trans-5</artifactId>
    <version>1.0.0</version>
    <packaging>pom</packaging>

    <properties>
        <maven.compiler.source>1.8</maven.compiler.source>
        <maven.compiler.target>1.8</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        
        <!-- 版本管理 -->
        <grpc.version>1.58.0</grpc.version>
        <protobuf.version>3.24.2</protobuf.version>
        <netty.version>4.1.100.Final</netty.version>
        <spring.boot.version>2.7.17</spring.boot.version>
        <mongodb.version>4.11.1</mongodb.version>
        <jedis.version>4.4.3</jedis.version>
        <jackson.version>2.15.2</jackson.version>
        <slf4j.version>1.7.36</slf4j.version>
        <logback.version>1.2.12</logback.version>
        <junit.version>5.10.0</junit.version>
    </properties>

    <modules>
        <module>okex-common</module>
        <module>okex-server</module>
        <module>okex-client</module>
    </modules>

    <dependencyManagement>
        <dependencies>
            <!-- gRPC -->
            <dependency>
                <groupId>io.grpc</groupId>
                <artifactId>grpc-bom</artifactId>
                <version>${grpc.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            
            <!-- Protocol Buffers -->
            <dependency>
                <groupId>com.google.protobuf</groupId>
                <artifactId>protobuf-java</artifactId>
                <version>${protobuf.version}</version>
            </dependency>
            
            <!-- Netty -->
            <dependency>
                <groupId>io.netty</groupId>
                <artifactId>netty-all</artifactId>
                <version>${netty.version}</version>
            </dependency>
            
            <!-- Spring Boot -->
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring.boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            
            <!-- MongoDB -->
            <dependency>
                <groupId>org.mongodb</groupId>
                <artifactId>mongodb-driver-sync</artifactId>
                <version>${mongodb.version}</version>
            </dependency>
            
            <!-- Redis -->
            <dependency>
                <groupId>redis.clients</groupId>
                <artifactId>jedis</artifactId>
                <version>${jedis.version}</version>
            </dependency>
            
            <!-- Jackson -->
            <dependency>
                <groupId>com.fasterxml.jackson.core</groupId>
                <artifactId>jackson-databind</artifactId>
                <version>${jackson.version}</version>
            </dependency>
            
            <!-- 日志 -->
            <dependency>
                <groupId>org.slf4j</groupId>
                <artifactId>slf4j-api</artifactId>
                <version>${slf4j.version}</version>
            </dependency>
            <dependency>
                <groupId>ch.qos.logback</groupId>
                <artifactId>logback-classic</artifactId>
                <version>${logback.version}</version>
            </dependency>
            
            <!-- 测试 -->
            <dependency>
                <groupId>org.junit.jupiter</groupId>
                <artifactId>junit-jupiter</artifactId>
                <version>${junit.version}</version>
                <scope>test</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <extensions>
            <extension>
                <groupId>kr.motd.maven</groupId>
                <artifactId>os-maven-plugin</artifactId>
                <version>1.7.1</version>
            </extension>
        </extensions>
        
        <pluginManagement>
            <plugins>
                <!-- Protocol Buffers编译插件 -->
                <plugin>
                    <groupId>org.xolstice.maven.plugins</groupId>
                    <artifactId>protobuf-maven-plugin</artifactId>
                    <version>0.6.1</version>
                    <configuration>
                        <protocArtifact>com.google.protobuf:protoc:${protobuf.version}:exe:${os.detected.classifier}</protocArtifact>
                        <pluginId>grpc-java</pluginId>
                        <pluginArtifact>io.grpc:protoc-gen-grpc-java:${grpc.version}:exe:${os.detected.classifier}</pluginArtifact>
                    </configuration>
                    <executions>
                        <execution>
                            <goals>
                                <goal>compile</goal>
                                <goal>compile-custom</goal>
                            </goals>
                        </execution>
                    </executions>
                </plugin>
                
                <!-- Maven编译插件 -->
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-compiler-plugin</artifactId>
                    <version>3.11.0</version>
                    <configuration>
                        <source>1.8</source>
                        <target>1.8</target>
                    </configuration>
                </plugin>
            </plugins>
        </pluginManagement>
    </build>
</project>
```

### 2.2 公共模块 (okex-common)

#### 2.2.1 POM配置
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.okex</groupId>
        <artifactId>okex-trans-5</artifactId>
        <version>1.0.0</version>
    </parent>

    <artifactId>okex-common</artifactId>
    <packaging>jar</packaging>

    <dependencies>
        <!-- gRPC -->
        <dependency>
            <groupId>io.grpc</groupId>
            <artifactId>grpc-netty-shaded</artifactId>
        </dependency>
        <dependency>
            <groupId>io.grpc</groupId>
            <artifactId>grpc-protobuf</artifactId>
        </dependency>
        <dependency>
            <groupId>io.grpc</groupId>
            <artifactId>grpc-stub</artifactId>
        </dependency>
        
        <!-- Protocol Buffers -->
        <dependency>
            <groupId>com.google.protobuf</groupId>
            <artifactId>protobuf-java</artifactId>
        </dependency>
        
        <!-- Jackson -->
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
        </dependency>
        
        <!-- 日志 -->
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.xolstice.maven.plugins</groupId>
                <artifactId>protobuf-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

#### 2.2.2 Protocol Buffers定义
```protobuf
// 文件路径: okex-common/src/main/proto/indicator.proto
syntax = "proto3";

package com.okex.common.proto;

option java_package = "com.okex.common.proto";
option java_outer_classname = "IndicatorServiceProto";

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

#### 2.2.3 数据模型
```java
// 文件路径: okex-common/src/main/java/com/okex/common/model/Candle.java
package com.okex.common.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public class Candle {
    @JsonProperty("symbol")
    private String symbol;
    
    @JsonProperty("timestamp")
    private Instant timestamp;
    
    @JsonProperty("interval")
    private String interval;
    
    @JsonProperty("open")
    private double open;
    
    @JsonProperty("high")
    private double high;
    
    @JsonProperty("low")
    private double low;
    
    @JsonProperty("close")
    private double close;
    
    @JsonProperty("volume")
    private double volume;
    
    @JsonProperty("created_at")
    private Instant createdAt;
    
    // 构造函数、getter、setter
}

// 文件路径: okex-common/src/main/java/com/okex/common/model/IndicatorResult.java
package com.okex.common.model;

import java.util.Map;

public class IndicatorResult {
    private Double value;
    private Map<String, Double> values;
    private String timestamp;
    private Integer dataPoints;
    
    // 构造函数、getter、setter
}

// 文件路径: okex-common/src/main/java/com/okex/common/model/IndicatorParams.java
package com.okex.common.model;

import java.util.HashMap;
import java.util.Map;

public class IndicatorParams {
    private Map<String, Object> parameters = new HashMap<>();
    
    public void addParameter(String key, Object value) {
        parameters.put(key, value);
    }
    
    public Object getParameter(String key) {
        return parameters.get(key);
    }
    
    // getter、setter
}
```

#### 2.2.4 技术指标接口
```java
// 文件路径: okex-common/src/main/java/com/okex/common/indicator/TechnicalIndicator.java
package com.okex.common.indicator;

import com.okex.common.model.Candle;
import com.okex.common.model.IndicatorParams;
import com.okex.common.model.IndicatorResult;

public interface TechnicalIndicator {
    /**
     * 计算技术指标
     */
    IndicatorResult calculate(java.util.List<Candle> candles, IndicatorParams params);
    
    /**
     * 获取指标名称
     */
    String getName();
}
```

### 2.3 服务端模块 (okex-server)

#### 2.3.1 POM配置
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.okex</groupId>
        <artifactId>okex-trans-5</artifactId>
        <version>1.0.0</version>
    </parent>

    <artifactId>okex-server</artifactId>
    <packaging>jar</packaging>

    <dependencies>
        <!-- 公共模块 -->
        <dependency>
            <groupId>com.okex</groupId>
            <artifactId>okex-common</artifactId>
            <version>${project.version}</version>
        </dependency>
        
        <!-- Spring Boot -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
        </dependency>
        
        <!-- MongoDB -->
        <dependency>
            <groupId>org.mongodb</groupId>
            <artifactId>mongodb-driver-sync</artifactId>
        </dependency>
        
        <!-- Redis -->
        <dependency>
            <groupId>redis.clients</groupId>
            <artifactId>jedis</artifactId>
        </dependency>
        
        <!-- WebSocket客户端 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-websocket</artifactId>
        </dependency>
        
        <!-- 配置处理器 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-configuration-processor</artifactId>
            <optional>true</optional>
        </dependency>
        
        <!-- 测试 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

#### 2.3.2 服务端启动类
```java
// 文件路径: okex-server/src/main/java/com/okex/server/OkexServerApplication.java
package com.okex.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.okex.server", "com.okex.common"})
public class OkexServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(OkexServerApplication.class, args);
    }
}
```

#### 2.3.3 gRPC服务实现
```java
// 文件路径: okex-server/src/main/java/com/okex/server/grpc/IndicatorServiceImpl.java
package com.okex.server.grpc;

import com.okex.common.proto.*;
import com.okex.server.service.CalculationEngine;
import com.okex.server.storage.CacheService;
import com.okex.server.storage.MongoRepository;
import com.okex.common.model.Candle;
import com.okex.common.model.IndicatorParams;
import com.okex.common.model.IndicatorResult;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@GrpcService
public class IndicatorServiceImpl extends IndicatorServiceGrpc.IndicatorServiceImplBase {
    
    private static final Logger logger = LoggerFactory.getLogger(IndicatorServiceImpl.class);
    
    @Autowired
    private CalculationEngine calculationEngine;
    
    @Autowired
    private CacheService cacheService;
    
    @Autowired
    private MongoRepository mongoRepository;
    
    @Override
    public void calculateRSI(RSIRequest request, StreamObserver<IndicatorResponse> responseObserver) {
        try {
            // 检查缓存
            String cacheKey = String.format("rsi:%s:%d", request.getBase().getSymbol(), request.getPeriod());
            IndicatorResponse cachedResult = cacheService.get(cacheKey, IndicatorResponse.class);
            
            if (cachedResult != null) {
                IndicatorResponse.Builder builder = cachedResult.toBuilder()
                    .setFromCache(true);
                responseObserver.onNext(builder.build());
                responseObserver.onCompleted();
                return;
            }
            
            // 从MongoDB获取数据
            java.util.List<Candle> candles = mongoRepository.findCandles(
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
            cacheService.set(cacheKey, response, 300); // 5分钟TTL
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            logger.error("RSI计算失败", e);
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
    
    // 类似实现其他方法...
}
```

#### 2.3.4 技术指标计算引擎
```java
// 文件路径: okex-server/src/main/java/com/okex/server/service/CalculationEngine.java
package com.okex.server.service;

import com.okex.common.model.Candle;
import com.okex.common.model.IndicatorParams;
import com.okex.common.model.IndicatorResult;
import com.okex.server.processor.RSICalculator;
import com.okex.server.processor.BOLLCalculator;
import com.okex.server.processor.MACDCalculator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CalculationEngine {
    
    @Autowired
    private RSICalculator rsiCalculator;
    
    @Autowired
    private BOLLCalculator bollCalculator;
    
    @Autowired
    private MACDCalculator macdCalculator;
    
    public IndicatorResult calculateRSI(java.util.List<Candle> candles, IndicatorParams params) {
        return rsiCalculator.calculate(candles, params);
    }
    
    public IndicatorResult calculateBOLL(java.util.List<Candle> candles, IndicatorParams params) {
        return bollCalculator.calculate(candles, params);
    }
    
    public IndicatorResult calculateMACD(java.util.List<Candle> candles, IndicatorParams params) {
        return macdCalculator.calculate(candles, params);
    }
}
```

#### 2.3.5 技术指标计算器
```java
// 文件路径: okex-server/src/main/java/com/okex/server/processor/RSICalculator.java
package com.okex.server.processor;

import com.okex.common.indicator.TechnicalIndicator;
import com.okex.common.model.Candle;
import com.okex.common.model.IndicatorParams;
import com.okex.common.model.IndicatorResult;
import org.springframework.stereotype.Component;

@Component
public class RSICalculator implements TechnicalIndicator {
    
    @Override
    public IndicatorResult calculate(java.util.List<Candle> candles, IndicatorParams params) {
        int period = (Integer) params.getParameter("period");
        
        if (candles.size() < period + 1) {
            throw new IllegalArgumentException("数据点数量不足");
        }
        
        // RSI计算逻辑
        java.util.List<Double> gains = new java.util.ArrayList<>();
        java.util.List<Double> losses = new java.util.ArrayList<>();
        
        for (int i = 1; i < candles.size(); i++) {
            double change = candles.get(i).getClose() - candles.get(i - 1).getClose();
            if (change > 0) {
                gains.add(change);
                losses.add(0.0);
            } else {
                gains.add(0.0);
                losses.add(Math.abs(change));
            }
        }
        
        double avgGain = gains.subList(0, period).stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double avgLoss = losses.subList(0, period).stream().mapToDouble(Double::doubleValue).average().orElse(0);
        
        for (int i = period; i < gains.size(); i++) {
            avgGain = (avgGain * (period - 1) + gains.get(i)) / period;
            avgLoss = (avgLoss * (period - 1) + losses.get(i)) / period;
        }
        
        double rs = avgLoss == 0 ? 100 : avgGain / avgLoss;
        double rsi = 100 - (100 / (1 + rs));
        
        IndicatorResult result = new IndicatorResult();
        result.setValue(rsi);
        result.setDataPoints(candles.size());
        result.setTimestamp(String.valueOf(System.currentTimeMillis()));
        
        return result;
    }
    
    @Override
    public String getName() {
        return "RSI";
    }
}

// 类似实现BOLLCalculator和MACDCalculator...
```

#### 2.3.6 数据存储层
```java
// 文件路径: okex-server/src/main/java/com/okex/server/storage/MongoRepository.java
package com.okex.server.storage;

import com.okex.common.model.Candle;
import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Repository
public class MongoRepository {
    
    @Autowired
    private MongoClient mongoClient;
    
    private static final String DATABASE = "okex_data";
    private static final String COLLECTION = "candles";
    
    public List<Candle> findCandles(String symbol, String interval, int limit) {
        MongoDatabase database = mongoClient.getDatabase(DATABASE);
        MongoCollection<Document> collection = database.getCollection(COLLECTION);
        
        List<Candle> candles = new ArrayList<>();
        
        collection.find(Filters.and(
                Filters.eq("symbol", symbol),
                Filters.eq("interval", interval)
            ))
            .sort(Sorts.descending("timestamp"))
            .limit(limit)
            .forEach(doc -> {
                Candle candle = new Candle();
                candle.setSymbol(doc.getString("symbol"));
                candle.setTimestamp(Instant.parse(doc.getString("timestamp")));
                candle.setInterval(doc.getString("interval"));
                candle.setOpen(doc.getDouble("open"));
                candle.setHigh(doc.getDouble("high"));
                candle.setLow(doc.getDouble("low"));
                candle.setClose(doc.getDouble("close"));
                candle.setVolume(doc.getDouble("volume"));
                candles.add(candle);
            });
        
        // 反转列表，使时间戳升序
        java.util.Collections.reverse(candles);
        
        return candles;
    }
    
    public void saveCandle(Candle candle) {
        MongoDatabase database = mongoClient.getDatabase(DATABASE);
        MongoCollection<Document> collection = database.getCollection(COLLECTION);
        
        Document doc = new Document()
            .append("symbol", candle.getSymbol())
            .append("timestamp", candle.getTimestamp().toString())
            .append("interval", candle.getInterval())
            .append("open", candle.getOpen())
            .append("high", candle.getHigh())
            .append("low", candle.getLow())
            .append("close", candle.getClose())
            .append("volume", candle.getVolume())
            .append("created_at", Instant.now().toString());
        
        collection.insertOne(doc);
    }
}

// 文件路径: okex-server/src/main/java/com/okex/server/storage/CacheService.java
package com.okex.server.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.concurrent.TimeUnit;

@Service
public class CacheService {
    
    @Autowired
    private JedisPool jedisPool;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    public <T> T get(String key, Class<T> clazz) {
        try (Jedis jedis = jedisPool.getResource()) {
            String value = jedis.get(key);
            if (value != null) {
                return objectMapper.readValue(value, clazz);
            }
        } catch (Exception e) {
            // 日志记录
        }
        return null;
    }
    
    public void set(String key, Object value, int ttlSeconds) {
        try (Jedis jedis = jedisPool.getResource()) {
            String json = objectMapper.writeValueAsString(value);
            jedis.setex(key, ttlSeconds, json);
        } catch (Exception e) {
            // 日志记录
        }
    }
    
    public void delete(String key) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del(key);
        }
    }
}
```

#### 2.3.7 WebSocket客户端
```java
// 文件路径: okex-server/src/main/java/com/okex/server/websocket/OKExWebSocketClient.java
package com.okex.server.websocket;

import com.okex.common.model.Candle;
import com.okex.server.storage.MongoRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import java.net.URI;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Component
public class OKExWebSocketClient {
    
    @Autowired
    private MongoRepository mongoRepository;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private WebSocketSession session;
    
    public void connect() {
        try {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            session = container.connectToServer(this, URI.create("wss://ws.okx.com:8443/ws/v5/public"));
            
            // 发送订阅消息
            subscribe();
            
        } catch (Exception e) {
            // 日志记录
        }
    }
    
    private void subscribe() {
        try {
            String subscribeMsg = "{\"op\":\"subscribe\",\"args\":[{\"channel\":\"candle1m\",\"instId\":\"BTC-USDT-SWAP\"},{\"channel\":\"candle1m\",\"instId\":\"ETH-USDT-SWAP\"},{\"channel\":\"candle1H\",\"instId\":\"BTC-USDT-SWAP\"},{\"channel\":\"candle1H\",\"instId\":\"ETH-USDT-SWAP\"}]}";
            session.sendMessage(new TextMessage(subscribeMsg));
        } catch (Exception e) {
            // 日志记录
        }
    }
    
    @OnMessage
    public void onMessage(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);
            
            if (root.has("data")) {
                JsonNode data = root.get("data").get(0);
                JsonNode arg = root.get("arg");
                
                Candle candle = new Candle();
                candle.setSymbol(arg.get("instId").asText());
                candle.setTimestamp(Instant.ofEpochMilli(data.get("ts").asLong()));
                candle.setInterval(extractInterval(arg.get("channel").asText()));
                candle.setOpen(data.get("o").asDouble());
                candle.setHigh(data.get("h").asDouble());
                candle.setLow(data.get("l").asDouble());
                candle.setClose(data.get("c").asDouble());
                candle.setVolume(data.get("vol").asDouble());
                
                mongoRepository.saveCandle(candle);
            }
        } catch (Exception e) {
            // 日志记录
        }
    }
    
    private String extractInterval(String channel) {
        if (channel.equals("candle1m")) return "1m";
        if (channel.equals("candle1H")) return "1H";
        return channel;
    }
    
    // 其他WebSocket事件处理方法...
}
```

### 2.4 客户端模块 (okex-client)

#### 2.4.1 POM配置
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.okex</groupId>
        <artifactId>okex-trans-5</artifactId>
        <version>1.0.0</version>
    </parent>

    <artifactId>okex-client</artifactId>
    <packaging>jar</packaging>

    <dependencies>
        <!-- 公共模块 -->
        <dependency>
            <groupId>com.okex</groupId>
            <artifactId>okex-common</artifactId>
            <version>${project.version}</version>
        </dependency>
        
        <!-- gRPC客户端 -->
        <dependency>
            <groupId>net.devh</groupId>
            <artifactId>grpc-client-spring-boot-starter</artifactId>
            <version>2.15.0.RELEASE</version>
        </dependency>
        
        <!-- Spring Boot -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
        </dependency>
        
        <!-- 测试 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

#### 2.4.2 客户端启动类
```java
// 文件路径: okex-client/src/main/java/com/okex/client/OkexClientApplication.java
package com.okex.client;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.okex.client", "com.okex.common"})
public class OkexClientApplication {
    public static void main(String[] args) {
        SpringApplication.run(OkexClientApplication.class, args);
    }
}
```

#### 2.4.3 gRPC客户端服务
```java
// 文件路径: okex-client/src/main/java/com/okex/client/service/IndicatorClientService.java
package com.okex.client.service;

import com.okex.common.proto.*;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import java.util.concurrent.TimeUnit;

@Service
public class IndicatorClientService {
    
    @GrpcClient("okex-server")
    private IndicatorServiceGrpc.IndicatorServiceBlockingStub blockingStub;
    
    @GrpcClient("okex-server")
    private IndicatorServiceGrpc.IndicatorServiceStub asyncStub;
    
    public IndicatorResponse calculateRSI(String symbol, String interval, int period, int limit) {
        BaseRequest baseRequest = BaseRequest.newBuilder()
            .setSymbol(symbol)
            .setInterval(interval)
            .setLimit(limit)
            .build();
            
        RSIRequest request = RSIRequest.newBuilder()
            .setBase(baseRequest)
            .setPeriod(period)
            .build();
            
        return blockingStub.calculateRSI(request);
    }
    
    public IndicatorResponse calculateBOLL(String symbol, String interval, int period, double stdDev, int limit) {
        BaseRequest baseRequest = BaseRequest.newBuilder()
            .setSymbol(symbol)
            .setInterval(interval)
            .setLimit(limit)
            .build();
            
        BOLLRequest request = BOLLRequest.newBuilder()
            .setBase(baseRequest)
            .setPeriod(period)
            .setStdDev(stdDev)
            .build();
            
        return blockingStub.calculateBOLL(request);
    }
    
    public IndicatorResponse calculateMACD(String symbol, String interval, 
                                          int fastPeriod, int slowPeriod, int signalPeriod, int limit) {
        BaseRequest baseRequest = BaseRequest.newBuilder()
            .setSymbol(symbol)
            .setInterval(interval)
            .setLimit(limit)
            .build();
            
        MACDRequest request = MACDRequest.newBuilder()
            .setBase(baseRequest)
            .setFastPeriod(fastPeriod)
            .setSlowPeriod(slowPeriod)
            .setSignalPeriod(signalPeriod)
            .build();
            
        return blockingStub.calculateMACD(request);
    }
}
```

#### 2.4.4 使用示例
```java
// 文件路径: okex-client/src/main/java/com/okex/client/example/ClientExample.java
package com.okex.client.example;

import com.okex.client.service.IndicatorClientService;
import com.okex.common.proto.IndicatorResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class ClientExample implements CommandLineRunner {
    
    @Autowired
    private IndicatorClientService indicatorClientService;
    
    @Override
    public void run(String... args) throws Exception {
        // RSI计算示例
        IndicatorResponse rsiResponse = indicatorClientService.calculateRSI(
            "BTC-USDT-SWAP", "1m", 14, 100
        );
        System.out.println("RSI结果: " + rsiResponse.getRsiValue());
        
        // 布林带计算示例
        IndicatorResponse bollResponse = indicatorClientService.calculateBOLL(
            "BTC-USDT-SWAP", "1m", 15, 2.0, 100
        );
        System.out.println("布林带上轨: " + bollResponse.getBollUpper());
        System.out.println("布林带中轨: " + bollResponse.getBollMiddle());
        System.out.println("布林带下轨: " + bollResponse.getBollLower());
        
        // MACD计算示例
        IndicatorResponse macdResponse = indicatorClientService.calculateMACD(
            "BTC-USDT-SWAP", "1m", 12, 26, 9, 100
        );
        System.out.println("MACD线: " + macdResponse.getMacdLine());
        System.out.println("信号线: " + macdResponse.getMacdSignal());
        System.out.println("柱状图: " + macdResponse.getMacdHistogram());
    }
}
```

## 3. 部署和配置

### 3.1 服务端配置文件
```yaml
# 文件路径: okex-server/src/main/resources/application.yml
server:
  port: 8080

grpc:
  server:
    port: 50051

spring:
  application:
    name: okex-server

# MongoDB配置
mongodb:
  host: localhost
  port: 27017
  database: okex_data

# Redis配置
redis:
  host: localhost
  port: 6379
  database: 0

# WebSocket配置
websocket:
  okex:
    url: wss://ws.okx.com:8443/ws/v5/public
    reconnect_interval: 5000
    heartbeat_interval: 30000

# 日志配置
logging:
  level:
    com.okex: DEBUG
    root: INFO
```

### 3.2 客户端配置文件
```yaml
# 文件路径: okex-client/src/main/resources/application.yml
spring:
  application:
    name: okex-client

grpc:
  client:
    okex-server:
      address: 'static://127.0.0.1:50051'
      negotiation-type: plaintext

# 日志配置
logging:
  level:
    com.okex: DEBUG
    root: INFO
```

## 4. 构建和运行

### 4.1 构建命令
```bash
# 构建整个项目
mvn clean install

# 单独构建各个模块
mvn clean install -pl okex-common
mvn clean install -pl okex-server
mvn clean install -pl okex-client
```

### 4.2 运行服务
```bash
# 运行服务端
java -jar okex-server/target/okex-server-1.0.0.jar

# 运行客户端
java -jar okex-client/target/okex-client-1.0.0.jar
```

## 5. 监控和运维

### 5.1 健康检查
- gRPC服务状态监控
- WebSocket连接状态监控
- MongoDB连接监控
- Redis连接监控

### 5.2 日志管理
- 结构化日志输出
- 性能指标记录
- 错误日志追踪

### 5.3 性能优化
- 连接池配置
- 缓存策略优化
- 批量操作优化

这个架构设计提供了清晰的模块分离，便于开发、测试和维护。