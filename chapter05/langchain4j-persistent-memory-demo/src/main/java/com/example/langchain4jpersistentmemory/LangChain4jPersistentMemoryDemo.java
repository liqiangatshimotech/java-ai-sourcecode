package com.example.langchain4jpersistentmemory;
import com.example.langchain4jpersistentmemory.config.ModelFactory;
import com.example.langchain4jpersistentmemory.rag.RagMemoryBoundary;
import com.example.langchain4jpersistentmemory.store.JdbcChatMemoryStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/** 文件型 H2 默认落地到 data 目录；再次启动能读取同一 memoryId 的窗口。 */
public class LangChain4jPersistentMemoryDemo {
    public static void main(String[] args) {
        String url=System.getenv().getOrDefault("APP_MEMORY_DB_URL","jdbc:h2:file:./data/langchain4j-memory");
        var ds=new DriverManagerDataSource(url,"sa","");
        var jdbc=new JdbcTemplate(ds);
        // 此 DDL 面向示例 H2；迁移其他数据库时应提供对应的迁移脚本。
        jdbc.execute("create table if not exists ai_chat_memory(memory_id varchar(128) primary key,messages_json clob not null)");
        var store=new JdbcChatMemoryStore(jdbc);
        String id="user-a:conversation-a";
        System.out.println("启动时已有消息："+store.getMessages(id).size());
        var assistant=RagMemoryBoundary.create(ModelFactory.fromEnvironment(),store,false);
        System.out.println(assistant.chat(id,"退款一般多久到账？"));
        // false 只控制 UserMessage；AI 回复若引用政策，仍会留在窗口中。
        System.out.println("保存后的消息："+store.getMessages(id));
    }
}
