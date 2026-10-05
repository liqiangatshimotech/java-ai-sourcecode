package com.example.langchain4jticketclassifier.service;

import com.example.langchain4jticketclassifier.assistant.ObservableTicketClassifierAssistant;
import com.example.langchain4jticketclassifier.assistant.TicketClassifierAssistant;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.AiServices;

import java.time.Duration;

public class TicketClassifierFactory {

    public TicketClassifierAssistant create(String baseUrl, String modelName) {
        return AiServices.builder(TicketClassifierAssistant.class)
            .chatModel(chatModel(baseUrl, modelName))
            .build();
    }

    public ObservableTicketClassifierAssistant createObservable(String baseUrl, String modelName) {
        return AiServices.builder(ObservableTicketClassifierAssistant.class)
            .chatModel(chatModel(baseUrl, modelName))
            .build();
    }

    /** 默认真实 DeepSeek；Ollama 与 ChatGPT 通过环境变量切换。 */
    public TicketClassifierAssistant createFromEnvironment() {
        return AiServices.builder(TicketClassifierAssistant.class).chatModel(configuredModel()).build();
    }

    public ObservableTicketClassifierAssistant createObservableFromEnvironment() {
        return AiServices.builder(ObservableTicketClassifierAssistant.class).chatModel(configuredModel()).build();
    }

    private ChatModel configuredModel() {
        String provider = System.getenv().getOrDefault("AI_PROVIDER", "deepseek").toLowerCase(java.util.Locale.ROOT);
        if (provider.equals("ollama")) return chatModel(System.getenv().getOrDefault("OLLAMA_BASE_URL", "http://localhost:11434"),
                System.getenv().getOrDefault("OLLAMA_MODEL", "qwen2.5:7b"));
        if (!java.util.Set.of("deepseek", "openai", "chatgpt").contains(provider)) throw new IllegalArgumentException("Unsupported AI_PROVIDER");
        String prefix = provider.equals("deepseek") ? "DEEPSEEK" : "OPENAI";
        String key = System.getenv().getOrDefault(prefix + "_API_KEY", "");
        if (key.isBlank()) throw new IllegalStateException("请配置 " + prefix + "_API_KEY");
        String base = System.getenv().getOrDefault(prefix + "_BASE_URL", provider.equals("deepseek") ? "https://api.deepseek.com/v1" : "https://api.openai.com/v1").replaceAll("/+$", "");
        if (!base.endsWith("/v1")) base += "/v1";
        return dev.langchain4j.model.openai.OpenAiChatModel.builder().baseUrl(base).apiKey(key)
            .modelName(System.getenv().getOrDefault(prefix + "_MODEL", provider.equals("deepseek") ? "deepseek-v4-flash" : "gpt-4.1-mini"))
            .temperature(0.0).timeout(Duration.ofSeconds(60)).build();
    }

    private ChatModel chatModel(String baseUrl, String modelName) {
        return OllamaChatModel.builder()
            .baseUrl(baseUrl)
            .modelName(modelName)
            .temperature(0.0)
            .timeout(Duration.ofSeconds(60))
            .build();
    }
}
