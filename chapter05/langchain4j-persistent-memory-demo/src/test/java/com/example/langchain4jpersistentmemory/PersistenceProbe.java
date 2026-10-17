package com.example.langchain4jpersistentmemory;
import com.example.langchain4jpersistentmemory.store.JdbcChatMemoryStore;
/** 由测试启动的独立 JVM，验证数据库内容不依赖原进程内存。 */
public class PersistenceProbe {
    public static void main(String[] args) {
        System.out.println(new JdbcChatMemoryStore(StoreIntegrationTest.jdbc(args[0])).getMessages("restart"));
    }
}
