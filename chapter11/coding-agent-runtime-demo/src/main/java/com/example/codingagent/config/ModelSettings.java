package com.example.codingagent.config;

import java.util.Map;
import java.util.Objects;

/**
 * 模型配置只记录运行时需要知道的供应商、模型名和 baseUrl。
 * API Key 只记录是否配置，不输出原始值，避免日志里泄露密钥。
 */
public record ModelSettings(
        AiProvider provider,
        String model,
        String baseUrl,
        boolean apiKeyConfigured
) {

    public static ModelSettings fromEnvironment() {
        return from(System.getenv());
    }

    public static ModelSettings from(Map<String, String> env) {
        Objects.requireNonNull(env, "env must not be null");
        AiProvider provider = AiProvider.from(env.getOrDefault("AI_PROVIDER", "deepseek"));
        String model = firstNonBlank(env.get("AI_MODEL"), env.get(provider.modelEnv()), provider.defaultModel());
        String baseUrl = firstNonBlank(env.get("AI_BASE_URL"), env.get(provider.baseUrlEnv()), provider.defaultBaseUrl());
        boolean apiKeyConfigured = !firstNonBlank(env.get(provider.apiKeyEnv()), env.get("AI_API_KEY"), "").isBlank();
        return new ModelSettings(provider, model, baseUrl, apiKeyConfigured);
    }

    public String summary() {
        return "provider=" + provider.id()
                + ", model=" + model
                + ", baseUrl=" + baseUrl
                + ", apiKeyConfigured=" + apiKeyConfigured;
    }

    private static String firstNonBlank(String first, String second, String fallback) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }
        if (second != null && !second.isBlank()) {
            return second.trim();
        }
        return fallback;
    }
}
