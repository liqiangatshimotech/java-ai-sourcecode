package com.example.agentscope2.bailian.config;

import java.util.Map;

/**
 * AgentScope 2.0 模型接入配置。
 *
 * <p>所有密钥都从环境变量读取。这里不读取配置文件，是为了保证代码仓库、OSS 包和文档里都不会出现真实 API Key。
 */
public record BailianModelSettings(
        AiProvider provider,
        String apiKey,
        String modelName,
        String baseUrl,
        boolean stream,
        boolean enableThinking) {

    public static BailianModelSettings fromEnvironment() {
        return from(System.getenv());
    }

    public static BailianModelSettings from(Map<String, String> env) {
        AiProvider provider = AiProvider.from(env.getOrDefault("AI_PROVIDER", "deepseek"));
        boolean stream = Boolean.parseBoolean(env.getOrDefault("AI_STREAM", "true"));
        boolean enableThinking = Boolean.parseBoolean(env.getOrDefault("BAILIAN_ENABLE_THINKING", "false"));

        return switch (provider) {
            case DEEPSEEK -> new BailianModelSettings(
                    provider,
                    env.getOrDefault("DEEPSEEK_API_KEY", ""),
                    env.getOrDefault("DEEPSEEK_MODEL", "deepseek-v4-flash"),
                    env.getOrDefault("DEEPSEEK_BASE_URL", "https://api.deepseek.com/v1"),
                    stream,
                    false);
            case BAILIAN -> new BailianModelSettings(
                    provider,
                    firstNonBlank(env, "DASHSCOPE_API_KEY", "BAILIAN_API_KEY"),
                    firstNonBlank(env, "BAILIAN_MODEL", "DASHSCOPE_MODEL", "qwen-plus"),
                    env.getOrDefault("BAILIAN_BASE_URL", ""),
                    stream,
                    enableThinking);
            case CHATGPT -> new BailianModelSettings(
                    provider,
                    env.getOrDefault("OPENAI_API_KEY", ""),
                    env.getOrDefault("OPENAI_MODEL", "gpt-5.5"),
                    env.getOrDefault("OPENAI_BASE_URL", "https://api.openai.com/v1"),
                    stream,
                    false);
            case OLLAMA -> new BailianModelSettings(
                    provider,
                    "",
                    env.getOrDefault("OLLAMA_MODEL", "qwen3"),
                    env.getOrDefault("OLLAMA_BASE_URL", "http://localhost:11434"),
                    stream,
                    false);
        };
    }

    public boolean requiresApiKey() {
        return provider != AiProvider.OLLAMA;
    }

    public boolean hasUsableCredential() {
        return !requiresApiKey() || (apiKey != null && !apiKey.isBlank());
    }

    public String requiredCredentialName() {
        return switch (provider) {
            case DEEPSEEK -> "DEEPSEEK_API_KEY";
            case BAILIAN -> "DASHSCOPE_API_KEY";
            case CHATGPT -> "OPENAI_API_KEY";
            case OLLAMA -> "OLLAMA_API_KEY";
        };
    }

    public String modelId() {
        return switch (provider) {
            case DEEPSEEK, CHATGPT -> "openai:" + modelName;
            case BAILIAN -> "dashscope:" + modelName;
            case OLLAMA -> "ollama:" + modelName;
        };
    }

    public String summary() {
        String baseUrlText = baseUrl == null || baseUrl.isBlank() ? "(provider default)" : baseUrl;
        return "provider=%s, model=%s, baseUrl=%s, stream=%s, enableThinking=%s"
                .formatted(provider, modelName, baseUrlText, stream, enableThinking);
    }

    private static String firstNonBlank(Map<String, String> env, String firstKey, String secondKey) {
        String firstValue = env.get(firstKey);
        if (firstValue != null && !firstValue.isBlank()) {
            return firstValue;
        }
        return env.getOrDefault(secondKey, "");
    }

    private static String firstNonBlank(Map<String, String> env, String firstKey, String secondKey, String defaultValue) {
        String firstValue = env.get(firstKey);
        if (firstValue != null && !firstValue.isBlank()) {
            return firstValue;
        }
        String secondValue = env.get(secondKey);
        if (secondValue != null && !secondValue.isBlank()) {
            return secondValue;
        }
        return defaultValue;
    }
}
