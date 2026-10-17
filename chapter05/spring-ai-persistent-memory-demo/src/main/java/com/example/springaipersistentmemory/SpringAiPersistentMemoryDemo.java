package com.example.springaipersistentmemory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
/** 真正启动 JDBC 会话记忆服务；不再通过文件模拟框架 Repository。 */
@SpringBootApplication
public class SpringAiPersistentMemoryDemo {
    public static void main(String[] args) { SpringApplication.run(SpringAiPersistentMemoryDemo.class,args); }
}
