package com.example.langchain4jmemory.assistant;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;
/** 接口公开，才能从 demo 包调用；memoryId 是路由键，不能替代身份认证。 */
public interface Assistant {
    String chat(@MemoryId String memoryId, @UserMessage String message);
}
