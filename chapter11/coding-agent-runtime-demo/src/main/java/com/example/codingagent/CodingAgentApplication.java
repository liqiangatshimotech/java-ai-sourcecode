package com.example.codingagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 自研 Java Coding Agent 的企业化服务入口。
 *
 * 这个入口让第 11 章示例不再只能通过 main 方法或单元测试运行，
 * 而是可以像真实内部平台一样暴露 HTTP API 和管理台页面。
 */
@SpringBootApplication
public class CodingAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(CodingAgentApplication.class, args);
    }
}
