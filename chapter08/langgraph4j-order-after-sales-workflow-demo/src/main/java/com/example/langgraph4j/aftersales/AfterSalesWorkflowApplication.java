package com.example.langgraph4j.aftersales;

import com.example.langgraph4j.aftersales.config.AiModelProperties;
import com.example.langgraph4j.aftersales.config.CheckpointProperties;
import com.example.langgraph4j.aftersales.config.WorkflowProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * 订单售后状态图 Demo 的启动入口。
 *
 * <p>配置被拆成模型、checkpoint 和工作流三部分，便于分别切换 DeepSeek、
 * Ollama、OpenAI 以及 Memory/MySQL，而不让业务节点感知基础设施。</p>
 */
@SpringBootApplication
@EnableConfigurationProperties({AiModelProperties.class, CheckpointProperties.class, WorkflowProperties.class})
public class AfterSalesWorkflowApplication {

    public static void main(String[] args) {
        SpringApplication.run(AfterSalesWorkflowApplication.class, args);
    }
}
