# minimal-agent-loop-demo

这个项目不使用 Agent 框架，直接实现最小的“模型决策 → 工具执行 → 观察回填 → 再次决策”循环。内置最大步数、工具白名单和异常终止，便于看清 Agent 为什么既不是一次模型调用，也不是无限循环。

```bash
mvn -f chapter08/minimal-agent-loop-demo/pom.xml test
mvn -f chapter08/minimal-agent-loop-demo/pom.xml exec:java
```

默认模型配置为 DeepSeek；环境变量与 8.1 相同，支持 `AI_PROVIDER=ollama` 和 `AI_PROVIDER=openai`。离线演示用脚本模型模拟结构化工具调用，不读取真实密钥。
