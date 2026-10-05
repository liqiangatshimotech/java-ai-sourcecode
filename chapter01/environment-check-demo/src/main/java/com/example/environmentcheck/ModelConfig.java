package com.example.environmentcheck;

import java.net.URI;
import java.util.Locale;
import java.util.Map;

/**
 * 三种供应商共用一份配置，API Key 只在发请求时进入 Authorization 头。
 * 不使用含密钥的 record 默认 toString，避免调试打印时泄露配置。
 */
public final class ModelConfig {
    private final String provider;
    private final String model;
    private final URI endpoint;
    private final String apiKey;

    private ModelConfig(String provider, String model, URI endpoint, String apiKey) {
        this.provider = provider; this.model = model; this.endpoint = endpoint; this.apiKey = apiKey;
    }

    public static ModelConfig from(Map<String, String> env) {
        String provider = first(env.get("AI_PROVIDER"), "deepseek").toLowerCase(Locale.ROOT);
        if (provider.equals("chatgpt")) provider = "openai";
        String prefix, defaultModel, defaultUrl;
        switch (provider) {
            case "deepseek" -> { prefix = "DEEPSEEK"; defaultModel = "deepseek-v4-flash"; defaultUrl = "https://api.deepseek.com"; }
            case "ollama" -> { prefix = "OLLAMA"; defaultModel = "qwen3"; defaultUrl = "http://localhost:11434/v1"; }
            case "openai" -> { prefix = "OPENAI"; defaultModel = "gpt-4o-mini"; defaultUrl = "https://api.openai.com/v1"; }
            default -> throw new IllegalArgumentException("provider 必须是 deepseek、ollama、openai 或 chatgpt");
        }
        String model = first(env.get("AI_MODEL"), env.get(prefix + "_MODEL"), defaultModel);
        String url = first(env.get("AI_BASE_URL"), env.get(prefix + "_BASE_URL"), defaultUrl);
        URI base;
        try { base = URI.create(url); }
        catch (IllegalArgumentException invalid) { throw new IllegalArgumentException("BASE_URL 不是合法 URI；原值不回显"); }
        if (!("http".equals(base.getScheme()) || "https".equals(base.getScheme()))
                || base.getHost() == null || base.getUserInfo() != null || base.getQuery() != null
                || base.getFragment() != null || base.getPort() > 65535 || base.getPort() == 0) {
            throw new IllegalArgumentException("BASE_URL 需要 HTTP(S) 主机地址，不能包含凭据、查询参数或片段");
        }
        // 接受供应商根地址或已有 /v1 的兼容地址；这里只生成地址，默认不发请求。
        String normalized = base.toString().replaceAll("/+$", "");
        URI endpoint = URI.create(normalized + (normalized.endsWith("/v1") ? "" : "/v1") + "/chat/completions");
        return new ModelConfig(provider, model, endpoint, first(env.get(prefix + "_API_KEY"), env.get("AI_API_KEY"), ""));
    }

    private static String first(String... values) {
        for (String value : values) if (value != null && !value.isBlank()) return value.trim();
        return "";
    }
    public String provider() { return provider; }
    public String model() { return model; }
    public URI endpoint() { return endpoint; }
    public boolean credentialReady() { return provider.equals("ollama") || !apiKey.isBlank(); }
    public boolean apiKeySet() { return !apiKey.isBlank(); }
    String apiKeyForRequest() { return apiKey; }
    @Override public String toString() {
        return "provider=" + provider + ", model=" + model + ", endpoint=" + endpoint + ", apiKeySet=" + apiKeySet();
    }
}
