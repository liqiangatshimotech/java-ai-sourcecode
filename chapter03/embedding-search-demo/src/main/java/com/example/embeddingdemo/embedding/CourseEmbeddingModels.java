package com.example.embeddingdemo.embedding;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;

import java.time.Duration;
import java.util.Locale;
import java.util.Map;

/**
 * Embedding 模型工厂。
 *
 * <p>同一套检索代码只依赖 LangChain4j 的 EmbeddingModel 接口。
 * 本地演示、OpenAI、Ollama 都只是不同实现，业务检索逻辑不用改。
 */
public final class CourseEmbeddingModels {

    private CourseEmbeddingModels() {
    }

    public static EmbeddingModel fromEnvironment() {
        return from(System.getenv());
    }

    static EmbeddingModel from(Map<String, String> env) {
        String provider = env.getOrDefault("EMBEDDING_PROVIDER", "local")
            .trim()
            .toLowerCase(Locale.ROOT);

        return switch (provider) {
            case "local" -> new LocalConceptEmbeddingModel();
            case "openai", "chatgpt" -> openAi(env);
            case "ollama" -> ollama(env);
            default -> throw new IllegalArgumentException("不支持的 Embedding provider：" + provider);
        };
    }

    private static EmbeddingModel openAi(Map<String, String> env) {
        OpenAiEmbeddingModel.OpenAiEmbeddingModelBuilder builder = OpenAiEmbeddingModel.builder()
            .apiKey(required(env, "OPENAI_API_KEY"))
            .baseUrl(env.getOrDefault("OPENAI_BASE_URL", "https://api.openai.com/v1"))
            .modelName(env.getOrDefault("OPENAI_EMBEDDING_MODEL", "text-embedding-3-small"))
            .encodingFormat("float")
            .timeout(Duration.ofSeconds(30))
            .maxRetries(2);

        parseOptionalInteger(env.get("OPENAI_EMBEDDING_DIMENSIONS"))
            .ifPresent(builder::dimensions);

        return builder.build();
    }

    private static EmbeddingModel ollama(Map<String, String> env) {
        OllamaEmbeddingModel.OllamaEmbeddingModelBuilder builder = OllamaEmbeddingModel.builder()
            .baseUrl(env.getOrDefault("OLLAMA_BASE_URL", "http://localhost:11434"))
            .modelName(env.getOrDefault("OLLAMA_EMBEDDING_MODEL", "nomic-embed-text"))
            .timeout(Duration.ofSeconds(60))
            .maxRetries(1);

        parseOptionalInteger(env.get("OLLAMA_EMBEDDING_DIMENSIONS"))
            .ifPresent(builder::dimensions);

        return builder.build();
    }

    private static String required(Map<String, String> env, String key) {
        String value = env.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("缺少环境变量：" + key);
        }
        return value;
    }

    private static java.util.Optional<Integer> parseOptionalInteger(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(Integer.parseInt(rawValue.trim()));
    }
}
