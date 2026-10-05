# Bailian DashScope Model Demo

项目名称：`bailian-dashscope-model-demo`

这个项目对应 `13-1.12｜阿里云百炼 DashScope 模型接入与最佳实践`。代码默认走 DeepSeek，也支持 Ollama、ChatGPT；需要验证阿里云百炼时，把 provider 切到 `bailian`。

## 运行

默认 DeepSeek：

```bash
export DEEPSEEK_API_KEY=sk-your-key
mvn -f chapter13-1/bailian-dashscope-model-demo/pom.xml exec:java
```

切到阿里云百炼：

```bash
export AI_PROVIDER=bailian
export DASHSCOPE_API_KEY=sk-your-key
export BAILIAN_MODEL=qwen-plus
mvn -f chapter13-1/bailian-dashscope-model-demo/pom.xml exec:java
```

使用 DashScope 原生接口时，AgentScope 默认会请求 `https://dashscope.aliyuncs.com`，一般不需要配置 Base URL。
只有接入代理网关或私有网络出口时，才需要显式配置：

```bash
export BAILIAN_BASE_URL=https://dashscope.aliyuncs.com
```

如果使用的是百炼 OpenAI 兼容接口，URL 形如
`https://your-workspace-id.cn-beijing.maas.aliyuncs.com/compatible-mode/v1`，那应该走
OpenAI-compatible 适配器，而不是本项目里的 DashScope 原生模型。

切到 Ollama：

```bash
export AI_PROVIDER=ollama
export OLLAMA_BASE_URL=http://localhost:11434
export OLLAMA_MODEL=qwen3
mvn -f chapter13-1/bailian-dashscope-model-demo/pom.xml exec:java
```

切到 ChatGPT：

```bash
export AI_PROVIDER=chatgpt
export OPENAI_API_KEY=sk-your-key
export OPENAI_MODEL=gpt-5.5
mvn -f chapter13-1/bailian-dashscope-model-demo/pom.xml exec:java
```

真实请求模型：

```bash
export RUN_MODEL_CALL=true
mvn -f chapter13-1/bailian-dashscope-model-demo/pom.xml exec:java
```

## 测试

测试只验证配置解析和模型工厂，不会请求真实模型：

```bash
mvn -f chapter13-1/bailian-dashscope-model-demo/pom.xml test
```
