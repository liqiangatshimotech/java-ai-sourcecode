# agent-concepts-demo

这个项目用“查询订单并判断是否允许退款”说明 Agent 的五个核心构件：目标、模型、工具、记忆和护栏。代码不访问网络，适合先观察一次决策是怎样产生的。

```bash
mvn -f chapter08/agent-concepts-demo/pom.xml test
mvn -f chapter08/agent-concepts-demo/pom.xml exec:java
```

真实模型配置统一使用环境变量，默认提供方是 DeepSeek：`AI_PROVIDER=deepseek`、`DEEPSEEK_API_KEY`；切换 Ollama 使用 `AI_PROVIDER=ollama`，切换 ChatGPT 使用 `AI_PROVIDER=openai` 和 `OPENAI_API_KEY`。演示入口只打印配置，不发送请求，也不会把密钥写入日志。
