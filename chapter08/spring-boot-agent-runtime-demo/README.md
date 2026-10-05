# spring-boot-agent-runtime-demo

这个项目对应 8.3.2、8.3.3、8.3.4。它把订单售后 Agent 的状态、轨迹、事件、工具权限和人工确认做成一个可运行的 Spring Boot 服务。默认规划器和业务数据都在本地，不需要模型密钥即可验证完整流程。

从 `java-ai-course-examples` 目录运行：

```bash
mvn -f chapter08/spring-boot-agent-runtime-demo/pom.xml test
mvn -f chapter08/spring-boot-agent-runtime-demo/pom.xml spring-boot:run
```

提交订单 A100 的售后请求：

```bash
curl -sS -X POST http://localhost:8080/api/agent/runs \
  -H 'Content-Type: application/json' \
  -d '{"sessionId":"demo-1","message":"查询订单 A100 的物流并申请补偿","allowWrite":false}'
```

返回的 `status` 为 `WAITING_APPROVAL`，五步轨迹中最后一步是 `BLOCKED`。从返回值中取 `runId`，继续查询与确认：

```bash
curl -sS http://localhost:8080/api/agent/runs/<runId>/trace
curl -sS http://localhost:8080/api/agent/runs/<runId>/events
curl -N http://localhost:8080/api/agent/runs/<runId>/events/stream
curl -sS -X POST http://localhost:8080/api/agent/runs/<runId>/approve
```

确认后会创建工单并返回 `COMPLETED`。`allowWrite:true` 可以在首次请求中直接授权写操作，适合自动化测试；实际业务中应由已认证的人工处理入口控制确认接口，不应把客户端布尔值当作生产权限凭证。内存中的运行状态、轨迹和事件在服务重启后会消失，生产环境需要换成持久化实现。

配置文件提供 DeepSeek、Ollama 和 ChatGPT 的统一连接字段：`AI_PROVIDER`、`AI_BASE_URL`、`AI_MODEL`、`AI_API_KEY`。默认值是 DeepSeek，密钥从 `DEEPSEEK_API_KEY` 读取；Ollama 可以设为 `AI_PROVIDER=ollama AI_BASE_URL=http://localhost:11434/v1 AI_MODEL=deepseek-r1:7b`；ChatGPT 可以设为 `AI_PROVIDER=openai AI_BASE_URL=https://api.openai.com/v1 AI_MODEL=gpt-4.1-mini` 并配置 `OPENAI_API_KEY`。当前项目使用确定性 Planner，不会发起模型请求；这些字段为替换模型 Planner 预留。
