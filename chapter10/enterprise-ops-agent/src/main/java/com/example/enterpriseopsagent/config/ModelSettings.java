package com.example.enterpriseopsagent.config;

/**
 * 模型连接配置。
 *
 * 这里先用普通 Java record 表达配置含义。
 * 真正接入 Spring Boot 时，可以把这些字段迁移到 @ConfigurationProperties。
 */
public record ModelSettings(
    ModelProvider provider,
    String baseUrl,
    String apiKey,
    String modelName,
    double temperature
) {
    /**
     * 默认使用 DeepSeek。
     * 密钥从环境变量读取，避免把 API Key 写进代码仓库。
     */
    public static ModelSettings deepSeekDefault() {
        return new ModelSettings(
            ModelProvider.DEEPSEEK,
            "https://api.deepseek.com",
            System.getenv("DEEPSEEK_API_KEY"),
            "deepseek-v4-flash",
            0.2
        );
    }

    /**
     * Ollama 通常运行在本机，适合离线演示和本地开发。
     */
    public static ModelSettings ollamaLocal() {
        return new ModelSettings(
            ModelProvider.OLLAMA,
            "http://localhost:11434/v1",
            "",
            "qwen2.5:7b",
            0.2
        );
    }

    /**
     * ChatGPT / OpenAI 配置。
     * 保留这个入口，是为了让同一套 Agent Runtime 可以切换模型。
     */
    public static ModelSettings openAiDefault() {
        return new ModelSettings(
            ModelProvider.OPENAI,
            "https://api.openai.com/v1",
            System.getenv("OPENAI_API_KEY"),
            "gpt-4.1-mini",
            0.2
        );
    }

    /**
     * 根据供应商枚举返回默认配置。
     * 业务代码只依赖 ModelSettings，不需要知道每个供应商的默认地址和模型名。
     */
    public static ModelSettings forProvider(ModelProvider provider) {
        return switch (provider) {
            case DEEPSEEK -> deepSeekDefault();
            case OLLAMA -> ollamaLocal();
            case OPENAI -> openAiDefault();
        };
    }

    /**
     * 从命令行参数或环境变量中解析模型供应商。
     *
     * 支持值：
     * - deepseek
     * - ollama
     * - openai
     * - chatgpt
     *
     * 空值默认使用 DeepSeek。
     */
    public static ModelSettings fromProviderName(String providerName) {
        String normalized = providerName == null || providerName.isBlank() ? "deepseek" : providerName.trim().toLowerCase(java.util.Locale.ROOT);
        ModelSettings defaults = switch (normalized) {
            case "deepseek" -> deepSeekDefault();
            case "ollama" -> ollamaLocal();
            case "openai", "chatgpt" -> openAiDefault();
            default -> throw new IllegalArgumentException("不支持的模型供应商");
        };
        String prefix = switch (defaults.provider()) {
            case DEEPSEEK -> "DEEPSEEK";
            case OLLAMA -> "OLLAMA";
            case OPENAI -> "OPENAI";
        };
        return new ModelSettings(defaults.provider(), env(prefix + "_BASE_URL", defaults.baseUrl()),
                defaults.apiKey(), env(prefix + "_MODEL", defaults.modelName()), defaults.temperature());
    }

    /**
     * 日志里只输出模型名和供应商，不输出密钥。
     */
    public String safeSummary() {
        return provider + " / " + modelName + " / " + baseUrl;
    }
    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    /** record 默认的 toString 会输出全部字段，这里显式屏蔽密钥。 */
    @Override public String toString() { return safeSummary(); }
}
