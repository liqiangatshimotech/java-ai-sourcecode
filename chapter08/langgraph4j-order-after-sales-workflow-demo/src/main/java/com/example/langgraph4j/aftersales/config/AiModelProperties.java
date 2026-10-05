package com.example.langgraph4j.aftersales.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 模型连接配置。密钥只由环境变量映射进来，任何日志都只使用 {@link #safeSummary()}。
 */
@ConfigurationProperties(prefix = "aftersales.ai")
public class AiModelProperties {

    private boolean enabled;
    private String provider = "deepseek";
    private String apiKey = "";
    private String baseUrl = "https://api.deepseek.com";
    private String model = "deepseek-v4-flash";
    private double temperature = 0.0;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }

    public AiProvider providerEnum() {
        return AiProvider.from(provider);
    }

    public String safeSummary() {
        return "provider=%s, model=%s, baseUrl=%s, apiKeyConfigured=%s"
                .formatted(providerEnum(), model, baseUrl, apiKey != null && !apiKey.isBlank());
    }
}
