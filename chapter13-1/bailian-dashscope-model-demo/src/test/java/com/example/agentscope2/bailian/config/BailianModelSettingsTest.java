package com.example.agentscope2.bailian.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class BailianModelSettingsTest {

    @Test
    void defaultsToDeepSeek() {
        BailianModelSettings settings = BailianModelSettings.from(Map.of());

        assertEquals(AiProvider.DEEPSEEK, settings.provider());
        assertEquals("deepseek-v4-flash", settings.modelName());
        assertEquals("https://api.deepseek.com/v1", settings.baseUrl());
        assertFalse(settings.hasUsableCredential());
    }

    @Test
    void supportsBailianDashScopeConfiguration() {
        BailianModelSettings settings = BailianModelSettings.from(Map.of(
                "AI_PROVIDER", "bailian",
                "DASHSCOPE_API_KEY", "sk-test",
                "BAILIAN_MODEL", "qwen-plus"));

        assertEquals(AiProvider.BAILIAN, settings.provider());
        assertEquals("qwen-plus", settings.modelName());
        assertEquals("dashscope:qwen-plus", settings.modelId());
        assertTrue(settings.hasUsableCredential());
    }

    @Test
    void supportsBailianAliasAndBaseUrl() {
        BailianModelSettings settings = BailianModelSettings.from(Map.of(
                "AI_PROVIDER", "dashscope",
                "BAILIAN_API_KEY", "sk-test",
                "DASHSCOPE_MODEL", "qwen3.7-plus",
                "BAILIAN_BASE_URL", "https://workspace.cn-beijing.maas.aliyuncs.com"));

        assertEquals(AiProvider.BAILIAN, settings.provider());
        assertEquals("qwen3.7-plus", settings.modelName());
        assertEquals("https://workspace.cn-beijing.maas.aliyuncs.com", settings.baseUrl());
        assertTrue(settings.hasUsableCredential());
    }

    @Test
    void supportsOllamaWithoutApiKey() {
        BailianModelSettings settings = BailianModelSettings.from(Map.of("AI_PROVIDER", "ollama"));

        assertEquals(AiProvider.OLLAMA, settings.provider());
        assertEquals("ollama:qwen3", settings.modelId());
        assertTrue(settings.hasUsableCredential());
    }
}
