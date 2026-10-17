package com.example.langchain4jpersistentmemory.config;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import java.time.Duration;
import java.util.Locale;
import java.util.function.Function;

/** 三个供应商共用 OpenAI 兼容协议；Ollama 此处使用 /v1/chat/completions。 */
public final class ModelFactory {
    private ModelFactory() {}
    public static ChatModel fromEnvironment() { return create(System::getenv); }
    public static ChatModel create(Function<String,String> env) {
        String provider = value(env, "APP_AI_DEFAULT_PROVIDER", "deepseek").toLowerCase(Locale.ROOT);
        if (!java.util.List.of("deepseek","ollama","chatgpt").contains(provider))
            throw new IllegalArgumentException("仅支持 deepseek、ollama、chatgpt");
        String prefix = provider.toUpperCase(Locale.ROOT);
        String key = value(env, prefix+"_API_KEY", provider.equals("ollama") ? "ollama" : "");
        if (key.isBlank()) throw new IllegalArgumentException("请设置 "+prefix+"_API_KEY");
        String base = value(env, prefix+"_BASE_URL", switch(provider) {
            case "ollama" -> "http://localhost:11434";
            case "chatgpt" -> "https://api.openai.com";
            default -> "https://api.deepseek.com";
        }).replaceAll("/+$","").replaceAll("/v1$","");
        String model = value(env, prefix+"_MODEL", switch(provider) {
            case "ollama" -> "qwen2.5:7b";
            case "chatgpt" -> "gpt-4.1-mini";
            default -> "deepseek-v4-flash";
        });
        // 记忆测试不依赖隐式重试；错误直接交给调用方处理，避免重复保存用户输入。
        return OpenAiChatModel.builder().baseUrl(base+"/v1").apiKey(key).modelName(model)
                .timeout(Duration.ofSeconds(60)).maxRetries(0).build();
    }
    private static String value(Function<String,String> env,String key,String fallback) {
        String v=env.apply(key);return v==null||v.isBlank()?fallback:v.trim();
    }
}
