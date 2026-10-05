package com.example.a2a.config;

/**
 * 课程示例统一支持的模型供应商。
 *
 * <p>A2A 协议层不强制绑定某个模型。这里保留模型配置入口，是为了后续把示例里的
 * {@code CustomerSupportAgentExecutor} 换成真实大模型或 Agent Runtime 时，不需要重新设计配置。
 */
public enum AiProvider {
    DEEPSEEK,
    OLLAMA,
    CHATGPT;

    public static AiProvider from(String value) {
        if (value == null || value.isBlank()) {
            return DEEPSEEK;
        }
        return switch (value.trim().toLowerCase()) {
            case "ollama" -> OLLAMA;
            case "chatgpt", "openai" -> CHATGPT;
            case "deepseek" -> DEEPSEEK;
            default -> throw new IllegalArgumentException("Unsupported AI_PROVIDER: " + value);
        };
    }
}
