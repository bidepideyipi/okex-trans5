# Mock使用技巧详解

本文档基于 `OkexWebSocketClientTest.shouldScheduleReconnectOnTransportError()` 测试方法，详细讲解在单元测试中使用Mock的各种技巧。

## 测试目标

验证当WebSocket发生传输错误时，系统会自动调度重连，并且第一次重连延迟使用斐波那契退避算法（1000ms × fib(1) = 1000ms）。

## 完整测试代码

```java
@Test
void shouldScheduleReconnectOnTransportError() throws Exception {
    SubscriptionConfigLoader loader = Mockito.mock(SubscriptionConfigLoader.class);
    OkexMessageParser parser = Mockito.mock(OkexMessageParser.class);
    CandleBatchWriter batchWriter = Mockito.mock(CandleBatchWriter.class);
    OkexWebSocketClient client = new OkexWebSocketClient(loader, parser, batchWriter);

    TestScheduler scheduler = new TestScheduler();
    ReflectionTestUtils.setField(client, "scheduler", scheduler);
    ReflectionTestUtils.setField(client, "initialReconnectIntervalMs", 1000L);
    ReflectionTestUtils.setField(client, "maxReconnectAttempts", 3);

    WebSocketSession session = Mockito.mock(WebSocketSession.class);
    ReflectionTestUtils.setField(client, "session", session);

    // Create OkexWebSocketHandler instance via reflection
    Object handler = null;
    for (Class<?> innerClass : OkexWebSocketClient.class.getDeclaredClasses()) {
        if (innerClass.getSimpleName().equals("OkexWebSocketHandler")) {
            java.lang.reflect.Constructor<?> ctor = innerClass.getDeclaredConstructor(OkexWebSocketClient.class);
            ctor.setAccessible(true);
            handler = ctor.newInstance(client);
            break;
        }
    }

    Assertions.assertNotNull(handler, "OkexWebSocketHandler not found via reflection");
    org.springframework.web.socket.WebSocketHandler wsHandler =
            (org.springframework.web.socket.WebSocketHandler) handler;

    wsHandler.handleTransportError(session, new RuntimeException("test-error"));

    Assertions.assertFalse(scheduler.scheduledDelaysMs.isEmpty(),
            "Expected reconnect to be scheduled on transport error");
    Assertions.assertEquals(1000L, scheduler.scheduledDelaysMs.get(0));
}
```

## Mock技巧详解

### 1. 基础Mock对象创建

```java
SubscriptionConfigLoader loader = Mockito.mock(SubscriptionConfigLoader.class);
OkexMessageParser parser = Mockito.mock(OkexMessageParser.class);
CandleBatchWriter batchWriter = Mockito.mock(CandleBatchWriter.class);
```

**技巧说明**：
- 使用 `Mockito.mock()` 创建依赖对象的**模拟版本**
- 这些mock对象不会执行真实逻辑，避免了对外部组件的依赖
- 适用于**构造函数注入**的场景，将mock对象传给被测类
- **优点**：
  - 隔离测试：不依赖真实的配置加载器、解析器和批量写入器
  - 快速执行：无需初始化复杂的依赖
  - 可控行为：可以通过 `when(...).thenReturn(...)` 控制返回值

### 2. 自定义测试替身（Test Double）

```java
TestScheduler scheduler = new TestScheduler();
```

**TestScheduler实现**：
```java
private static class TestScheduler extends ScheduledThreadPoolExecutor {
    private final List<Long> scheduledDelaysMs = new ArrayList<>();

    TestScheduler() {
        super(1);
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        scheduledDelaysMs.add(unit.toMillis(delay));  // 记录延迟
        return Mockito.mock(ScheduledFuture.class);    // 返回mock的Future
    }
}
```

**技巧说明**：
- 没有使用Mockito mock `ScheduledExecutorService`，而是继承 `ScheduledThreadPoolExecutor` 创建自定义实现
- **为什么这样做**？
  - 需要**捕获调度任务的延迟时间**（这是测试的关键断言点）
  - Mockito虽然可以mock，但验证参数会很复杂
  - 自定义实现可以在 `schedule()` 方法中记录延迟值到 `scheduledDelaysMs` 列表
- **关键设计**：
  - 继承真实类而非实现接口，复用现有功能
  - 重写关键方法以捕获调用信息
  - 不执行实际的定时任务（避免异步复杂性）
  - 返回mock的 `ScheduledFuture` 满足返回值要求

**何时使用自定义Test Double**：
- 需要捕获方法调用的参数
- 需要验证调用的顺序或次数
- 需要在方法内执行额外的验证逻辑
- Mock框架难以表达复杂的验证需求

### 3. 使用ReflectionTestUtils注入mock对象

```java
ReflectionTestUtils.setField(client, "scheduler", scheduler);
ReflectionTestUtils.setField(client, "initialReconnectIntervalMs", 1000L);
ReflectionTestUtils.setField(client, "maxReconnectAttempts", 3);
ReflectionTestUtils.setField(client, "session", session);
```

**技巧说明**：
- Spring Test提供的**反射工具**，可以访问和修改**私有字段**
- **应用场景**：
  - `scheduler`：替换真实的定时任务执行器为测试版本
  - `initialReconnectIntervalMs`、`maxReconnectAttempts`：设置测试用的配置值
  - `session`：注入mock的WebSocket会话对象
- **优点**：
  - 不需要修改生产代码添加setter方法
  - 不破坏封装性（生产代码保持私有字段）
  - 灵活控制测试条件
- **替代方案对比**：

| 方案 | 优点 | 缺点 |
|------|------|------|
| 添加setter方法 | 简单直接 | 破坏封装，可能被误用 |
| 添加package-private构造器 | 不暴露公共API | 需要测试和代码在同一包 |
| ReflectionTestUtils | 不修改生产代码 | 反射性能略低（测试场景可忽略） |

**注意事项**：
- 这是测试代码的特权，生产代码应避免反射修改私有字段
- 字段名必须精确匹配，否则会抛异常
- 类型不匹配会导致运行时错误

### 4. 通过反射获取内部类实例（高级技巧）

```java
Object handler = null;
for (Class<?> innerClass : OkexWebSocketClient.class.getDeclaredClasses()) {
    if (innerClass.getSimpleName().equals("OkexWebSocketHandler")) {
        java.lang.reflect.Constructor<?> ctor = 
            innerClass.getDeclaredConstructor(OkexWebSocketClient.class);
        ctor.setAccessible(true);
        handler = ctor.newInstance(client);
        break;
    }
}
```

**技巧说明**：
- **问题**：`OkexWebSocketHandler` 是 `OkexWebSocketClient` 的**私有内部类**，测试代码无法直接访问
- **解决方案步骤**：
  1. 使用 `getDeclaredClasses()` 获取外部类的所有内部类
  2. 通过名称匹配找到目标内部类（`getSimpleName().equals("OkexWebSocketHandler")`）
  3. 获取内部类的构造器（非静态内部类的构造器需要外部类实例作为参数）
  4. 使用 `setAccessible(true)` 绕过访问权限检查
  5. 调用 `newInstance(client)` 创建内部类实例

**为什么需要传 `client`**？
- 非静态内部类持有外部类的引用
- 构造时需要传入外部类实例作为隐式参数
- 如果是静态内部类，则不需要外部类实例

**应用场景**：
- 测试私有内部类的行为（如WebSocket事件处理器）
- 测试回调类或监听器
- 测试策略模式的内部实现类

**替代方案**：
- 将内部类提取为独立的包私有类（更易测试，但增加类数量）
- 通过外部类的公共方法间接触发内部类逻辑（集成测试风格）

### 5. Mock对象的类型转换与断言

```java
Assertions.assertNotNull(handler, "OkexWebSocketHandler not found via reflection");
org.springframework.web.socket.WebSocketHandler wsHandler =
        (org.springframework.web.socket.WebSocketHandler) handler;
```

**技巧说明**：
- **防御性断言**：先断言handler不为null，确保反射成功
  - 如果反射失败，测试会立即失败并给出清晰的错误消息
  - 避免后续的 `NullPointerException`
- **类型转换**：将 `Object` 类型转换为Spring的 `WebSocketHandler` 接口类型
  - 这样才能调用接口方法 `handleTransportError()`
  - 如果类型不兼容，会在此处抛出 `ClassCastException`（比在后续调用时更早发现问题）

**最佳实践**：
- 反射操作后立即断言结果非空
- 使用有意义的错误消息
- 尽早进行类型转换以快速失败

### 6. Mock WebSocketSession

```java
WebSocketSession session = Mockito.mock(WebSocketSession.class);
```

**技巧说明**：
- Spring WebSocket的会话对象，包含大量方法
- 使用mock避免创建真实WebSocket连接
- **无需设置行为**：测试中不需要设置任何行为（`when(...).thenReturn(...)`）
- **原因**：
  - `handleTransportError()` 方法内部只是记录日志和调用重连逻辑
  - 不需要session返回特定值
  - 传递mock对象只是为了满足方法签名

**何时需要设置Mock行为**：
```java
// 如果代码依赖session.isOpen()的返回值
Mockito.when(session.isOpen()).thenReturn(true);

// 如果代码调用session.getAttributes()
Mockito.when(session.getAttributes()).thenReturn(new HashMap<>());
```

**最佳实践**：
- 默认不设置行为（Mockito返回合理的默认值）
- 只在代码依赖返回值时才设置行为
- 避免过度配置mock（增加维护成本）

### 7. 行为验证而非状态验证

```java
wsHandler.handleTransportError(session, new RuntimeException("test-error"));

Assertions.assertFalse(scheduler.scheduledDelaysMs.isEmpty(),
        "Expected reconnect to be scheduled on transport error");
Assertions.assertEquals(1000L, scheduler.scheduledDelaysMs.get(0));
```

**技巧说明**：
- **行为验证**：检查 `scheduler.scheduledDelaysMs` 是否记录了调度任务
- **而非状态验证**：不检查client内部状态（如reconnectAttempts字段值）
- **验证点**：
  1. 确认有重连任务被调度（列表非空）
  2. 确认第一次重连延迟为1000ms（符合斐波那契退避算法）

**两种验证方式对比**：

| 验证方式 | 示例 | 优点 | 缺点 |
|---------|------|------|------|
| **状态验证** | `assertEquals(1, client.getReconnectAttempts())` | 直观，易理解 | 依赖内部状态，脆弱 |
| **行为验证** | `assertTrue(scheduler.scheduledDelaysMs.contains(1000L))` | 关注输出行为，鲁棒 | 需要额外的捕获机制 |

**最佳实践**：
- 优先验证**对外部的影响**（调用其他对象的方法、产生的输出）
- 避免验证**内部实现细节**（私有字段值、内部状态）
- 使用 `Mockito.verify()` 验证方法调用：
  ```java
  Mockito.verify(mockObject).someMethod(argumentCaptor.capture());
  ```

## Mock使用技巧总结

| 技巧 | 适用场景 | 示例 | 何时使用 |
|------|---------|------|---------|
| **Mockito.mock()** | 快速创建接口/类的模拟对象 | `mock(Parser.class)` | 大部分依赖对象 |
| **自定义Test Double** | 需要捕获方法调用参数或执行特定逻辑 | `TestScheduler extends ScheduledThreadPoolExecutor` | 需要复杂验证逻辑 |
| **ReflectionTestUtils** | 注入私有字段，修改配置值 | `setField(obj, "field", value)` | 不想修改生产代码 |
| **反射获取内部类** | 测试私有内部类的行为 | `getDeclaredClasses()` + `getDeclaredConstructor()` | 内部类包含关键逻辑 |
| **行为验证** | 验证方法被调用及其参数 | 检查 `scheduledDelaysMs` 列表内容 | 关注对外部影响 |
| **不设置行为的Mock** | 当方法不依赖返回值时 | `mock(WebSocketSession.class)` 无需 `when()` | 仅需满足方法签名 |

## 测试设计原则

### 1. 隔离性（Isolation）
- 每个测试只测试一个单元（类或方法）
- 使用Mock隔离外部依赖
- 示例：mock了所有OkexWebSocketClient的依赖

### 2. 可重复性（Repeatability）
- 测试结果不依赖外部系统（数据库、网络）
- 使用自定义TestScheduler避免真实的异步调度
- 每次运行结果一致

### 3. 快速执行（Speed）
- Mock避免了真实依赖的初始化开销
- 不等待真实的定时任务执行
- 整个测试在毫秒级完成

### 4. 清晰的意图（Clarity）
- 测试名称明确：`shouldScheduleReconnectOnTransportError`
- 断言消息清晰：`"Expected reconnect to be scheduled on transport error"`
- 一个测试验证一个行为

## 进阶技巧

### ArgumentCaptor：捕获方法参数

```java
@Test
void shouldSendCorrectReconnectDelay() {
    ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
    ArgumentCaptor<Long> delayCaptor = ArgumentCaptor.forClass(Long.class);
    
    ScheduledExecutorService mockScheduler = Mockito.mock(ScheduledExecutorService.class);
    
    // 触发重连逻辑...
    
    Mockito.verify(mockScheduler).schedule(
        runnableCaptor.capture(), 
        delayCaptor.capture(), 
        eq(TimeUnit.MILLISECONDS)
    );
    
    assertEquals(1000L, delayCaptor.getValue());
}
```

### InOrder：验证调用顺序

```java
@Test
void shouldCloseSessionBeforeReconnect() {
    WebSocketSession session = Mockito.mock(WebSocketSession.class);
    OkexWebSocketClient client = ...;
    
    // 触发错误和重连...
    
    InOrder inOrder = Mockito.inOrder(session, scheduler);
    inOrder.verify(session).close();
    inOrder.verify(scheduler).schedule(any(), anyLong(), any());
}
```

### Answer：自定义返回逻辑

```java
Mockito.when(mockObject.complexMethod(any())).thenAnswer(invocation -> {
    Object arg = invocation.getArgument(0);
    // 基于参数返回不同结果
    return processArg(arg);
});
```

## 常见陷阱与解决方案

### 陷阱1：过度Mock
**问题**：Mock了所有内容，测试变成了"Mock的测试"而非"代码的测试"

**解决**：
- 只mock外部依赖（数据库、网络、文件系统）
- 真实对象能用则用（如简单的POJO）
- 集成测试中减少mock使用

### 陷阱2：依赖内部实现
**问题**：验证私有方法调用次数，重构时测试大量失败

**解决**：
- 验证公共行为和输出
- 使用黑盒测试思维
- 重构不应破坏测试（只要行为不变）

### 陷阱3：Mock静态方法困难
**问题**：Mockito默认不支持静态方法mock

**解决**：
- 使用Mockito 3.4+的 `mockStatic()`
- 或重构代码避免静态方法
- 或使用PowerMock（已不推荐）

### 陷阱4：忘记验证Mock调用
**问题**：测试通过但代码没有调用预期的方法

**解决**：
```java
// 验证方法被调用
Mockito.verify(mockObject).expectedMethod();

// 验证方法未被调用
Mockito.verify(mockObject, never()).unexpectedMethod();

// 验证调用次数
Mockito.verify(mockObject, times(2)).someMethod();
```

## 参考资源

- [Mockito官方文档](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [Spring ReflectionTestUtils](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/test/util/ReflectionTestUtils.html)
- [Martin Fowler - Mocks Aren't Stubs](https://martinfowler.com/articles/mocksArentStubs.html)
- [Test Double模式](http://xunitpatterns.com/Test%20Double.html)

## 相关测试用例

- `OkexWebSocketClientTest.shouldSendSubscribeMessageOnInitialConfig()` - 基础Mock使用
- `OkexWebSocketClientTest.shouldSendUnsubscribeAndSubscribeOnConfigChange()` - ArgumentCaptor使用
- `CandleBatchWriterTest` - Mock Repository验证批量写入行为
