package com.example.springaitool.config;

import com.example.springaitool.tool.CommerceTools;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ToolConfiguration {

    @Bean
    public ToolCallbackProvider commerceToolCallbacks(CommerceTools commerceTools) {
        // MethodToolCallbackProvider 会扫描 @Tool 方法，生成模型可调用的 ToolCallback。
        return MethodToolCallbackProvider.builder()
            .toolObjects(commerceTools)
            .build();
    }
}
