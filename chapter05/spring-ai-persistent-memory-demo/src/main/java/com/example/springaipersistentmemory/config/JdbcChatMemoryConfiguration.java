package com.example.springaipersistentmemory.config;
import java.time.Clock;
import javax.sql.DataSource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.*;
import org.springframework.ai.chat.memory.repository.jdbc.*;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
/** Repository 负责数据库读写，窗口负责裁剪，Advisor 负责对接真实 ChatClient。 */
@Configuration
public class JdbcChatMemoryConfiguration {
    // 返回具体类型，让 JDBC 自动配置识别已有 Bean，避免再创建第二份 Repository。
    @Bean public JdbcChatMemoryRepository chatMemoryRepository(JdbcTemplate jdbc,DataSource dataSource) {
        return JdbcChatMemoryRepository.builder().jdbcTemplate(jdbc)
                .dialect(JdbcChatMemoryRepositoryDialect.from(dataSource)).build();
    }
    @Bean public ChatMemory chatMemory(ChatMemoryRepository repository) {
        return MessageWindowChatMemory.builder().chatMemoryRepository(repository).maxMessages(10).build();
    }
    @Bean public ChatClient chatClient(ChatModel model,ChatMemory memory) {
        // 没有 ChatClient 自动配置时，显式从 ChatModel 创建 Builder，避免缺少 Builder Bean。
        return ChatClient.builder(model).defaultAdvisors(MessageChatMemoryAdvisor.builder(memory).build()).build();
    }
    @Bean public Clock clock() { return Clock.systemUTC(); }
}
