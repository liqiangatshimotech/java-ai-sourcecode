package com.example.langgraph4j.aftersales;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/** 验证默认离线 + MemorySaver 组合可以完整启动 Spring 容器。 */
@SpringBootTest(properties = {
        "aftersales.ai.enabled=false",
        "aftersales.checkpoint.type=memory"
})
class AfterSalesWorkflowApplicationTest {

    @Test
    void contextLoads() {
        // 如果模型、checkpoint、状态图或 Web 层装配失败，容器会在此测试前终止。
    }
}
