# langchain4j-mcp-client-demo

第 6.4.2 节示例工程：使用 LangChain4j MCP Client 连接 MCP Server，并通过 `McpToolProvider` 把远程工具接入 AI Service。

默认使用 DeepSeek，可通过环境变量切换：

```bash
export APP_AI_DEFAULT_PROVIDER=deepseek
export DEEPSEEK_API_KEY=...

# 或使用本地 Ollama
export APP_AI_DEFAULT_PROVIDER=ollama
export OLLAMA_BASE_URL=http://localhost:11434
export OLLAMA_MODEL=qwen3.5:9b

# 或使用 ChatGPT
export APP_AI_DEFAULT_PROVIDER=chatgpt
export CHATGPT_API_KEY=...
```

先打包服务端，再运行客户端：

```bash
mvn -f chapter06/langchain4j-mcp-server-demo/pom.xml package
mvn -f chapter06/langchain4j-mcp-client-demo/pom.xml exec:java
```
