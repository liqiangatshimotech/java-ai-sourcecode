package com.example.springaichat.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * Application-level AI configuration.
 *
 * <p>These properties intentionally live under app.ai.* instead of directly using
 * spring.ai.* because this demo creates multiple ChatClient instances manually.
 * That makes provider selection explicit and easier to explain in class.</p>
 */
@ConfigurationProperties(prefix = "app.ai")
public class AiChatProperties {

    /**
     * Provider used when a request does not pass provider.
     */
    private AiProvider defaultProvider = AiProvider.DEEPSEEK;

    /**
     * Global system prompt applied to both synchronous and streaming calls.
     */
    private String systemPrompt = "You are a helpful AI assistant.";

    private final Ollama ollama = new Ollama();

    private final OpenAi openai = new OpenAi();

    /** DeepSeek 使用独立配置，避免误用 OpenAI 的密钥与地址。 */
    private final OpenAi deepseek = deepSeekDefaults();

    private static OpenAi deepSeekDefaults() {
        OpenAi settings = new OpenAi();
        settings.setBaseUrl("https://api.deepseek.com");
        settings.setModel("deepseek-v4-flash");
        return settings;
    }

    public OpenAi getDeepseek() { return deepseek; }

    public AiProvider getDefaultProvider() {
        return defaultProvider;
    }

    public void setDefaultProvider(AiProvider defaultProvider) {
        this.defaultProvider = defaultProvider;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public Ollama getOllama() {
        return ollama;
    }

    public OpenAi getOpenai() {
        return openai;
    }

    public static class Ollama {

        /**
         * Local Ollama server address, for example http://localhost:11434.
         */
        private String baseUrl = "http://localhost:11434";

        /**
         * Ollama model name that has already been pulled locally.
         */
        private String model = "qwen2.5:7b";

        /**
         * Higher temperature means more creative output; lower means more deterministic output.
         */
        private Double temperature = 0.7;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public Double getTemperature() {
            return temperature;
        }

        public void setTemperature(Double temperature) {
            this.temperature = temperature;
        }
    }

    public static class OpenAi {

        /**
         * Commercial model API key. Empty means the OpenAI provider is not registered.
         */
        private String apiKey = "";

        /**
         * OpenAI-compatible base URL. Keep the default for official OpenAI.
         */
        private String baseUrl = "https://api.openai.com";

        private String model = "gpt-4o-mini";

        private Double temperature = 0.7;

        /**
         * Limit generated tokens to control cost and response size.
         */
        private Integer maxTokens = 1024;

        public boolean isConfigured() {
            return StringUtils.hasText(apiKey);
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public Double getTemperature() {
            return temperature;
        }

        public void setTemperature(Double temperature) {
            this.temperature = temperature;
        }

        public Integer getMaxTokens() {
            return maxTokens;
        }

        public void setMaxTokens(Integer maxTokens) {
            this.maxTokens = maxTokens;
        }
    }
}
