package com.example.agentscope2.sdk.openai;

import java.util.Map;

/**
 * 把模型接入参数集中到一个不可变对象里。
 *
 * <p>真实项目里不要把 API Key 写进配置文件或代码仓库。这个示例只读环境变量，打包到 OSS 后也不会泄漏本机密钥。
 */
public record OpenAiSdkSettings(
        AiProvider provider,
        String apiKey,
        String modelName,
        String baseUrl) {

    public static OpenAiSdkSettings fromEnvironment() {
        return from(System.getenv());
    }

    public static OpenAiSdkSettings from(Map<String, String> env) {
        AiProvider provider = AiProvider.from(env.getOrDefault("AI_PROVIDER", "deepseek"));
        return switch (provider) {
            case DEEPSEEK -> new OpenAiSdkSettings(
                    provider,
                    env.getOrDefault("DEEPSEEK_API_KEY", ""),
                    env.getOrDefault("DEEPSEEK_MODEL", "deepseek-v4-flash"),
                    env.getOrDefault("DEEPSEEK_BASE_URL", "https://api.deepseek.com/v1"));
            case OLLAMA -> new OpenAiSdkSettings(
                    provider,
                    env.getOrDefault("OLLAMA_API_KEY", "ollama"),
                    env.getOrDefault("OLLAMA_MODEL", "qwen3"),
                    env.getOrDefault("OLLAMA_BASE_URL", "http://localhost:11434/v1"));
            case CHATGPT -> new OpenAiSdkSettings(
                    provider,
                    env.getOrDefault("OPENAI_API_KEY", ""),
                    env.getOrDefault("OPENAI_MODEL", "gpt-5.5"),
                    env.getOrDefault("OPENAI_BASE_URL", "https://api.openai.com/v1"));
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
            case CHATGPT -> "OPENAI_API_KEY";
            case OLLAMA -> "OLLAMA_API_KEY";
        };
    }

    public String summary() {
        return "provider=%s, model=%s, baseUrl=%s".formatted(provider, modelName, baseUrl);
    }
}
