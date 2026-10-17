# 5.5｜LangChain4j 持久化记忆和 RAG 边界

项目名称：`langchain4j-persistent-memory-demo`。使用 JDK 21、Maven 3.9+、LangChain4j 1.15.0、文件型 H2。

本项目实际使用 JDBC、框架 ChatMemoryStore、AI Services 和 DefaultRetrievalAugmentor。检索器返回本地退款政策样本，模型调用默认 DeepSeek。

在解压后的 java-ai-course-examples 根目录运行：

- 测试：`mvn -f chapter05/langchain4j-persistent-memory-demo/pom.xml clean test`
- 设置模型密钥后运行：`mvn -f chapter05/langchain4j-persistent-memory-demo/pom.xml compile exec:java`

数据库默认 `jdbc:h2:file:./data/langchain4j-memory`，可用 APP_MEMORY_DB_URL 改路径。路径相对启动工作目录；再次启动时保持同一路径。默认 H2 用户 sa、空密码只适合本机示例，不能当作生产部署配置。

## 文件对照

所有类位于 src/main/java/com/example/langchain4jpersistentmemory：

- store/JdbcChatMemoryStore：消息 JSON 序列化及事务性窗口替换。
- assistant/Assistant、PersistentAssistantFactory：真实框架接口与持久窗口装配。
- rag/RagMemoryBoundary：显式切换是否保存增强后的用户消息。
- config/ModelFactory：三个供应商的 OpenAI 兼容协议配置。
- LangChain4jPersistentMemoryDemo：启动时读取已有窗口，再执行一次退款问答。
- src/main/resources/db/schema-h2.sql：演示用 H2 表结构。其他数据库需另做迁移和验证。

## 关键边界

LangChain4j 1.15.0 默认 storeRetrievedContentInChatMemory=true；本例显式设为 false。false 保存原始 UserMessage，模型仍收到检索增强内容。true 保存增强后的 UserMessage，不是另存一条 SystemMessage。AI 回复始终可能复述政策，因此 false 不保证后续上下文完全没有知识内容。

delete 和 insert 在同一个事务中；插入失败会回滚删除。同一 memoryId 的整轮模型调用仍需串行，数据库事务不自动解决两个调用读旧窗口后相互覆盖的问题。窗口最多 20 条，被裁剪的消息会从 Store 消失；它不是完整 History。

memoryId 只接受不超过 128 字符的非空字符串。示例不含登录授权；业务应用应由可信身份生成 ID。测试包含独立 JVM 读取、回滚、AI Service 重建、窗口裁剪、RAG true/false/default 及清空隔离，共 7 项。

真实模型默认 `APP_AI_DEFAULT_PROVIDER=deepseek`，另支持 `ollama` 和 `chatgpt`。供应商在启动时选择。

| 供应商 | 环境变量与默认值 |
| --- | --- |
| DeepSeek | DEEPSEEK_API_KEY 必填；DEEPSEEK_BASE_URL=https://api.deepseek.com；DEEPSEEK_MODEL=deepseek-v4-flash |
| Ollama | OLLAMA_BASE_URL=http://localhost:11434；OLLAMA_MODEL=qwen2.5:7b；需要先启动服务并拉取模型 |
| ChatGPT | CHATGPT_API_KEY 必填；CHATGPT_BASE_URL=https://api.openai.com；CHATGPT_MODEL=gpt-4.1-mini |

密钥只放环境变量或 IDE 运行配置。模型名可以改成账号或本机已安装的模型。测试不调用真实账号，不代表模型、额度或网络已在线验证。

Ollama 在此使用 OpenAI 兼容 /v1/chat/completions。
