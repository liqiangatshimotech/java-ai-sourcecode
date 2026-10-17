package com.example.langchain4jpersistentmemory.assistant;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;
/** 按会话路由持久化窗口；调用方必须先完成用户授权。 */
public interface Assistant {
    String chat(@MemoryId String memoryId, @UserMessage String message);
}
