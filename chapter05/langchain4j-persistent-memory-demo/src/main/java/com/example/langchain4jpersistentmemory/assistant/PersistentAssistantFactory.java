package com.example.langchain4jpersistentmemory.assistant;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;

/** 同一个 Store 可以被重建后的 AI Service 继续读取；框架负责裁剪后写回。 */
public class PersistentAssistantFactory {
    public Assistant create(ChatModel model,ChatMemoryStore store) {
        return builder(model,store).build();
    }
    public Assistant create(ChatModel model,ChatMemoryStore store,RetrievalAugmentor rag,boolean saveRetrieved) {
        return builder(model,store).retrievalAugmentor(rag)
                // 框架 1.15.0 默认 true；本例调用处显式选择 false。
                .storeRetrievedContentInChatMemory(saveRetrieved).build();
    }
    private AiServices<Assistant> builder(ChatModel model,ChatMemoryStore store) {
        return AiServices.builder(Assistant.class).chatModel(model)
                .chatMemoryProvider(id -> MessageWindowChatMemory.builder()
                        .id(id).maxMessages(20).chatMemoryStore(store).build());
    }
}
