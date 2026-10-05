package com.example.enterpriseopsagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 企业级智能运维排障 Agent 的服务入口。
 *
 * 这个入口让项目从命令行 demo 变成可以部署的 Spring Boot 应用：
 * 可以打包成 jar，可以暴露 HTTP API，也可以放进容器运行。
 */
@SpringBootApplication
public class EnterpriseOpsAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(EnterpriseOpsAgentApplication.class, args);
    }
}
