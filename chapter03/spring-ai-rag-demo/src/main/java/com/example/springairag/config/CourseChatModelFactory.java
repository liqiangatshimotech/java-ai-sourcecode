package com.example.springairag.config;

import java.util.Map;
import java.time.Duration;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;

/** 此项目保留 Spring AI 2.0 M7 API；配置统一，业务 RAG 不区分供应商。 */
public final class CourseChatModelFactory {
    private CourseChatModelFactory() { }
    public static ChatModel create(Map<String,String> env) {
        CourseChatSettings settings=CourseChatSettings.from(env);
        // Spring AI 2.0 的 OpenAI 实现使用 SDK，base URL 保留 /v1，不能套用 1.x OpenAiApi 写法。
        return OpenAiChatModel.builder().options(OpenAiChatOptions.builder()
            .baseUrl(settings.baseUrl()).apiKey(settings.keyForClient()).model(settings.model())
            .temperature(0.0).maxTokens(2048).timeout(Duration.ofSeconds(30)).maxRetries(0).build()).build();
    }
}
