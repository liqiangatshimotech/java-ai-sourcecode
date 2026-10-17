package com.example.memoryservice;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
/** 本机后端服务：认证、会话窗口、偏好、清空和到期清理均有真实调用入口。 */
@SpringBootApplication @EnableScheduling
public class MemoryServiceFinalDemo {
    public static void main(String[] args) { SpringApplication.run(MemoryServiceFinalDemo.class,args); }
}
