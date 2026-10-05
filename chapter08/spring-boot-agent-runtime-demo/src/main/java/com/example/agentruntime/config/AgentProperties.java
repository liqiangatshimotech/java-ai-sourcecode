package com.example.agentruntime.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 集中保存模型连接和运行时限制。apiKey 虽然存在于内存配置中，但任何响应和日志都不应输出它。
 * Ollama 可把 baseUrl 设为 http://localhost:11434/v1；ChatGPT 设为 https://api.openai.com/v1。
 */
@ConfigurationProperties("ai")
public record AgentProperties(Model model, Runtime runtime) {
    public record Model(String provider, String baseUrl, String modelName, String apiKey) {}
    public record Runtime(int maxSteps, int sessionHistoryLimit) {}
}
