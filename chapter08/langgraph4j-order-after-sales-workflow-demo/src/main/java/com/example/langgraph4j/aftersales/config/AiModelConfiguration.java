package com.example.langgraph4j.aftersales.config;

import com.example.langgraph4j.aftersales.ai.AfterSalesAssistant;
import com.example.langgraph4j.aftersales.ai.RuleBasedAfterSalesAssistant;
import com.example.langgraph4j.aftersales.ai.SpringAiAfterSalesAssistant;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.deepseek.DeepSeekChatOptions;
import org.springframework.ai.deepseek.api.DeepSeekApi;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 根据配置创建 Spring AI 模型适配器。 */
@Configuration
public class AiModelConfiguration {

    private static final Logger log = LoggerFactory.getLogger(AiModelConfiguration.class);

    @Bean
    AfterSalesAssistant afterSalesAssistant(AiModelProperties properties) {
        log.info("售后模型配置：{}", properties.safeSummary());
        if (!properties.isEnabled()) {
            log.info("AI_ENABLED=false，使用可离线运行的规则助手");
            return new RuleBasedAfterSalesAssistant();
        }
        validate(properties);
        ChatModel chatModel = switch (properties.providerEnum()) {
            case DEEPSEEK -> deepSeek(properties);
            case OLLAMA -> ollama(properties);
            case OPENAI -> openAi(properties);
        };
        // LangGraph4j 1.8.x saver 使用 Jackson 2，这里保持同一条序列化基线。
        return new SpringAiAfterSalesAssistant(chatModel, new ObjectMapper());
    }

    private static ChatModel deepSeek(AiModelProperties properties) {
        DeepSeekApi api = DeepSeekApi.builder()
                .baseUrl(properties.getBaseUrl())
                .apiKey(properties.getApiKey())
                .build();
        return DeepSeekChatModel.builder()
                .deepSeekApi(api)
                .options(DeepSeekChatOptions.builder()
                        .model(properties.getModel())
                        .temperature(properties.getTemperature())
                        .build())
                .build();
    }

    private static ChatModel ollama(AiModelProperties properties) {
        return OllamaChatModel.builder()
                .ollamaApi(OllamaApi.builder().baseUrl(properties.getBaseUrl()).build())
                .options(OllamaChatOptions.builder()
                        .model(properties.getModel())
                        .temperature(properties.getTemperature())
                        .build())
                .build();
    }

    private static ChatModel openAi(AiModelProperties properties) {
        return OpenAiChatModel.builder()
                .options(OpenAiChatOptions.builder()
                        .baseUrl(properties.getBaseUrl())
                        .apiKey(properties.getApiKey())
                        .model(properties.getModel())
                        .temperature(properties.getTemperature())
                        .build())
                .build();
    }

    private static void validate(AiModelProperties properties) {
        if (properties.providerEnum() != AiProvider.OLLAMA
                && (properties.getApiKey() == null || properties.getApiKey().isBlank())) {
            throw new IllegalStateException(properties.providerEnum() + " 已启用，但没有配置 API Key");
        }
    }
}
