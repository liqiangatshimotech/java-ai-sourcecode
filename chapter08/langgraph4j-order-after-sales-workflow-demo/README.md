# langgraph4j-order-after-sales-workflow-demo

这是一个订单售后工作流 Demo，重点不是“多调一次模型”，而是把长流程变成可观察、可暂停、可恢复的显式状态图。

- 项目名称：`langgraph4j-order-after-sales-workflow-demo`
- 项目路径：`java-ai-course-examples/chapter08/langgraph4j-order-after-sales-workflow-demo`
- Java：17+
- LangGraph4j：锁定 `1.8.23`
- Spring AI：锁定 `2.0.0`
- Spring Boot：`4.1.0`

没有 API Key 也能跑完全部测试和本地演示。默认供应商配置是 DeepSeek，但 `AI_ENABLED` 默认为 `false`，此时使用可重复的离线规则助手。显式启用 `deepseek` / `ollama` / `openai` profile 后，模型层才会发出真实请求。

## 状态图

```text
START
  → understand_request
      ├─ 缺订单号 → ask_for_information → [interrupt: wait_for_information]
      │                                      ↓ 恢复后回到 understand_request
      └─ 信息完整 → load_order
                         ├─ 普通咨询 → answer_consultation → build_response → END
                         └─ 退款 → prepare_refund_checks
                                      ├─ policy_check ─┐
                                      └─ risk_check   ─┴→ evaluate_refund
                                                              ├─ 低风险 → execute_refund
                                                              ├─ 中风险 → [interrupt: human_approval]
                                                              └─ 拒绝 → build_response
```

`prepare_refund_checks` 同时指向两个节点，LangGraph4j 会按 fork-join 模式并行执行政策和风险检查，再在 `evaluate_refund` 汇合。两个分支都会写 `trace`，`AfterSalesState.SCHEMA` 中的 `Channels.appender` reducer 会合并分支结果并去掉 ParallelNode 二次合并完整 state 带来的重复轨迹。

## 快速运行

在 `java-ai-course-examples` 目录下执行：

```bash
# 离线测试，不会访问任何模型 API
mvn -f chapter08/langgraph4j-order-after-sales-workflow-demo/pom.xml test

# 启动离线演示，默认端口 8080
mvn -f chapter08/langgraph4j-order-after-sales-workflow-demo/pom.xml spring-boot:run
```

启动一个缺订单号的退款流程：

```bash
curl -X POST http://localhost:8080/api/after-sales/workflows \
  -H 'Content-Type: application/json' \
  -d '{"threadId":"demo-refund-1","message":"我想申请退款"}'
```

返回值的 `status` 是 `WAITING_FOR_INFORMATION`，`nextNode` 是 `wait_for_information`。补充订单号时不会重新起一条流程，而是读取同一个 threadId 的 checkpoint：

```bash
curl -X POST http://localhost:8080/api/after-sales/workflows/demo-refund-1/information \
  -H 'Content-Type: application/json' \
  -d '{"message":"订单号 ORD-1001"}'
```

`ORD-1001` 金额超过 100 元，风险等级为中，图会暂停在 `human_approval` 之前。审批后继续：

```bash
curl -X POST http://localhost:8080/api/after-sales/workflows/demo-refund-1/approval \
  -H 'Content-Type: application/json' \
  -d '{"decision":"APPROVED","approver":"reviewer-a"}'
```

`approver` 放在请求体里只是为了让 Demo 能独立运行。生产环境必须从已验证的登录上下文、审批系统回调或签名 token 取审批人，不能信任客户端自报的姓名。

另外两张内置订单用于走不同分支：

| 订单 | 金额 / 特征 | 预期路径 |
| --- | --- | --- |
| `ORD-1001` | 129 元 | 中风险，人工审批后恢复 |
| `ORD-1002` | 49 元 | 低风险，自动退款 |
| `ORD-9009` | 899 元且账号可疑 | 高风险拒绝，不执行资金操作 |

## SSE 节点流

`POST /api/after-sales/workflows/stream` 的请求体与启动接口一样。每完成一个图节点就发送 `node` 事件，执行结束或暂停后发送 `result` 事件。并行分支的汇总事件节点名形如 `__PARALLEL__(prepare_refund_checks)`。

SSE 只是观察通道，不是工作流的生命周期开关。浏览器中途断线后会停止推送，但图仍会继续执行并保存 checkpoint/终态。

```bash
curl -N -X POST http://localhost:8080/api/after-sales/workflows/stream \
  -H 'Content-Type: application/json' \
  -d '{"threadId":"demo-stream-1","message":"订单 ORD-1002 申请退款"}'
```

其他调试接口：

- `GET /api/after-sales/workflows/{threadId}`：当前 checkpoint
- `GET /api/after-sales/workflows/{threadId}/history`：从新到旧的 checkpoint 历史
- `GET /api/after-sales/workflows/diagram`：导出 Mermaid 图定义

## 模型配置

模型只负责意图理解和用户回复。政策判定、风险规则、审批结论和资金操作都是确定性 Java 代码。

| 供应商 | 启动 profile | 密钥 | 默认模型 | 默认地址 |
| --- | --- | --- | --- | --- |
| DeepSeek（默认） | `deepseek` | `DEEPSEEK_API_KEY` | `deepseek-flash` | `https://api.deepseek.com` |
| Ollama | `ollama` | 无 | `qwen2.5:7b` | `http://localhost:11434` |
| ChatGPT / OpenAI | `openai` | `OPENAI_API_KEY` | `gpt-4.1-mini` | `https://api.openai.com/v1` |

DeepSeek：

```bash
export DEEPSEEK_API_KEY='your-key'
mvn -f chapter08/langgraph4j-order-after-sales-workflow-demo/pom.xml spring-boot:run \
  -Dspring-boot.run.profiles=deepseek
```

Ollama：

```bash
ollama pull qwen2.5:7b
mvn -f chapter08/langgraph4j-order-after-sales-workflow-demo/pom.xml spring-boot:run \
  -Dspring-boot.run.profiles=ollama
```

OpenAI：

```bash
export OPENAI_API_KEY='your-key'
mvn -f chapter08/langgraph4j-order-after-sales-workflow-demo/pom.xml spring-boot:run \
  -Dspring-boot.run.profiles=openai
```

代码不会打印 API Key。`AiModelProperties.safeSummary()` 只会记录是否已配置密钥。

## Checkpoint：本地 Memory 与生产 MySQL

默认 `CHECKPOINT_TYPE=memory`，无需外部依赖，但应用重启后不能恢复。生产演示使用 LangGraph4j 1.8.23 官方 `MysqlSaver`：

```bash
export MYSQL_JDBC_URL='jdbc:mysql://localhost:3306/langgraph4j_demo?useSSL=false&allowPublicKeyRetrieval=true'
export MYSQL_JDBC_USER='course_user'
export MYSQL_JDBC_PASSWORD='your-password'
mvn -f chapter08/langgraph4j-order-after-sales-workflow-demo/pom.xml spring-boot:run \
  -Dspring-boot.run.profiles=mysql
```

`MysqlSaver.builder().dataSource(...).createOption(CREATE_IF_NOT_EXISTS).build()` 会维护 `LANGRAPH4J_THREAD` 和 `LANGRAPH4J_CHECKPOINT` 表。应用重启后，只要使用同一个 threadId，`stateOf` / `updateState` / `stream((Map<String, Object>) null, config)` 依然可以继续。这里对 `null` 显式转型，是为了避免 Java 在 `Map` 和 `GraphInput` 重载之间产生歧义。不要在生产使用 `CREATE_OR_REPLACE`，它会重建表并丢失 checkpoint。

如果需要同时启用真实模型和 MySQL，profile 可组合，例如 `deepseek,mysql`。

## 中断与恢复是怎么发生的

`AfterSalesWorkflow` 在编译图时显式配置：

```java
CompileConfig compileConfig = CompileConfig.builder()
        .checkpointSaver(checkpointSaver)
        .interruptBefore(WAIT_FOR_INFORMATION, HUMAN_APPROVAL)
        .releaseThread(false)
        .recursionLimit(40)
        .build();
```

恢复时先把用户输入写入 checkpoint，然后用 `null` 输入继续，不会从 START 重跑：

```java
StateSnapshot<AfterSalesState> snapshot = graph.stateOf(config).orElseThrow();
RunnableConfig updated = graph.updateState(snapshot.config(), resumeData);
for (NodeOutput<AfterSalesState> event : graph.stream((Map<String, Object>) null, updated)) {
    // 将每个已完成节点转成 SSE 事件
}
```

还有两层循环保护：`max-clarification-rounds` 限制业务上的补充次数，`recursion-limit` 是 LangGraph4j 全图保险丝。

## 幂等和失败处理

- 退款幂等键由 `after-sales:{threadId}` 生成；相同线程重试只返回同一张 `RefundReceipt`。
- 业务节点统一通过 `safe(...)` 包装，异常会写入 `error` / `failedNode` 并路由到 `failure`，便于 checkpoint 审计。
- 人工恢复时必须是 `APPROVED` 或 `REJECTED`，`PENDING` 不可执行。
- 每个 threadId 有进程内互斥锁，避免同一工作流被两个审批请求同时恢复。多实例生产环境还应在数据库或消息层增加分布式互斥。
- `RunnableConfig.addParallelNodeExecutor(PREPARE_REFUND_CHECKS, refundCheckExecutor)` 显式绑定了图内并行池；该池与 SSE 池隔离、队列有界，Spring 关闭时会执行 `shutdown`。

## 代码结构

```text
com.example.langgraph4j.aftersales
├── ai
│   ├── AfterSalesAssistant              # 业务节点依赖的模型端口
│   ├── SpringAiAfterSalesAssistant      # Spring AI ChatModel 适配器
│   └── RuleBasedAfterSalesAssistant     # 离线可重复实现
├── config
│   ├── AiModelConfiguration             # DeepSeek / Ollama / OpenAI
│   └── CheckpointConfiguration          # MemorySaver / MysqlSaver
├── service
│   ├── RefundPolicyService / RefundRiskService
│   ├── IdempotentRefundExecutionService
│   └── AfterSalesWorkflowService        # start / updateState / resume / history
├── workflow
│   ├── AfterSalesState                  # 状态字段和 Channel reducer
│   ├── AfterSalesWorkflow               # StateGraph 定义
│   └── WorkflowEvent                    # SSE 事件
└── web
    └── AfterSalesController             # REST / SSE / Mermaid
```

## 测试范围

- 缺订单号中断、补充信息恢复、人工审批再次恢复
- 政策/风险并行分支和 reducer 合并痕迹
- 低风险自动退款、高风险拒绝、人工拒绝
- 补充次数上限和模型异常进入可审计失败状态
- 幂等退款
- Fake `ChatModel` 验证 Spring AI 适配层，不依赖真实 API
- checkpoint 历史和 Mermaid 导出

同一个 threadId 只能 start 一次，重复启动返回冲突；补充信息和审批使用专用恢复接口，避免把新订单混入旧 checkpoint。IntentAnalysis 会补齐缺失 orderId 标记，即使模型漏报 missingFields，也会进入追问。当前回归覆盖 MemorySaver；本次环境没有可运行的 MySQL，未完成 MySQL 跨进程恢复实测。
