package com.example.langchain4jpersistentmemory.store;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import java.util.List;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** 用一行 JSON 保存一个窗口；JSON 数组保留消息顺序，事务防止删除后插入失败。 */
public class JdbcChatMemoryStore implements ChatMemoryStore {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;
    public JdbcChatMemoryStore(JdbcTemplate jdbc) {
        this.jdbc=jdbc;
        this.transaction=new TransactionTemplate(new DataSourceTransactionManager(Objects.requireNonNull(jdbc.getDataSource())));
    }
    @Override public List<ChatMessage> getMessages(Object memoryId) {
        String json=jdbc.query("select messages_json from ai_chat_memory where memory_id = ?",
                rs -> rs.next()?rs.getString(1):"[]",key(memoryId));
        return List.copyOf(ChatMessageDeserializer.messagesFromJson(json));
    }
    @Override public void updateMessages(Object memoryId,List<ChatMessage> messages) {
        String id=key(memoryId);
        String json=ChatMessageSerializer.messagesToJson(List.copyOf(messages));
        transaction.executeWithoutResult(status -> {
            jdbc.update("delete from ai_chat_memory where memory_id = ?",id);
            jdbc.update("insert into ai_chat_memory(memory_id,messages_json) values (?,?)",id,json);
        });
        // 单次替换原子，不代表跨模型调用的读-改-写被锁住；同一 ID 仍需串行请求。
    }
    @Override public void deleteMessages(Object memoryId) {
        jdbc.update("delete from ai_chat_memory where memory_id = ?",key(memoryId));
    }
    private String key(Object value) {
        // 明确只接受字符串，避免数字 1 与字符串 "1" 被无意映射到同一个 ID。
        if (!(value instanceof String id) || id.isBlank() || id.length()>128)
            throw new IllegalArgumentException("memoryId 必须是 1～128 字符的非空字符串");
        return id;
    }
}
