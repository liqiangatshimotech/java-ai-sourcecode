package com.example.agentscopeframework.config;

import java.util.Locale;

/**
 * 课程里的模型提供商枚举。
 *
 * <p>后续章节默认使用 DeepSeek，但示例项目仍然保留 Ollama 和 ChatGPT 配置，方便在本地或海外环境中替换。
 */
public enum AiProvider {
    DEEPSEEK,
    OLLAMA,
    CHATGPT;

    /**
     * 从环境变量字符串解析 provider。
     *
     * <p>这里故意支持几个常见别名，避免命令行里大小写或命名稍有不同就启动失败。
     */
    public static AiProvider from(String value) {
        if (value == null || value.isBlank()) {
            return DEEPSEEK;
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "deepseek", "ds" -> DEEPSEEK;
            case "ollama", "local" -> OLLAMA;
            case "chatgpt", "openai", "gpt" -> CHATGPT;
            default -> throw new IllegalArgumentException("Unsupported AI_PROVIDER: " + value);
        };
    }
}
