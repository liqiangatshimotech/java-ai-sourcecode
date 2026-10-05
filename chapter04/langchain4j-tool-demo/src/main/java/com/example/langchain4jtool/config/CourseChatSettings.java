package com.example.langchain4jtool.config;

import java.net.URI;
import java.util.Locale;
import java.util.Map;

/** 三种真实回答模型共用配置；默认 DeepSeek V4 Flash，密钥只从环境取并禁止打印。 */
public record CourseChatSettings(String provider, String model, String baseUrl, String apiKey) {
    public static CourseChatSettings from(Map<String, String> env) {
        String provider = first(env.get("AI_PROVIDER"), env.get("APP_AI_DEFAULT_PROVIDER"), "deepseek").toLowerCase(Locale.ROOT);
        if (provider.equals("chatgpt")) provider = "openai";
        String prefix, model, url;
        switch (provider) {
            case "deepseek" -> { prefix="DEEPSEEK"; model="deepseek-v4-flash"; url="https://api.deepseek.com/v1"; }
            case "ollama" -> { prefix="OLLAMA"; model="qwen2.5:7b"; url="http://localhost:11434/v1"; }
            case "openai" -> { prefix="OPENAI"; model="gpt-4.1-mini"; url="https://api.openai.com/v1"; }
            default -> throw new IllegalArgumentException("AI_PROVIDER 仅支持 deepseek、ollama、openai、chatgpt");
        }
        String configured = first(env.get("AI_BASE_URL"), env.get(prefix+"_BASE_URL"), url).replaceAll("/+$", "");
        URI uri;
        try { uri=URI.create(configured); }
        catch(IllegalArgumentException invalid) { throw new IllegalArgumentException("模型地址格式不合法；原值不回显"); }
        if (!("http".equals(uri.getScheme()) || "https".equals(uri.getScheme())) || uri.getHost()==null
            || uri.getUserInfo()!=null || uri.getQuery()!=null || uri.getFragment()!=null) {
            throw new IllegalArgumentException("模型地址必须是无内嵌凭据的 HTTP(S) base URL");
        }
        // 三家均使用兼容协议；这里统一 /v1，避免 Ollama 根地址误调用 /chat/completions。
        if (!configured.endsWith("/v1")) configured+="/v1";
        return new CourseChatSettings(provider, first(env.get("AI_MODEL"),env.get(prefix+"_MODEL"),model),
            configured, first(env.get(prefix+"_API_KEY"),env.get("AI_API_KEY"),""));
    }
    private static String first(String... values) {
        for(String value:values) if(value!=null&&!value.isBlank()) return value.trim();
        return "";
    }
    /** 只有创建真实客户端才要求凭据；离线检查与测试不需要个人 Key。 */
    public String keyForClient() {
        if (provider.equals("ollama")) return apiKey.isBlank()?"not-required":apiKey;
        if (apiKey.isBlank()) throw new IllegalStateException("真实模式请设置 "+provider.toUpperCase(Locale.ROOT)+"_API_KEY");
        return apiKey;
    }
    @Override public String toString() {
        return "provider="+provider+", model="+model+", baseUrl="+baseUrl+", apiKeySet="+!apiKey.isBlank();
    }
}
