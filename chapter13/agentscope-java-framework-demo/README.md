# AgentScope Java Framework Demo

本项目对应课程 `13｜AgentScope Java 框架实战`。

项目名称：`agentscope-java-framework-demo`

它演示 AgentScope Java 的几个关键能力：

- `ModelConfigurationKnowledgeDemo`：模型配置，默认 DeepSeek，兼容 Ollama 和 ChatGPT。
- `ReActToolKnowledgeDemo`：ReAct Agent 里的 Java 工具注册方式。
- `StructuredOutputKnowledgeDemo`：结构化输出对象 `TicketDecision`。
- `MemorySessionStateKnowledgeDemo`：记忆、会话和状态快照的工程形态。
- `RagMcpKnowledgeDemo`：RAG 和 MCP 的适配边界。
- `MultiAgentKnowledgeDemo`：多智能体协作的 Supervisor / Specialist 拆分方式。
- `HookPlanStudioKnowledgeDemo`：Hook、Plan 和 Studio 背后的可观测事件流。
- `ReActAgent`：用 ReAct 范式组织智能体推理和工具调用。
- `OpenAIChatModel` / `OllamaChatModel`：默认 DeepSeek，兼容 ChatGPT 和本地 Ollama。
- `Toolkit` / `@Tool`：把 Java 业务方法注册成 Agent 可调用工具。
- 结构化输出：把模型结果映射成 Java POJO，便于后端校验和入库。
- Hook 设计思路：在 Agent 执行过程中做日志、审计和人工干预。
- 项目实战：客服工单分派、知识检索、升级建议和处理摘要。

## 运行方式

默认 provider 是 `deepseek`：

```bash
export DEEPSEEK_API_KEY=sk-your-key
mvn -f chapter13/agentscope-java-framework-demo/pom.xml exec:java
```

切换到 ChatGPT：

```bash
export AI_PROVIDER=chatgpt
export OPENAI_API_KEY=sk-your-key
export OPENAI_MODEL=gpt-4o-mini
mvn -f chapter13/agentscope-java-framework-demo/pom.xml exec:java
```

切换到 Ollama：

```bash
export AI_PROVIDER=ollama
export OLLAMA_BASE_URL=http://localhost:11434
export OLLAMA_MODEL=qwen3
mvn -f chapter13/agentscope-java-framework-demo/pom.xml exec:java
```

## 只运行项目实战 Demo

```bash
mvn -f chapter13/agentscope-java-framework-demo/pom.xml exec:java \
  -Dexec.mainClass=com.example.agentscopeframework.demo.ServiceTicketProjectDemo
```

## 不调用模型的验证

测试只验证配置、工具和项目编排，不会真实请求大模型：

```bash
mvn -f chapter13/agentscope-java-framework-demo/pom.xml test
```
