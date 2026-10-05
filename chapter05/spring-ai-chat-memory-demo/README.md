# 5.2 Spring AI Chat Memory Demo

对应课程 `5.2｜Spring AI 会话记忆`。

```bash
mvn -f pom.xml exec:java
```

示例用 Advisor 风格的调用链演示 `conversationId` 隔离和短期消息窗口。真实接入 Spring AI 时，对应的是 `MessageChatMemoryAdvisor`、`MessageWindowChatMemory` 和 `ChatMemory.CONVERSATION_ID`。
