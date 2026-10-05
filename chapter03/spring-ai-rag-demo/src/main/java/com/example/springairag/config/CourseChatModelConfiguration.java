package com.example.springairag.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 保留现有 RAG_AI_MODE 显式入口；fake 模式不初始化真实模型客户端。 */
@Configuration
public class CourseChatModelConfiguration {
    @Bean
    @ConditionalOnProperty(name="app.rag.ai-mode",havingValue="spring-ai")
    public ChatClient.Builder chatClientBuilder() {
        return ChatClient.builder(CourseChatModelFactory.create(System.getenv()));
    }
}
