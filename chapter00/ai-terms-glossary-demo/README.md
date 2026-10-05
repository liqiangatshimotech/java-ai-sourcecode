# ai-terms-glossary-demo

这个 demo 把术语表里的几个高频词落到 Java 对象上：模型路由、上下文打包、RAG 检索、工具注册、权限边界、评测门禁和可观测 Trace。

代码不会真实调用模型，默认配置按 DeepSeek 写好，同时保留 Ollama 和 ChatGPT 的配置位。这样可以先理解工程边界，再把 `MockChatClient` 换成真实 HTTP 调用或 Spring AI、LangChain4j 的客户端。

## 运行

```bash
mvn -f chapter00/ai-terms-glossary-demo/pom.xml compile exec:java
```

默认 provider 是 DeepSeek：

```bash
mvn -f chapter00/ai-terms-glossary-demo/pom.xml compile exec:java -Dai.provider=deepseek
```

也可以切到本地 Ollama 或 ChatGPT：

```bash
mvn -f chapter00/ai-terms-glossary-demo/pom.xml compile exec:java -Dai.provider=ollama
mvn -f chapter00/ai-terms-glossary-demo/pom.xml compile exec:java -Dai.provider=chatgpt
```

## 配置说明

| Provider | Base URL | Model |
|---|---|---|
| deepseek | `https://api.deepseek.com` | `deepseek-v4-flash` |
| ollama | `http://localhost:11434` | `qwen2.5:7b` |
| chatgpt | `https://api.openai.com/v1` | `gpt-4o-mini` |

如果要接真实模型，把 `ProviderConfig` 里的 baseUrl、model 和 apiKeyEnv 按业务环境调整，再实现一个真正的 `ChatClient`。
