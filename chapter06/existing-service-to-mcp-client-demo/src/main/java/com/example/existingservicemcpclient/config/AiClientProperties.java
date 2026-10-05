package com.example.existingservicemcpclient.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai")
public class AiClientProperties {

    private AiProvider defaultProvider = AiProvider.DEEPSEEK;

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
