# a2a-protocol-demo

一个最小 Java A2A 协议示例。

- `GET /.well-known/agent-card.json`：返回 Agent Card。
- `POST /message:send`：提交用户消息并创建 Task。
- `GET /tasks/{taskId}`：查询任务状态和 Artifact。

示例不真实调用大模型，协议层可以先离线跑通。项目保留了 DeepSeek、Ollama 和 ChatGPT 的配置入口，后续可以把 `CustomerSupportAgentExecutor` 换成真实模型或 AgentScope Runtime。

```bash
cd java-ai-course-examples
mvn -f chapter13-2/a2a-protocol-demo/pom.xml test
```

```bash
cd java-ai-course-examples
mvn -f chapter13-2/a2a-protocol-demo/pom.xml exec:java
```
