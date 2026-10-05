# 5.3 Spring AI Persistent Memory Demo

对应课程 `5.3｜Spring AI 持久化与长期记忆`。

```bash
mvn -f pom.xml exec:java
```

示例演示会话消息持久化、窗口裁剪和长期偏好提炼的边界。真实接入 Spring AI 时，对应的是 `ChatMemoryRepository` 和长期记忆召回链路。

## JDBC ChatMemoryRepository

真实项目接入 JDBC 版会话记忆时，看这几个文件：

| 文件 | 作用 |
| --- | --- |
| `src/main/java/com/example/springaipersistentmemory/config/JdbcChatMemoryConfiguration.java` | 使用 `JdbcChatMemoryRepository` 创建 `ChatMemoryRepository`，再交给 `MessageWindowChatMemory`。 |
| `src/main/resources/application.yml` | 配置 JDBC memory schema 初始化、默认 H2 数据源，以及 DeepSeek / Ollama / ChatGPT 的 provider 配置入口。 |
| `src/main/resources/db/spring-ai-chat-memory-h2.sql` | 本地演示默认脚本。 |
| `src/main/resources/db/spring-ai-chat-memory-postgresql.sql` | PostgreSQL 建表脚本。 |
| `src/main/resources/db/spring-ai-chat-memory-mysql.sql` | MySQL / MariaDB 建表脚本。 |

本地演示默认使用 H2：

```bash
mvn -f pom.xml test
```

切换 PostgreSQL 时，只要改数据源和 schema 路径：

```bash
export APP_DATASOURCE_URL=jdbc:postgresql://localhost:5432/ai_course
export APP_DATASOURCE_USERNAME=ai_course
export APP_DATASOURCE_PASSWORD=ai_course
export APP_DATASOURCE_DRIVER=org.postgresql.Driver
export SPRING_AI_CHAT_MEMORY_SCHEMA=classpath:db/spring-ai-chat-memory-postgresql.sql
export SPRING_AI_CHAT_MEMORY_PLATFORM=postgresql
```

生产环境通常把 `SPRING_AI_CHAT_MEMORY_INITIALIZE_SCHEMA` 设为 `never`，由 Flyway 或 Liquibase 执行同目录下的 SQL 脚本。
