# backend

`ai-customer-service-system` 的 Java 后端，使用 Java 21、Spring Boot 3.5、Spring MVC、Validation、JDBC、PostgreSQL 和 pgvector。

## 分层

- `controller`：REST 参数接收、校验、requestId 和 DTO 转换。
- `dto`：稳定的接口契约，不把数据库对象直接暴露给页面。
- `service`：会话状态机、AI 编排、工单 SLA、订单和知识生命周期。
- `dao`：PostgreSQL 与 memory 两套实现，上层 Service 不依赖具体存储。
- `entity`：会话、消息、客户、订单、工单、知识、工具轨迹和 AI Run。
- `ai`：DeepSeek、Ollama、ChatGPT 共用的 OpenAI-compatible 客户端。
- `common`：业务异常、错误响应和哈希工具。

## 会话状态机

`ConversationService` 是客服收件箱的核心业务层：

```text
WAITING → IN_PROGRESS → WAITING_CUSTOMER / WAITING_INTERNAL → RESOLVED
```

- `claim`：只有待领取会话可以进入处理中；同一处理人重复领取保持幂等。
- `assign`：主管可以转接，系统写入内部事件。
- `reply`：只有当前处理人可以发送客户可见消息。
- `notes`：只有当前处理人可以添加内部备注。
- `wait-customer`：记录等待原因并切换状态。
- `resolve`：记录解决结论；解决后禁止继续写入。

PostgreSQL DAO 使用事务同时更新会话主数据和消息，memory DAO 也在同一同步临界区完成两次写入，避免出现状态已变但事件丢失的半成功结果。

## 存储模式

memory：

```bash
SUPPORT_AUTH_ENABLED=false SERVER_ADDRESS=127.0.0.1 SUPPORT_STORAGE=memory SERVER_PORT=18081 mvn spring-boot:run
```

postgres：

```bash
docker compose -f docker/local/docker-compose.yml up -d
SUPPORT_STORAGE=postgres SERVER_PORT=18081 mvn spring-boot:run
```

`AiCustomerServiceApplication` 关闭了 Spring Boot 无条件数据源自动装配，`CustomerServiceConfiguration` 再根据 `support.storage.type` 创建对应 DAO。memory 模式不会尝试连接 PostgreSQL。

## AI 编排与审计

`CustomerSupportService` 依次完成：

1. 读取客户画像。
2. 识别意图。
3. 检索已发布知识。
4. 按意图调用订单、物流或退款工具。
5. 执行 Java 风险护栏。
6. 调用真实模型生成回复草稿。
7. 高风险场景创建或复用工单。
8. 写入会话摘要和 AI Run。

`AiRunService` 以 requestId 保存 Provider、模型、意图置信度、知识版本、工具摘要、护栏、耗时和转人工结果。审计写入失败只记录告警，不会吞掉已经生成的客服回复。

## 测试与打包

```bash
mvn test
mvn clean package
```

最终产物：

```text
target/ai-customer-service-system-0.0.1-SNAPSHOT.jar
```

运行：

```bash
DEEPSEEK_API_KEY=你的Key \
AI_PROVIDER=deepseek \
SUPPORT_STORAGE=memory \
SERVER_PORT=18081 \
java -jar target/ai-customer-service-system-0.0.1-SNAPSHOT.jar
```

浏览器打开 `http://localhost:18081`，页面和 API 由同一个 JAR 提供。


## 2026-09-09 运行变更

默认启用 Bearer 身份验证，先配置 `SUPPORT_AUTH_IDENTITIES` 和 `SUPPORT_TENANT`；具体字段及浏览器配置见项目根 README。上方 memory 命令仅限本机无登录演示。已有 PostgreSQL 数据库必须执行 `src/main/resources/db/postgres/02_production_hardening.sql`；新数据卷会自动运行该脚本。

AI 编排前先写 RUNNING，失败与并发拒绝也写入审计；真实 usage、Prompt 版本和错误码可以在 AI 运行审计详情查看。热点缓存具有 TTL/LRU，模型入口使用信号量限制并发。真实向量通过 `RAG_EMBEDDING_MODE=remote` 单独配置；Hashing 仍是离线模式。知识同步失败后保持未发布，可以再次调用发布重试。
