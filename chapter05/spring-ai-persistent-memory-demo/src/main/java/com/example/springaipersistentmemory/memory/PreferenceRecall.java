package com.example.springaipersistentmemory.memory;
import java.util.*;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.*;
import org.springframework.stereotype.Service;
/** SQL 是持久事实源；可选向量索引只包含当前用户未过期的偏好。 */
@Service
public class PreferenceRecall {
    private final PreferenceMemoryStore store;
    private final Optional<EmbeddingModel> embedding;
    public PreferenceRecall(PreferenceMemoryStore store,Optional<EmbeddingModel> embedding) {
        this.store=store;this.embedding=embedding;
    }
    public List<String> recall(String userId,String question) {
        var memories=store.findActive(userId);
        if(embedding.isEmpty()||memories.isEmpty())
            return memories.stream().map(UserPreferenceMemory::content).toList();
        // 先按用户和有效期取候选，再建立临时索引，避免其他用户数据进入此次召回。
        // 小样本演示重建索引，不代表生产建议每次重算 embedding。
        var vector=SimpleVectorStore.builder(embedding.get()).build();
        vector.add(memories.stream().map(m->new Document(m.content(),Map.of("userId",m.userId(),"type",m.type()))).toList());
        return vector.similaritySearch(SearchRequest.builder().query(question).topK(5).similarityThreshold(0.0).build())
                .stream().map(Document::getText).toList();
    }
}
