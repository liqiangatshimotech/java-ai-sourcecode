package com.example.langchain4jpersistentmemory.rag;
import com.example.langchain4jpersistentmemory.assistant.*;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import java.util.List;

/** 实际调用 LangChain4j RAG；检索器用本地政策样本，重点验证增强消息与存储消息的区别。 */
public final class RagMemoryBoundary {
    public static Assistant create(ChatModel model,ChatMemoryStore store,boolean saveRetrieved) {
        var rag=DefaultRetrievalAugmentor.builder().contentRetriever(query ->
                query.text().contains("退款")?List.of(Content.from("退款政策样本：原路退回，预计 3 个工作日。")):List.of()).build();
        return new PersistentAssistantFactory().create(model,store,rag,saveRetrieved);
    }
}
