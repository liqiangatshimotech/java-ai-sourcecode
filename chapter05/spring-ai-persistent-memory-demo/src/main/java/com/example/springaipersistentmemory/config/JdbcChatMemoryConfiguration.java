package com.example.springaipersistentmemory.config;

import javax.sql.DataSource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepositoryDialect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 真实 Spring AI 项目中的 JDBC 会话记忆配置。
 *
 * <p>SpringAiPersistentMemoryDemo.java 为了课堂演示的稳定性使用文件模拟持久化；
 * 这个配置类展示生产项目更常见的接法：用 Spring AI 提供的 JdbcChatMemoryRepository
 * 把会话消息写入数据库表 SPRING_AI_CHAT_MEMORY。</p>
 */
@Configuration
public class JdbcChatMemoryConfiguration {

    /**
     * 创建 JDBC 版 ChatMemoryRepository。
     *
     * <p>JdbcTemplate 负责执行 SQL；DataSource 用来识别数据库类型并选择对应 dialect。
     * 只要 application.yml 中的数据源和 schema 配置正确，上层 ChatMemory 不需要知道底层是
     * H2、PostgreSQL 还是 MySQL。</p>
     */
    @Bean
    ChatMemoryRepository chatMemoryRepository(JdbcTemplate jdbcTemplate, DataSource dataSource) {
        return JdbcChatMemoryRepository.builder()
                .jdbcTemplate(jdbcTemplate)
                // 根据 DataSource 自动选择 SQL 方言。不同数据库的 timestamp 引号、分页和类型细节并不完全一样。
                .dialect(JdbcChatMemoryRepositoryDialect.from(dataSource))
                .build();
    }

    /**
     * 创建带窗口裁剪的 ChatMemory。
     *
     * <p>JdbcChatMemoryRepository 只负责存取消息；MessageWindowChatMemory 决定每次上下文保留多少条。
     * maxMessages 不是越大越好，真实项目要结合模型上下文窗口、延迟和成本一起调。</p>
     */
    @Bean
    ChatMemory chatMemory(ChatMemoryRepository chatMemoryRepository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(10)
                .build();
    }

    /**
     * 给 ChatClient 挂上 MessageChatMemoryAdvisor。
     *
     * <p>Advisor 会在请求前按 conversationId 读取历史消息，并在请求后写入本轮 user/assistant 消息。
     * 调用方仍然需要在每次请求中传入正确的 conversationId，否则记忆无法隔离。</p>
     */
    @Bean
    ChatClient chatClient(ChatClient.Builder builder, ChatMemory chatMemory) {
        return builder
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }
}
