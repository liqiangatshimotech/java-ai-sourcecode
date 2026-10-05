# ai-customer-service-system

面向电商售后场景的 AI 客服运营平台。这个项目不再把聊天、订单、工单和知识库堆在一个演示页面里，而是按真实客服业务拆成会话收件箱、坐席工作台、工单协作、订单售后、知识运营、RAG 评测和 AI 运行审计。

我保留了单体 Spring Boot 架构。这里真正需要解决的是业务状态、证据约束和人工协同，不是为了“企业级”提前拆微服务。

## 项目名称与目录

- 项目名称：`ai-customer-service-system`
- 后端：`backend`，Java 21 + Spring Boot 3.5 + JDBC
- 前端：`frontend`，零依赖多页面应用
- 默认模型：DeepSeek
- 可切换模型：Ollama、ChatGPT
- 生产存储：PostgreSQL 16 + pgvector
- 快速体验存储：进程内 memory 模式

Maven 会把 `frontend` 复制到 Spring Boot 的 `classpath:/static`。最终 JAR 同时提供页面和 API，运行时不需要 Node.js，也不需要另起静态文件服务器。

## 业务闭环

```text
客户从官网 / App / 微信 / 邮件进线
        ↓
会话进入队列，按优先级和 SLA 排队
        ↓
坐席领取会话，查看客户 360 与关联订单
        ↓
AI 识别意图、检索知识、查询业务工具并生成建议
        ↓
Java 护栏判断是否允许直接回复或必须人工确认
        ↓
坐席编辑并发送，必要时创建工单跨团队协作
        ↓
等待客户 / 等待内部处理 / 解决会话
        ↓
requestId、知识版本、工具轨迹和护栏写入 AI Run 审计
```

会话状态由后端状态机控制：

```text
WAITING → IN_PROGRESS → WAITING_CUSTOMER / WAITING_INTERNAL → RESOLVED
```

领取、转接、回复、内部备注、等待客户和解决会话都会写入消息或系统事件。已解决会话不能继续发送消息，其他坐席也不能越过当前负责人直接操作。

## 页面

| 页面 | 主要用途 |
| --- | --- |
| 运营驾驶舱 | 当前排队、服务中会话、SLA 风险、首响、AI 采纳率、实时队列和异常提醒 |
| 会话收件箱 | 队列筛选、完整消息时间线、领取/转接/等待/解决、客户 360、订单上下文、AI 回复建议 |
| 工单协作 | 工单筛选与独立详情页，处理负责人、内部备注、解决结论和事件时间线 |
| 订单与售后 | 订单查询与独立详情页，履约信息、风险标签和受控状态动作 |
| 知识运营 | 草稿、提交审核、审核发布、下线和 Chunk 预览 |
| RAG 评测实验室 | 使用带预期文档的黄金问题验证 Hit@K、首位命中、相似度和知识版本 |
| AI 运行审计 | 按 requestId 回溯模型、意图、知识、工具、护栏、耗时和转人工结果 |
| 平台设置 | 查看 DeepSeek / Ollama / ChatGPT 配置、开发连接、团队队列和渠道接入 |

## 快速启动：memory 模式

有 DeepSeek Key 时，在项目根目录执行：

```bash
DEEPSEEK_API_KEY=你的Key \
AI_PROVIDER=deepseek \
SUPPORT_AUTH_ENABLED=false SERVER_ADDRESS=127.0.0.1 SUPPORT_STORAGE=memory \
SERVER_PORT=18081 \
mvn -f backend/pom.xml spring-boot:run
```

浏览器访问：

```text
http://localhost:18081
```

上面的无登录演示显式绑定 127.0.0.1。默认鉴权模式需先按文末“启动与访问身份”配置服务端凭据。memory 模式不创建数据库连接，订单、工单、知识、会话、消息和 AI Run 都有可操作的种子数据。数据在进程重启后恢复为初始状态。

没有模型 Key 时服务仍能启动，会话、工单、订单和知识流程都可以操作；点击“生成 AI 建议”时会明确返回模型配置错误，不会伪造一段规则答案冒充模型结果。

## PostgreSQL + pgvector 模式

先启动数据库：

```bash
docker compose -f backend/docker/local/docker-compose.yml up -d
docker exec ai-customer-service-postgres \
  pg_isready -U ai_customer_service -d ai_customer_service
```

再启动应用：

```bash
DEEPSEEK_API_KEY=你的Key \
AI_PROVIDER=deepseek \
SUPPORT_STORAGE=postgres \
SERVER_PORT=18081 \
mvn -f backend/pom.xml spring-boot:run
```

PostgreSQL 模式会持久化：

- `cs_conversation`：会话主数据、队列、负责人、状态和 SLA 时间
- `cs_conversation_message`：客户消息、坐席回复、内部备注和系统事件
- `cs_ticket` / `cs_ticket_timeline`：工单与协作时间线
- `cs_order`：订单和履约状态
- `cs_knowledge_document` / `cs_knowledge_chunk`：知识版本、切分和向量
- `cs_ai_run`：模型、意图、知识、工具、护栏和耗时审计

## 模型切换

DeepSeek 是默认 Provider：

```bash
DEEPSEEK_API_KEY=你的Key AI_PROVIDER=deepseek mvn -f backend/pom.xml spring-boot:run
```

本地 Ollama：

```bash
AI_PROVIDER=ollama \
OLLAMA_BASE_URL=http://localhost:11434/v1 \
OLLAMA_MODEL=qwen2.5:7b \
mvn -f backend/pom.xml spring-boot:run
```

ChatGPT / OpenAI：

```bash
OPENAI_API_KEY=你的Key AI_PROVIDER=chatgpt mvn -f backend/pom.xml spring-boot:run
```

API Key 只从环境变量读取。页面、代码和数据库都不会保存或回显密钥。

## 关键 API

```text
GET  /api/support/conversations
GET  /api/support/conversations/{conversationId}
GET  /api/support/conversations/dashboard
POST /api/support/conversations/{conversationId}/claim
POST /api/support/conversations/{conversationId}/assign
POST /api/support/conversations/{conversationId}/reply
POST /api/support/conversations/{conversationId}/notes
POST /api/support/conversations/{conversationId}/wait-customer
POST /api/support/conversations/{conversationId}/resolve

POST /api/support/chat
GET  /api/support/ai-runs
GET  /api/support/ai-runs/{requestId}

GET  /api/support/orders
GET  /api/support/tickets
GET  /api/support/knowledge/documents
GET  /api/support/knowledge/search
```

## 固定演示链路

打开“会话收件箱”，选择 `CONV-20260710-001`：

1. 企业客户通过微信咨询 5600 元活动物料订单退款。
2. 会话是 `URGENT` 且尚未领取。
3. 领取会话后可以查看客户等级、关联订单和历史消息。
4. AI 检索退款知识并查询订单；高价值订单和仓库拦截触发人工确认。
5. 坐席采用或修改 AI 草稿后发送，不会由模型直接向客户承诺退款。
6. 需要跨团队处理时进入工单协作；处理完成后解决会话。
7. AI 运行审计页可以按 requestId 回看当时使用的知识版本、工具和护栏。

## 验证

运行全部后端测试：

```bash
mvn -f backend/pom.xml test
```

当前测试覆盖 35 项，包括：

- `claim → reply → wait-customer → resolve` 完整会话链路
- 重复领取幂等、转接和越权状态检查
- 会话 Dashboard 与 SLA 统计
- 工单幂等、分配、备注和关闭
- 订单查询与状态更新
- RAG、知识生命周期和生产护栏
- DeepSeek / Ollama / ChatGPT 配置
- Controller 参数校验与响应字段

健康检查：

```bash
curl -s http://localhost:18081/actuator/health
curl -s http://localhost:18081/api/support/conversations/dashboard
curl -s http://localhost:18081/api/support/ai-runs
```

更细的后端分层与接口说明见 `backend/README.md`，页面交互说明见 `frontend/README.md`。


## 2026-09-09：把客服项目接入生产验证

项目名称：`ai-customer-service-system`。代码路径：`java-ai-course-examples/chapter10/ai-customer-service-system`。

我把最容易在联调时漏掉的边界落实到了代码中：

- 进入 AI 编排前写入 `RUNNING`，成功写 `SUCCEEDED` / `CACHED`，异常写 `FAILED` / `REJECTED`。记录真实模型返回名、Prompt 版本以及供应商 usage。供应商不返回 Token 时保存 `null`；缓存命中本次模型用量为 0。
- `QueuedModelExecutor` 使用最多 16 个 Semaphore 许可，超额立即返回 HTTP 429，不排无限队列；HTTP 或业务异常都在 finally 释放许可。`modelTier` 是业务标记，不是实际自动切换模型。
- 热点缓存默认 5 分钟 TTL、1000 项 LRU；key 包含客户、历史摘要和全部检索证据的版本/内容。历史最多 5 条、每段 800 字、手机号脱敏；只放 AI 问答，不读取内部备注，按客户和会话隔离。它仍是进程内缓存，重启后清空。
- 物流查询先确认订单属于当前客户。退款只判断资格和创建人工工单，不发起实际退款。
- 知识发布先完成 chunk/向量同步，最后保存 `PUBLISHED`；向量检索同时检查主文档发布状态、同步状态、模型及维度。发布中断后文档仍是草稿/审核态，可重试相同发布接口；下线失败也可再次调用下线重试清理。这个最小实现不包含跨进程任务调度或通用 outbox。

### 启动与访问身份

默认启用服务端 Bearer 凭据验证。通过 `SUPPORT_AUTH_IDENTITIES` 环境变量提供 JSON 数组，每个元素含 `token`（至少 24 位随机值）、`user`、`tenant`、`role`。角色取 `viewer`、`agent` 或 `admin`；`SUPPORT_TENANT` 指定此部署的租户，默认 `course`。令牌属于其他租户时返回 403；它是“一租户一实例”的隔离方式，并没有实现多租户共库。

`viewer` 只读；`agent` 可生成建议和处理会话；知识写入、订单状态变更、会话/工单分配需要 `admin`。操作人取验证后的身份，JSON 的 operator 字段仅保留接口兼容，不作为身份依据。浏览器设置页输入客服访问凭据，保存在 sessionStorage。模型 API Key 只放后端环境变量。

本机无登录演示可以显式设置 `SUPPORT_AUTH_ENABLED=false`；此模式不具备鉴权，不能用于公开部署。默认模式没有配置凭据时会拒绝所有 API 请求。

从解压后的 `java-ai-course-examples` 目录运行：

```bash
mvn -f chapter10/ai-customer-service-system/backend/pom.xml spring-boot:run
mvn -f chapter10/ai-customer-service-system/backend/pom.xml test
```

### 数据库升级与真实语义向量

新建 Docker 数据卷自动执行 `01_schema.sql` 和 `02_production_hardening.sql`；已有数据库必须先手工执行 `backend/src/main/resources/db/postgres/02_production_hardening.sql`。迁移增加审计字段并把向量列改成可容纳不同维度的 vector；小数据集使用精确距离排序，移除固定 64 维 HNSW 索引。

聊天仍默认 DeepSeek，支持 `AI_PROVIDER=ollama` 和 `AI_PROVIDER=chatgpt`。Embedding 单独配置，不能用 DeepSeek 聊天模型充当语义向量模型：

```bash
export RAG_EMBEDDING_MODE=remote
export RAG_EMBEDDING_BASE_URL=http://localhost:11434/v1
export RAG_EMBEDDING_MODEL=nomic-embed-text
export RAG_EMBEDDING_DIMENSIONS=768
```

Ollama 本机先拉取对应 embedding 模型。OpenAI 服务可配置其 `/v1` 地址、向量模型名称与实际返回维度，并通过 `RAG_EMBEDDING_API_KEY` 提供密钥。当前适配器不发送模型特有的缩维参数；预期维度必须与模型实际输出一致。改变模型或维度后重启会重新生成已发布知识的向量；HTTP 失败、零向量和维度不匹配明确报错。默认 `RAG_EMBEDDING_MODE=hashing` / 64 维是离线算法演示，不能当作真实语义检索效果。

### 与第 12 章联调

使用专用测试实例和种子数据启动本项目，再运行 `chapter12/ai-production-readiness-demo` 的 `CustomerServiceEvaluationDemo`。评测器逐题访问 chat API，再读取对应 AI Run，保存每题 NDJSON 和汇总 JSON，以通过率、p95 延迟和零红线失败决定是否允许发布。评测可能创建测试工单，所以不要对日常使用的业务实例执行。

自动测试使用本地 HTTP mock 验证聊天与 Embedding 适配器、usage、历史输入、并发拒绝与异常释放、缓存 TTL/LRU、发布失败重试和身份/租户边界；没有伪称完成外部模型调用或真实线上验收。
