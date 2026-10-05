# java-mcp-repo-radar-demo

第 6.6 节示例项目，用 Java 查询几个值得关注的 Java MCP 相关 GitHub 仓库，并输出一个简短的技术雷达。

默认只访问 GitHub API，不调用大模型。如果希望让模型生成摘要，可以设置 `APP_AI_SUMMARY=true`。

## 运行

```bash
mvn -f chapter06/java-mcp-repo-radar-demo/pom.xml exec:java
```

GitHub API 匿名调用有频率限制。需要更稳定时设置：

```bash
export GITHUB_TOKEN=ghp_xxx
```

## 模型配置

默认模型提供方是 DeepSeek：

```bash
export APP_AI_SUMMARY=true
export APP_AI_DEFAULT_PROVIDER=deepseek
export DEEPSEEK_API_KEY=sk-xxx
mvn -f chapter06/java-mcp-repo-radar-demo/pom.xml exec:java
```

也可以切换到 Ollama：

```bash
export APP_AI_SUMMARY=true
export APP_AI_DEFAULT_PROVIDER=ollama
export OLLAMA_BASE_URL=http://localhost:11434
export OLLAMA_MODEL=qwen3.5:9b
mvn -f chapter06/java-mcp-repo-radar-demo/pom.xml exec:java
```

或 ChatGPT：

```bash
export APP_AI_SUMMARY=true
export APP_AI_DEFAULT_PROVIDER=chatgpt
export CHATGPT_API_KEY=sk-xxx
export CHATGPT_MODEL=gpt-4o-mini
mvn -f chapter06/java-mcp-repo-radar-demo/pom.xml exec:java
```
