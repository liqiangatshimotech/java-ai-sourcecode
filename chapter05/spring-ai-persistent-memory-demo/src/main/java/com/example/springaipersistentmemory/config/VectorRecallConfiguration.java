package com.example.springaipersistentmemory.config;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
/** 聊天模型和 embedding 是两种能力；不能把 DeepSeek 聊天接口当 embedding 接口。 */
@Configuration
public class VectorRecallConfiguration {
    @Bean @ConditionalOnProperty(name="memory.vector.enabled",havingValue="true")
    public EmbeddingModel preferenceEmbeddingModel(Environment env) {
        // 只在显式开启向量召回时需要 Ollama embedding 服务；默认 SQL 精确读取结构化偏好。
        return OllamaEmbeddingModel.builder().ollamaApi(OllamaApi.builder()
                .baseUrl(env.getProperty("EMBEDDING_BASE_URL","http://localhost:11434")).build())
                .defaultOptions(OllamaEmbeddingOptions.builder()
                        .model(env.getProperty("EMBEDDING_MODEL","nomic-embed-text")).build()).build();
    }
}
