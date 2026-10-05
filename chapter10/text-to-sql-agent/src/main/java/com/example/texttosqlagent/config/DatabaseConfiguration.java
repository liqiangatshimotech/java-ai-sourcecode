package com.example.texttosqlagent.config;

import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/** 数据初始化和业务查询身份隔离，生产配置不会执行 schema.sql/data.sql。 */
@Configuration
public class DatabaseConfiguration {
    @Bean
    @Profile("!production")
    public DataSource demoDataSource() {
        return ReadOnlyDatabaseFactory.createDemo();
    }

    @Bean
    @Profile("production")
    public DataSource productionDataSource(
        @Value("${TEXT2SQL_DB_URL}") String url,
        @Value("${TEXT2SQL_DB_USER}") String user,
        @Value("${TEXT2SQL_DB_PASSWORD}") String password) {
        // 当前护栏明确针对 H2。切换数据库必须同步变更 schema 解析和方言测试。
        if (!url.startsWith("jdbc:h2:") || user.isBlank() || user.equalsIgnoreCase("sa")) {
            throw new IllegalArgumentException("production 需要 H2 URL 与独立的非 sa 只读账号");
        }
        // 非 sa 名称本身不证明权限正确；上线前必须执行 README 的授权验收。
        return new DriverManagerDataSource(url, user, password);
    }
}
