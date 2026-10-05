package com.example.langchain4jchat.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.ai")
public class AiChatProperties {

    @NotNull
    private AiProvider defaultProvider = AiProvider.DEEPSEEK;

    private boolean logRequests = false;

    private boolean logResponses = false;

    @Valid
    private Ollama ollama = new Ollama();

    @Valid
    private OpenAi openai = new OpenAi();

    /** DeepSeek 使用独立配置，避免误用 OpenAI 的密钥与地址。 */
    private final OpenAi deepseek = deepSeekDefaults();

    private static OpenAi deepSeekDefaults() {
        OpenAi settings = new OpenAi();
        settings.setBaseUrl("https://api.deepseek.com/v1");
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

    public boolean isLogRequests() {
        return logRequests;
    }

    public void setLogRequests(boolean logRequests) {
        this.logRequests = logRequests;
    }

    public boolean isLogResponses() {
        return logResponses;
    }

    public void setLogResponses(boolean logResponses) {
        this.logResponses = logResponses;
    }

    public Ollama getOllama() {
        return ollama;
    }

    public void setOllama(Ollama ollama) {
        this.ollama = ollama;
    }

    public OpenAi getOpenai() {
        return openai;
    }

    public void setOpenai(OpenAi openai) {
        this.openai = openai;
    }

    public static class Ollama {
        @NotBlank
        private String baseUrl = "http://localhost:11434";

        @NotBlank
        private String model = "qwen2.5:7b";

        @Positive
        private double temperature = 0.2;

        @NotNull
        private Duration timeout = Duration.ofSeconds(60);

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

        public double getTemperature() {
            return temperature;
        }

        public void setTemperature(double temperature) {
            this.temperature = temperature;
        }

        public Duration getTimeout() {
            return timeout;
        }

        public void setTimeout(Duration timeout) {
            this.timeout = timeout;
        }
    }

    public static class OpenAi {
        private String apiKey = "";

        @NotBlank
        private String baseUrl = "https://api.openai.com/v1";

        @NotBlank
        private String model = "gpt-4.1-mini";

        @Positive
        private double temperature = 0.2;

        @NotNull
        private Duration timeout = Duration.ofSeconds(60);

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

        public double getTemperature() {
            return temperature;
        }

        public void setTemperature(double temperature) {
            this.temperature = temperature;
        }

        public Duration getTimeout() {
            return timeout;
        }

        public void setTimeout(Duration timeout) {
            this.timeout = timeout;
        }

        public boolean isConfigured() {
            return apiKey != null && !apiKey.isBlank();
        }
    }
}
