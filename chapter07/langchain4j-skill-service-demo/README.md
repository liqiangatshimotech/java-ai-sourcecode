# langchain4j-skill-service-demo

我使用 LangChain4j 1.20.0 的真实 `AiServices`、`@MemoryId`，以及 `langchain4j-skills:1.20.0-beta30` 的 `Skill` / `Skills`。模型调用原生 `activate_skill`，框架返回 Skill 正文后再继续回答。beta 模块升级前须重跑测试。

先设置 `DEEPSEEK_API_KEY`，再运行 `APP_AI_MODE=live APP_AI_DEFAULT_PROVIDER=deepseek mvn -q -f chapter07/langchain4j-skill-service-demo/pom.xml compile exec:java`。

| provider | 模型变量及默认值 | 地址变量及默认值 | 密钥变量 |
|---|---|---|---|
| deepseek | DEEPSEEK_MODEL=deepseek-v4-flash | DEEPSEEK_BASE_URL=https://api.deepseek.com/v1 | DEEPSEEK_API_KEY |
| ollama | OLLAMA_MODEL=qwen2.5:7b | OLLAMA_BASE_URL=http://localhost:11434/v1 | 无需密钥 |
| chatgpt / openai | OPENAI_MODEL=gpt-4.1-mini | OPENAI_BASE_URL=https://api.openai.com/v1 | OPENAI_API_KEY |

通过 APP_AI_DEFAULT_PROVIDER 切换，LangChain4j 使用含 `/v1` 的兼容接口。Ollama 模型须支持工具调用，地址兼容不等于模型能力兼容。401 检查对应密钥，404 检查地址和模型名；不要打印密钥排错。

没有启用 Shell Skills。Skill 不授予权限；生产端先认证，再构造 `tenant:user:conversation` 的 MemoryId。记忆只在当前进程存活，同会话并发应在业务层串行化。测试验证原生工具结果进入下一轮模型、不同会话隔离与同会话历史保留。

环境要求：JDK 21+、Maven 3.9+。解压后进入 java-ai-course-examples，使用项目 POM 单独运行，不要求下载其他章节。

```bash
mvn -q -f chapter07/langchain4j-skill-service-demo/pom.xml test
mvn -q -f chapter07/langchain4j-skill-service-demo/pom.xml compile exec:java
```

默认 APP_AI_MODE 未设置时为 offline，仅最后一跳模型使用本地替身，框架调用链真实执行。offline 输出是模拟回答，不代表真实模型效果。APP_AI_MODE=live 才访问模型服务，密钥仅从环境变量读取。

本轮验收仅运行离线测试和启动入口，未发起付费真实模型调用。上线前需在所选服务执行 live 验收。
