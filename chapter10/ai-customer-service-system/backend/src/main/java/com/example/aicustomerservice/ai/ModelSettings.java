package com.example.aicustomerservice.ai;

/**
 * 模型运行配置。
 *
 * 课程代码默认使用 DeepSeek，但保留 Ollama 和 ChatGPT 的配置入口。
 * 真实项目里通常把这份配置放到配置中心，这里用环境变量模拟生产部署。
 */
public record ModelSettings(
    AiProvider provider,
    String baseUrl,
    String apiKey,
    String modelName,
    double temperature
) {
    public static ModelSettings fromProviderName(String providerName) {
        String normalizedName = providerName == null ? "deepseek" : providerName.trim().toLowerCase();
        return switch (normalizedName) {
            case "", "deepseek" -> deepSeekDefault();
            case "ollama" -> ollamaLocal();
            case "openai", "chatgpt" -> chatGptDefault();
            default -> throw new IllegalArgumentException("不支持的模型供应商：" + providerName);
        };
    }

    public static ModelSettings deepSeekDefault() {
        return new ModelSettings(
            AiProvider.DEEPSEEK,
            envOrDefault("DEEPSEEK_BASE_URL", "https://api.deepseek.com/v1"),
            System.getenv("DEEPSEEK_API_KEY"),
            envOrDefault("DEEPSEEK_MODEL", "deepseek-v4-flash"),
            doubleEnvOrDefault("AI_TEMPERATURE", 0.2)
        );
    }

    public static ModelSettings ollamaLocal() {
        return new ModelSettings(
            AiProvider.OLLAMA,
            envOrDefault("OLLAMA_BASE_URL", "http://localhost:11434/v1"),
            envOrDefault("OLLAMA_API_KEY", "ollama"),
            envOrDefault("OLLAMA_MODEL", "qwen2.5:7b"),
            doubleEnvOrDefault("AI_TEMPERATURE", 0.2)
        );
    }

    public static ModelSettings chatGptDefault() {
        return new ModelSettings(
            AiProvider.CHATGPT,
            envOrDefault("OPENAI_BASE_URL", "https://api.openai.com/v1"),
            System.getenv("OPENAI_API_KEY"),
            envOrDefault("OPENAI_MODEL", "gpt-4.1-mini"),
            doubleEnvOrDefault("AI_TEMPERATURE", 0.2)
        );
    }

    /**
     * 日志和接口里只展示非敏感信息，避免把 API Key 泄漏到响应或日志。
     */
    public String safeSummary() {
        return provider + " / " + modelName + " / " + baseUrl;
    }

    public boolean configuredForGeneration() {
        return configurationMessage().equals("READY");
    }

    public String configurationMessage() {
        if (isBlank(baseUrl)) {
            return "大模型 Base URL 未配置，请配置对应的 BASE_URL。";
        }
        if (isBlank(modelName)) {
            return "大模型名称未配置，请配置对应的 MODEL。";
        }
        if (provider != AiProvider.OLLAMA && isBlank(apiKey)) {
            return "大模型 API Key 未配置，请配置 " + requiredApiKeyName() + " 后再发起 AI 客服处理。";
        }
        return "READY";
    }

    public String requiredApiKeyName() {
        return switch (provider) {
            case DEEPSEEK -> "DEEPSEEK_API_KEY";
            case CHATGPT -> "OPENAI_API_KEY";
            case OLLAMA -> "OLLAMA_API_KEY";
        };
    }

    private static String envOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private static double doubleEnvOrDefault(String name, double defaultValue) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return Double.parseDouble(value);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
