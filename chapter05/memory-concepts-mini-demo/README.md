# 5.1｜先分清 History、Memory 和业务状态

项目名称：`memory-concepts-mini-demo`。项目路径：`java-ai-course-examples/chapter05/memory-concepts-mini-demo`。

我用三个独立对象表示原始对话、选给模型的上下文和业务状态快照。Memory 不一定是摘要，也可以是原始消息窗口；本例的两条 MemoryEntry 是人工选取的样本，不是自动提炼结果。

使用 JDK 21、Maven 3.9+。在解压后的 `java-ai-course-examples` 根目录运行：

`mvn -f chapter05/memory-concepts-mini-demo/pom.xml compile exec:java`

会输出 History 3 条、Memory 2 条及业务状态。首次运行必须 compile。包内保留根 POM、chapter05 POM 和项目目录。

src/main/java/com/example/memoryconcepts 下的 MemoryConceptDemo、ChatTurn、MemoryEntry、SupportSessionState、ModelSettings 与文章逐一对应。本例不调用模型；ModelSettings 只展示配置，不能把打印了 provider 当成已经连上模型。用户自述的地区不是权限证明。

真实模型默认 `APP_AI_DEFAULT_PROVIDER=deepseek`，另支持 `ollama` 和 `chatgpt`。供应商在启动时选择。

| 供应商 | 环境变量与默认值 |
| --- | --- |
| DeepSeek | DEEPSEEK_API_KEY 必填；DEEPSEEK_BASE_URL=https://api.deepseek.com；DEEPSEEK_MODEL=deepseek-v4-flash |
| Ollama | OLLAMA_BASE_URL=http://localhost:11434；OLLAMA_MODEL=qwen2.5:7b；需要先启动服务并拉取模型 |
| ChatGPT | CHATGPT_API_KEY 必填；CHATGPT_BASE_URL=https://api.openai.com；CHATGPT_MODEL=gpt-4.1-mini |

密钥只放环境变量或 IDE 运行配置。模型名可以改成账号或本机已安装的模型。测试不调用真实账号，不代表模型、额度或网络已在线验证。

