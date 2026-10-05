package com.example.langgraph4j.aftersales.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/** SSE 与图内并行分支分别使用独立、有界的线程池。 */
@Configuration
public class AsyncConfiguration {

    @Bean(name = "workflowStreamExecutor", destroyMethod = "shutdown")
    ThreadPoolTaskExecutor workflowStreamExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("after-sales-stream-");
        executor.initialize();
        return executor;
    }

    /**
     * LangGraph4j fork-join 专用池。至少两个核心线程，确保 policy/risk
     * 能同时开始；不与慢 SSE 客户端抢线程。Spring 关闭容器时会调用 shutdown。
     */
    @Bean(name = "refundCheckExecutor", destroyMethod = "shutdown")
    ThreadPoolTaskExecutor refundCheckExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("refund-check-");
        executor.initialize();
        return executor;
    }
}
