# langchain4j-agentic-workflow-demo

默认入口 `OfficialAgenticWorkflowDemo` 使用 `langchain4j-agentic:1.20.0-beta30` 与 LangChain4j 1.20.0。官方 `@Agent` / `@V` 绑定变量；`parallelBuilder` 查订单和物流，`sequenceBuilder` 等待事实，`loopBuilder` 生成与检查回复。Scope 与调用记录由框架生成。

先设置 `DEEPSEEK_API_KEY`，再运行 `APP_AI_MODE=live AI_PROVIDER=deepseek mvn -q -f chapter08/langchain4j-agentic-workflow-demo/pom.xml compile exec:java`。

`AI_PROVIDER` 默认 deepseek，也可取 ollama 或 openai/chatgpt；`AI_BASE_URL`、`AI_MODEL` 可覆盖地址与模型。DeepSeek 默认 https://api.deepseek.com、deepseek-flash；Ollama 默认 http://localhost:11434/v1、deepseek-r1:7b；OpenAI 默认 https://api.openai.com/v1、gpt-4.1-mini，密钥为 OPENAI_API_KEY。Ollama 不需要云端密钥。真实模型调用设 45 秒超时且不自动重试。

演示数据仅包含 A100，两项事实查询是本地 Java 仓库，模型负责回复生成。离线模型第一次故意漏写人工确认，第二次修正；真实模型可能一次通过，也可能触发上限。检查未通过时抛异常转人工，循环停止不代表质量过关。关键词规则只是教学最小门禁，不能代替补偿审批或语义安全评测；没有支付、工单创建等外部写操作，也没有跨进程恢复。

测试包含 Scope 变量交接、两轮修订、上限失败关闭、未知订单在模型调用前拒绝。`AgenticWorkflowDemo` 保留纯 Java 对照，可用 `-Dexec.mainClass=com.example.agenticworkflow.AgenticWorkflowDemo` 运行；其中的自定义 Scope 仅属于对照实现。

环境要求：JDK 21+、Maven 3.9+。解压后进入 java-ai-course-examples，使用项目 POM 单独运行，不要求下载其他章节。

```bash
mvn -q -f chapter08/langchain4j-agentic-workflow-demo/pom.xml test
mvn -q -f chapter08/langchain4j-agentic-workflow-demo/pom.xml compile exec:java
```

默认 APP_AI_MODE 未设置时为 offline，仅最后一跳模型使用本地替身，框架调用链真实执行。offline 输出是模拟回答，不代表真实模型效果。APP_AI_MODE=live 才访问模型服务，密钥仅从环境变量读取。

本轮验收仅运行离线测试和启动入口，未发起付费真实模型调用。上线前需在所选服务执行 live 验收。
