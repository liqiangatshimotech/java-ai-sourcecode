package com.example.texttosqlagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Text-to-SQL 数据分析 Agent 的 Spring Boot 入口。
 *
 * 业务闭环：运营同学用自然语言提问（例如「2026年6月销售额最高的五个商品分类」），
 * Agent 依次完成：Schema Linking 选表 → 生成 SQL → 安全护栏校验 → 只读执行 →
 * 把结果集翻译成中文结论 → 写审计日志。
 *
 * 启动后数据库是内嵌 H2（schema.sql + data.sql 自动初始化），不需要装 MySQL。
 */
@SpringBootApplication
public class TextToSqlAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(TextToSqlAgentApplication.class, args);
    }
}
