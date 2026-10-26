# spring-boot-agent-runtime-demo

对应飞书 8.3–8.3.4。JDK 21+，Spring Boot 3.5.14。默认离线 RulePlanner，不访问模型。

在解压后的 `java-ai-course-examples` 目录运行：

```bash
mvn -f chapter08/spring-boot-agent-runtime-demo/pom.xml test
mvn -f chapter08/spring-boot-agent-runtime-demo/pom.xml spring-boot:run
curl -sS -X POST http://localhost:8080/api/agent/runs \
  -H 'Content-Type: application/json' \
  -d '{"userId":"demo-1","goal":"查询物流并申请补偿","orderNo":"A20260001","allowWrite":false}'
```

文章和 HTTP 使用相同字段：`userId`、`goal`、`orderNo`、`allowWrite`。`userId` 在 demo 中只是会话标识，生产从认证上下文读取。旧版 `sessionId`、`message` 是输入别名，但现在必须显式提供 `orderNo`；缺失订单不会自动处理 A100。

响应字段：`runId`、`userId`、`provider`、`status`、`finalAnswer`、`pendingApproval`、`steps`、`facts`。`pendingApproval` 使用可直接序列化的字符串，无待确认动作时为空。第一轮 `WAITING_APPROVAL`、5 条轨迹；`compensationDecision=ELIGIBLE`，`compensationAmount=20`。

```bash
curl -sS http://localhost:8080/api/agent/runs/<runId>/trace
curl -sS http://localhost:8080/api/agent/runs/<runId>/events
curl -N http://localhost:8080/api/agent/runs/<runId>/events/stream
curl -sS -X POST http://localhost:8080/api/agent/runs/<runId>/approve
```

确认接口仅执行冻结的原工具及参数；后续新写动作重新要求确认。确认后 `COMPLETED`，累计 6 条轨迹，`ticketNo=TICKET-A20260001`；查询动作不重跑。重复审批 HTTP 409。SSE 回放已有事件，等待确认时保持连接，终态回放后关闭；初始 POST 同步执行，拿到 runId 后的订阅先读历史，不是“POST 返回前已实时订阅”。

补偿规则与文章一致：`shippedHoursAgo > promiseHours && hoursSinceLastUpdate >= 12`。等于承诺时限不符合；物流停滞恰好 12 小时且已超承诺时限符合。A20260001/A100 满足条件，A200 不满足，未知订单 FAILED。工单创建前重新核实资格，创建工单不等于补偿已发放。

源码根目录 `src/main/java/com/example/agentruntime`：

| 目录 | 文章中的类 |
| --- | --- |
| domain | AgentRunRequest / AgentRunResponse / AgentState / AgentRunTrace / AgentStepRecord / AgentEvent |
| planner | AgentPlanner / RulePlanner / AgentDecision |
| tool | ToolRegistry / ToolCall / ToolObservation / ToolPermissionGuard / ToolAuthorization / ToolRisk |
| business | AfterSalesTools / OrderRepository / LogisticsGateway / DemoOrderData |
| repository | AgentTraceRepository / InMemoryAgentTraceRepository |
| event | AgentEventPublisher / InMemoryAgentEventPublisher |
| service / web | AgentRuntimeService / StopCondition / AgentController / ApiExceptionHandler |

`maxSteps` 统计工具尝试，包括 BLOCKED 和 FAILED。最终 Finish 不占工具额度；因此直接授权用 5 步完成，先拦截再确认需 6 步。默认上限 8。失败写入轨迹后停止，不提供自动工具重试或结果未知写操作的自动恢复。

模型连接配置按 `AI_PROVIDER` 自动选择默认地址和模型。DeepSeek 默认 `deepseek-flash` / `https://api.deepseek.com` / `DEEPSEEK_API_KEY`；Ollama 为 `deepseek-r1:7b` / `http://localhost:11434/v1`；ChatGPT 使用 `openai` 或 `chatgpt` / `gpt-4.1-mini` / `https://api.openai.com/v1` / `OPENAI_API_KEY`。`AI_BASE_URL`、`AI_MODEL`、`AI_API_KEY` 可覆盖。配置日志不输出密钥；本项目没有真实模型 Planner。

状态、轨迹、事件和工单幂等均只在当前 JVM 内有效，重启会丢失。本项目未实现生产鉴权、数据库恢复或跨实例锁。`allowWrite` 只用于本地测试，生产需改成服务端授权。测试覆盖原文流程、规则边界、冻结审批、后续写权限、异常与步数上限，并真实启动 HTTP/SSE 服务。
