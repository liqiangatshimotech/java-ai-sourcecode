package com.example.agentruntime.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.Locale;
/** 目前只保存配置，不调用模型；provider 的切换同时选择正确的地址和密钥。 */
@ConfigurationProperties("ai")
public record AgentProperties(Model model, Runtime runtime) {
    public record Model(String provider, String baseUrl, String modelName, String apiKey) {
        public Model {
            provider = provider == null || provider.isBlank() ? "deepseek" : provider.toLowerCase(Locale.ROOT);
            if (provider.equals("chatgpt")) provider = "openai";
            String defaultUrl, defaultModel, keyName;
            switch (provider) {
                case "deepseek" -> { defaultUrl = "https://api.deepseek.com"; defaultModel = "deepseek-flash"; keyName = "DEEPSEEK_API_KEY"; }
                case "ollama" -> { defaultUrl = "http://localhost:11434/v1"; defaultModel = "deepseek-r1:7b"; keyName = ""; }
                case "openai" -> { defaultUrl = "https://api.openai.com/v1"; defaultModel = "gpt-4.1-mini"; keyName = "OPENAI_API_KEY"; }
                default -> throw new IllegalArgumentException("不支持的 AI_PROVIDER：" + provider);
            }
            if (baseUrl == null || baseUrl.isBlank()) baseUrl = defaultUrl;
            if (modelName == null || modelName.isBlank()) modelName = defaultModel;
            if (apiKey == null || apiKey.isBlank()) apiKey = keyName.isEmpty() ? "" : System.getenv().getOrDefault(keyName, "");
        }
        @Override public String toString() { return "Model[provider=" + provider + ", baseUrl=" + baseUrl + ", modelName=" + modelName + ", apiKey=***]"; }
    }
    public record Runtime(int maxSteps, int sessionHistoryLimit) {
        public Runtime {
            if (maxSteps < 1 || sessionHistoryLimit < 1) throw new IllegalArgumentException("步数和历史上限必须大于 0");
        }
    }
}
