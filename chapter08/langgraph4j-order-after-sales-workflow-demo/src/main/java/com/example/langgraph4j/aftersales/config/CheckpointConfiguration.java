package com.example.langgraph4j.aftersales.config;

import com.mysql.cj.jdbc.MysqlDataSource;
import org.bsc.langgraph4j.checkpoint.BaseCheckpointSaver;
import org.bsc.langgraph4j.checkpoint.CreateOption;
import org.bsc.langgraph4j.checkpoint.MemorySaver;
import org.bsc.langgraph4j.checkpoint.MysqlSaver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 创建 LangGraph4j 官方 checkpoint saver。 */
@Configuration
public class CheckpointConfiguration {

    @Bean
    BaseCheckpointSaver checkpointSaver(CheckpointProperties properties) {
        if (!properties.mysqlEnabled()) {
            // MemorySaver 适合本地和单测；进程退出后状态会消失。
            return new MemorySaver();
        }

        // MysqlSaver 1.8.23 会创建 LANGRAPH4J_THREAD / LANGRAPH4J_CHECKPOINT 两张表。
        MysqlDataSource dataSource = new MysqlDataSource();
        dataSource.setURL(properties.getJdbcUrl());
        dataSource.setUser(properties.getUsername());
        dataSource.setPassword(properties.getPassword());
        return MysqlSaver.builder()
                .dataSource(dataSource)
                .createOption(CreateOption.CREATE_IF_NOT_EXISTS)
                .build();
    }
}
