package com.example.springaimcpclient.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "app.ai")
public class AiClientProperties {

    private AiProvider defaultProvider = AiProvider.DEEPSEEK;

    private String systemPrompt = """
        你是企业售后支持助手。涉及订单、退款政策或处理建议时，必须优先调用 MCP 工具读取业务数据，
        不要编造订单状态，不要暴露内部备注，不要承诺未授权赔付。
        """;

    private final OpenAiCompatible deepseek = new OpenAiCompatible(
        "https://api.deepseek.com",
        "deepseek-v4-flash",
        0.2,
        1024
    );

    private final Ollama ollama = new Ollama();

    private final OpenAiCompatible chatgpt = new OpenAiCompatible(
        "https://api.openai.com",
        "gpt-4o-mini",
        0.2,
        1024
    );

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

    public OpenAiCompatible getDeepseek() {
        return deepseek;
    }

    public Ollama getOllama() {
        return ollama;
    }

    public OpenAiCompatible getChatgpt() {
        return chatgpt;
    }

    public static class OpenAiCompatible {

        private String apiKey = "";

        private String baseUrl;

        private String model;

        private Double temperature;

        private Integer maxTokens;

        public OpenAiCompatible(String baseUrl, String model, Double temperature, Integer maxTokens) {
            this.baseUrl = baseUrl;
            this.model = model;
            this.temperature = temperature;
            this.maxTokens = maxTokens;
        }

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

    public static class Ollama {

        private String baseUrl = "http://localhost:11434";

        private String model = "qwen3.5:9b";

        private Double temperature = 0.2;

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
}
