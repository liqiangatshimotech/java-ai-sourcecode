package com.example.memoryconcepts;
import java.util.Locale;
/** 只展示统一配置形状，不读取或打印密钥，本项目不会发网络请求。 */
public record ModelSettings(String provider,String model,String baseUrl) {
    public static ModelSettings fromEnv() {
        String provider=env("APP_AI_DEFAULT_PROVIDER","deepseek").toLowerCase(Locale.ROOT);
        return switch(provider) {
            case "deepseek" -> new ModelSettings(provider,env("DEEPSEEK_MODEL","deepseek-v4-flash"),env("DEEPSEEK_BASE_URL","https://api.deepseek.com"));
            case "ollama" -> new ModelSettings(provider,env("OLLAMA_MODEL","qwen2.5:7b"),env("OLLAMA_BASE_URL","http://localhost:11434"));
            case "chatgpt" -> new ModelSettings(provider,env("CHATGPT_MODEL","gpt-4.1-mini"),env("CHATGPT_BASE_URL","https://api.openai.com"));
            default -> throw new IllegalArgumentException("只支持 deepseek、ollama、chatgpt");
        };
    }
    private static String env(String key,String fallback) {
        String v=System.getenv(key);return v==null||v.isBlank()?fallback:v.trim();
    }
}
