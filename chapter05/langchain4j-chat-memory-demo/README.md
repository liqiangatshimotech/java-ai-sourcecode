# 5.4｜LangChain4j 会话记忆

项目名称：`langchain4j-chat-memory-demo`。使用 JDK 21、Maven 3.9+、LangChain4j 1.15.0。

我用真实 AI Services、@MemoryId、MessageWindowChatMemory 和 TokenWindowChatMemory 验证多会话隔离，不再用自写同名类模拟框架。

在解压后的 java-ai-course-examples 根目录：

- 真实多轮调用：`mvn -f chapter05/langchain4j-chat-memory-demo/pom.xml compile exec:java`
- 离线窗口比较：`mvn -f chapter05/langchain4j-chat-memory-demo/pom.xml compile exec:java -Dexec.args=--windows`
- 自动测试：`mvn -f chapter05/langchain4j-chat-memory-demo/pom.xml clean test`

模型调用前设置对应密钥。预期 user-a 回答华东区、user-b 回答华南区；措辞由模型决定。离线比较使用真实 GPT tokenizer，不调用云端；估算值不代表 DeepSeek/Ollama 的精确计费。

文章中的 Assistant、AssistantFactory 在 com.example.langchain4jmemory.assistant；MemoryIsolationDemo、WindowComparisonDemo 在 demo；ModelFactory 在 config。旧入口 com.example.langchain4jchatmemory.LangChain4jChatMemoryDemo 保留为启动入口。

AI Service 管理 ID 对应的记忆实例；Provider 不需要每轮新建无存储的空窗口。窗口默认在内存里，重启丢失；同一 ID 的请求应串行。ID 不是身份认证，7 项测试验证隔离、裁剪、token 预算及三种供应商请求。

真实模型默认 `APP_AI_DEFAULT_PROVIDER=deepseek`，另支持 `ollama` 和 `chatgpt`。供应商在启动时选择。

| 供应商 | 环境变量与默认值 |
| --- | --- |
| DeepSeek | DEEPSEEK_API_KEY 必填；DEEPSEEK_BASE_URL=https://api.deepseek.com；DEEPSEEK_MODEL=deepseek-v4-flash |
| Ollama | OLLAMA_BASE_URL=http://localhost:11434；OLLAMA_MODEL=qwen2.5:7b；需要先启动服务并拉取模型 |
| ChatGPT | CHATGPT_API_KEY 必填；CHATGPT_BASE_URL=https://api.openai.com；CHATGPT_MODEL=gpt-4.1-mini |

密钥只放环境变量或 IDE 运行配置。模型名可以改成账号或本机已安装的模型。测试不调用真实账号，不代表模型、额度或网络已在线验证。

三个供应商在这个项目中均使用 OpenAI 兼容 /v1/chat/completions；Ollama 与 5.2 的原生适配器不同。Base URL 带或不带末尾 /v1 均可。
