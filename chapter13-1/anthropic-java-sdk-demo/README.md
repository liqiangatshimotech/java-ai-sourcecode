# Anthropic Java SDK Demo

项目名称：`anthropic-java-sdk-demo`

这个项目对应 `13-1.11｜Anthropic Java SDK 接入与最佳实践`。业务层只依赖 `ChatClient`，默认 DeepSeek，也支持 Ollama、ChatGPT；需要验证 Anthropic Java SDK 时，把 provider 切到 `anthropic`。

## 运行

默认 DeepSeek：

```bash
export DEEPSEEK_API_KEY=sk-your-key
mvn -f chapter13-1/anthropic-java-sdk-demo/pom.xml exec:java
```

切换到 Anthropic：

```bash
export AI_PROVIDER=anthropic
export ANTHROPIC_API_KEY=sk-ant-your-key
export ANTHROPIC_MODEL=claude-opus-4-8
mvn -f chapter13-1/anthropic-java-sdk-demo/pom.xml exec:java
```

切换到 Ollama：

```bash
export AI_PROVIDER=ollama
export OLLAMA_BASE_URL=http://localhost:11434/v1
export OLLAMA_MODEL=qwen3
mvn -f chapter13-1/anthropic-java-sdk-demo/pom.xml exec:java
```

切换到 ChatGPT：

```bash
export AI_PROVIDER=chatgpt
export OPENAI_API_KEY=sk-your-key
export OPENAI_MODEL=gpt-5.5
mvn -f chapter13-1/anthropic-java-sdk-demo/pom.xml exec:java
```

## 测试

测试只验证配置解析和 OpenAI-compatible 请求体构造，不会请求真实模型：

```bash
mvn -f chapter13-1/anthropic-java-sdk-demo/pom.xml test
```
