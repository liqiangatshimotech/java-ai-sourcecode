package com.example.springaiticketclassifier.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 模型创建集中在一处，业务分类与校验代码不随供应商切换。 */
@Configuration
public class CourseModelConfiguration {
    @Bean
    public ChatClient.Builder chatClientBuilder() {
        return ChatClient.builder(createModel(System.getenv()));
    }

    public static ChatModel createModel(java.util.Map<String, String> env) {
        String provider = env.getOrDefault("AI_PROVIDER", "deepseek").toLowerCase(java.util.Locale.ROOT);
        if (provider.equals("ollama")) {
            return OllamaChatModel.builder()
                .ollamaApi(OllamaApi.builder().baseUrl(env.getOrDefault("OLLAMA_BASE_URL", "http://localhost:11434")).build())
                .defaultOptions(OllamaChatOptions.builder().model(env.getOrDefault("OLLAMA_MODEL", "qwen2.5:7b")).temperature(0.0).build())
                .build();
        }
        String prefix;
        String base;
        String model;
        if (provider.equals("deepseek")) {
            prefix = "DEEPSEEK"; base = "https://api.deepseek.com"; model = "deepseek-v4-flash";
        } else if (provider.equals("openai") || provider.equals("chatgpt")) {
            prefix = "OPENAI"; base = "https://api.openai.com"; model = "gpt-4.1-mini";
        } else throw new IllegalArgumentException("AI_PROVIDER 仅支持 deepseek、ollama、openai、chatgpt");
        String key = env.getOrDefault(prefix + "_API_KEY", "");
        if (key.isBlank()) throw new IllegalStateException("请配置 " + prefix + "_API_KEY，或显式选择 AI_PROVIDER=ollama");
        // Spring AI 的 OpenAiApi 默认添加 /v1/chat/completions，base URL 不重复带 /v1。
        return OpenAiChatModel.builder().openAiApi(OpenAiApi.builder()
                .baseUrl(env.getOrDefault(prefix + "_BASE_URL", base).replaceAll("/v1/?$", ""))
                .apiKey(key).build())
                .defaultOptions(OpenAiChatOptions.builder().model(env.getOrDefault(prefix + "_MODEL", model))
                        .temperature(0.0).maxTokens(2048).build()).build();
    }
}
