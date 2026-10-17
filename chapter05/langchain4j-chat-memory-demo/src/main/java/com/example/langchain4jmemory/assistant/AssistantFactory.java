package com.example.langchain4jmemory.assistant;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;

/** 真正创建 LangChain4j AI Service，不再使用自写注解或 lambda 模拟框架。 */
public class AssistantFactory {
    public Assistant create(ChatModel chatModel) {
        return AiServices.builder(Assistant.class).chatModel(chatModel)
                // 框架为新 ID 调用 Provider，并管理这个 AI Service 内的记忆实例。
                .chatMemoryProvider(id -> MessageWindowChatMemory.builder().id(id).maxMessages(10).build())
                .build();
    }
}
