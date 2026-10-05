# OpenAI Java SDK Demo

项目名称：`openai-java-sdk-demo`

这个项目对应 `13-1.9｜OpenAI Java SDK 接入与最佳实践`。代码默认走 DeepSeek 的 OpenAI-compatible 接口，也可以通过环境变量切换到 Ollama 或 ChatGPT。

## 运行

默认 DeepSeek：

```bash
export DEEPSEEK_API_KEY=sk-your-key
mvn -f chapter13-1/openai-java-sdk-demo/pom.xml exec:java
```

切换到 Ollama：

```bash
export AI_PROVIDER=ollama
export OLLAMA_BASE_URL=http://localhost:11434/v1
export OLLAMA_MODEL=qwen3
mvn -f chapter13-1/openai-java-sdk-demo/pom.xml exec:java
```

切换到 ChatGPT：

```bash
export AI_PROVIDER=chatgpt
export OPENAI_API_KEY=sk-your-key
export OPENAI_MODEL=gpt-5.5
mvn -f chapter13-1/openai-java-sdk-demo/pom.xml exec:java
```

## 测试

测试只验证配置解析，不会请求真实模型：

```bash
mvn -f chapter13-1/openai-java-sdk-demo/pom.xml test
```
