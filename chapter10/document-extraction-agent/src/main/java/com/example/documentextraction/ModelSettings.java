package com.example.documentextraction;
import java.net.URI;
import java.util.*;

/** 默认DeepSeek只处理OCR文本；视觉能力必须单独选择模型并显式启用。 */
public record ModelSettings(String provider, URI baseUrl, String model, String apiKey,
                            boolean runModel, boolean visionEnabled, String visionModel) {
    public static ModelSettings fromEnvironment(Map<String,String> env) {
        String provider=env.getOrDefault("AI_PROVIDER","deepseek").toLowerCase(Locale.ROOT);
        if(provider.equals("chatgpt"))provider="openai";
        if(!Set.of("deepseek","ollama","openai").contains(provider))throw new IllegalArgumentException("provider仅支持deepseek/ollama/openai/chatgpt");
        String prefix=provider.toUpperCase(Locale.ROOT);
        String defaultBase=switch(provider){case "deepseek"->"https://api.deepseek.com/v1";case "ollama"->"http://localhost:11434/v1";default->"https://api.openai.com/v1";};
        String defaultModel=switch(provider){case "deepseek"->"deepseek-v4-flash";case "ollama"->"qwen2.5:7b";default->"gpt-4.1-mini";};
        return new ModelSettings(provider,URI.create(env.getOrDefault(prefix+"_BASE_URL",defaultBase)),
                env.getOrDefault(prefix+"_MODEL",defaultModel),env.getOrDefault(prefix+"_API_KEY",provider.equals("ollama")?"ollama":""),
                Boolean.parseBoolean(env.getOrDefault("RUN_MODEL_CALL","false")),
                Boolean.parseBoolean(env.getOrDefault("AI_VISION_ENABLED","false")),env.getOrDefault("AI_VISION_MODEL",""));
    }
    public URI endpoint() {
        // 只向明确配置的HTTPS目标发送数据；本机测试和Ollama允许loopback HTTP。
        if(baseUrl.getHost()==null||baseUrl.getUserInfo()!=null||baseUrl.getQuery()!=null||baseUrl.getFragment()!=null
                ||!(baseUrl.getScheme().equals("https")||(baseUrl.getScheme().equals("http")
                && Set.of("localhost","127.0.0.1","[::1]").contains(baseUrl.getHost()))))
            throw new IllegalArgumentException("模型地址须HTTPS，HTTP仅允许本机");
        return URI.create(baseUrl.toString().replaceAll("/+$","")+"/chat/completions");
    }
}
