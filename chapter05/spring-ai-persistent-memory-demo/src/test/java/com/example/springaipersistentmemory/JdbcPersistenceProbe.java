package com.example.springaipersistentmemory;
import com.example.springaipersistentmemory.memory.PreferenceMemoryStore;
import java.time.Clock;
import java.util.stream.Collectors;
import org.springframework.ai.chat.memory.repository.jdbc.*;
import org.springframework.ai.chat.messages.Message;
/** 独立 JVM 验证真正的文件数据库恢复，不依赖原进程里的 Java 对象。 */
public class JdbcPersistenceProbe {
    public static void main(String[] args) {
        var jdbc=PersistenceAndPreferenceTest.jdbc(args[0]);
        var repository=JdbcChatMemoryRepository.builder().jdbcTemplate(jdbc)
                .dialect(JdbcChatMemoryRepositoryDialect.from(jdbc.getDataSource())).build();
        System.out.println(repository.findByConversationId("restart").stream().map(Message::getText).collect(Collectors.joining("|")));
        System.out.println(new PreferenceMemoryStore(jdbc,Clock.systemUTC()).findActive("a"));
    }
}
