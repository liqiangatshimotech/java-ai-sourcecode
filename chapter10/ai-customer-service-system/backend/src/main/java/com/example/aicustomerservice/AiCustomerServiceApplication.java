package com.example.aicustomerservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

/**
 * 数据源不再由 Spring Boot 无条件创建，而是在 CustomerServiceConfiguration 中根据
 * support.storage.type 选择。这样 memory 模式不会因为本机没有 PostgreSQL 而把健康检查标记为 DOWN。
 */
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
public class AiCustomerServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiCustomerServiceApplication.class, args);
    }
}
