# 5.3｜Spring AI 持久化与长期记忆

项目名称：`spring-ai-persistent-memory-demo`。使用 JDK 21、Maven 3.9+、Spring Boot 3.5.14、Spring AI 1.1.7。

我把当前窗口交给真实 JdbcChatMemoryRepository，把跨会话的偏好放进独立 SQL 表。主程序启动 Spring Boot HTTP 服务，不再运行文件模拟类。

在解压后的 java-ai-course-examples 根目录：

- 测试：`mvn -f chapter05/spring-ai-persistent-memory-demo/pom.xml clean test`
- 设置模型密钥后启动：`mvn -f chapter05/spring-ai-persistent-memory-demo/pom.xml spring-boot:run`
- 另一终端验证：`java chapter05/spring-ai-persistent-memory-demo/demo/PersistentMemorySmokeClient.java`

默认只监听 127.0.0.1:8053；用 SERVER_PORT 改端口后，把对应 URL 作为 Java 客户端第一个参数。接口 POST /api/persistent-memory-chat 接收 userId、conversationId、message。它是本机无登录示例；这些字段不能当成可信身份，带授权的版本见 5.6。

## 文件与数据库

- config/JdbcChatMemoryConfiguration：具体类型 JdbcChatMemoryRepository Bean、10 条窗口及 Advisor。声明具体返回类型可避免自动配置重复创建 Repository。
- config/ModelConfiguration：三个聊天供应商。
- memory/UserPreferenceMemory、PreferenceExtractor、PreferenceMemoryStore：有来源和 TTL 的结构化偏好。
- memory/PreferenceRecall、config/VectorRecallConfiguration：SQL 读取以及可选向量召回。
- web/PersistentMemoryController：串行处理请求，生成 36 字符会话 key，避免直接拼接的歧义。
- src/main/resources/application.yml 和 db/：文件型 H2 与版本匹配的 SQL。

默认 APP_DATASOURCE_URL 为 `jdbc:h2:file:./data/spring-ai-memory;MODE=PostgreSQL`。相对路径以启动目录为准；重启应使用同一路径。jdbc:h2:mem 是内存库，不能用于证明重启恢复。

切换 PostgreSQL/MySQL 时一起设置 APP_DATASOURCE_URL、APP_DATASOURCE_USERNAME、APP_DATASOURCE_PASSWORD、APP_DATASOURCE_DRIVER、SPRING_AI_CHAT_MEMORY_SCHEMA。对应驱动与 schema 已包含；这次自动测试仅运行 H2，没有宣称验证外部数据库。生产环境应关闭自动建表并将偏好表和会话表交给迁移工具。

1.1.7 JDBC 表中的 timestamp 用作排序序号，不是精确事件时间。schema 必须跟框架版本匹配；升级 2.0 时需单独做迁移，不能盲用这里的脚本。会话 ID 用 UUID 格式映射到 VARCHAR(36)，不会把用户原始长 ID 直接写入该列。

## 长期偏好与可选向量召回

只提炼明示的固定偏好。“以后回答短一点”只保存长度偏好，不额外推断中文；其他未匹配输入不自动保存。偏好按用户隔离、默认 30 天有效；订单状态与权限不进这个表。

默认直接从 SQL 读取当前用户的有效偏好。设置 MEMORY_VECTOR_ENABLED=true 可启用真实 SimpleVectorStore；embedding 使用 Ollama，EMBEDDING_BASE_URL 默认 http://localhost:11434，EMBEDDING_MODEL 默认 nomic-embed-text。需先拉取 embedding 模型。DeepSeek 聊天模型不承担 embedding。

SQL 是持久事实源。小样本示例每次先按用户及有效期筛选，再重建临时向量索引；生产服务应使用可维护的索引和明确的授权过滤。Embedding 测试替换远端向量模型，不代表在线 embedding 已验证。

6 项测试覆盖真实 HTTP/JDBC/Advisor、窗口隔离与裁剪、独立 JVM 恢复、TTL 和向量召回候选隔离。当前应用将整次请求串行化，未实现多实例并发协调。

真实模型默认 `APP_AI_DEFAULT_PROVIDER=deepseek`，另支持 `ollama` 和 `chatgpt`。供应商在启动时选择。

| 供应商 | 环境变量与默认值 |
| --- | --- |
| DeepSeek | DEEPSEEK_API_KEY 必填；DEEPSEEK_BASE_URL=https://api.deepseek.com；DEEPSEEK_MODEL=deepseek-v4-flash |
| Ollama | OLLAMA_BASE_URL=http://localhost:11434；OLLAMA_MODEL=qwen2.5:7b；需要先启动服务并拉取模型 |
| ChatGPT | CHATGPT_API_KEY 必填；CHATGPT_BASE_URL=https://api.openai.com；CHATGPT_MODEL=gpt-4.1-mini |

密钥只放环境变量或 IDE 运行配置。模型名可以改成账号或本机已安装的模型。测试不调用真实账号，不代表模型、额度或网络已在线验证。

