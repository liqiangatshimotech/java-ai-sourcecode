package com.example.texttosqlagent.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * ModelSettings 的单元测试：供应商解析、默认值、非法值、密钥不泄露。
 * 全部离线，不依赖任何环境变量真的存在。
 */
class ModelSettingsTest {

    @Test
    void deepseekIsTheDefaultProvider() {
        // 空值（包括 null 和空白字符串）都应该落到 DeepSeek 默认配置
        assertEquals(ModelProvider.DEEPSEEK, ModelSettings.fromProviderName(null).provider());
        assertEquals(ModelProvider.DEEPSEEK, ModelSettings.fromProviderName("  ").provider());
        // 端点和模型名允许用环境变量覆盖（DEEPSEEK_BASE_URL / DEEPSEEK_MODEL），
        // 所以断言要和环境变量感知后的期望值比，而不是写死字符串
        assertEquals(System.getenv().getOrDefault("DEEPSEEK_MODEL", "deepseek-v4-flash"),
            ModelSettings.fromProviderName(null).modelName());
        assertEquals(System.getenv().getOrDefault("DEEPSEEK_BASE_URL", "https://api.deepseek.com"),
            ModelSettings.fromProviderName(null).baseUrl());
    }

    @Test
    void ollamaAndOpenAiDefaults() {
        ModelSettings ollama = ModelSettings.fromProviderName("ollama");
        assertEquals(ModelProvider.OLLAMA, ollama.provider());
        assertEquals(System.getenv().getOrDefault("OLLAMA_BASE_URL", "http://localhost:11434/v1"),
            ollama.baseUrl());
        assertEquals(System.getenv().getOrDefault("OLLAMA_MODEL", "qwen2.5:7b"), ollama.modelName());

        ModelSettings openAi = ModelSettings.fromProviderName("openai");
        assertEquals(System.getenv().getOrDefault("OPENAI_BASE_URL", "https://api.openai.com/v1"),
            openAi.baseUrl());
        assertEquals(System.getenv().getOrDefault("OPENAI_MODEL", "gpt-4.1-mini"), openAi.modelName());
    }

    @Test
    void parsesAllSupportedProviderNames() {
        assertEquals(ModelProvider.DEEPSEEK, ModelSettings.fromProviderName("deepseek").provider());
        assertEquals(ModelProvider.OLLAMA, ModelSettings.fromProviderName("ollama").provider());
        assertEquals(ModelProvider.OPENAI, ModelSettings.fromProviderName("openai").provider());
        // chatgpt 是 openai 的别名，课程里两种叫法都支持
        assertEquals(ModelProvider.OPENAI, ModelSettings.fromProviderName("chatgpt").provider());
        // 大小写和前后空白不敏感
        assertEquals(ModelProvider.OLLAMA, ModelSettings.fromProviderName(" Ollama ").provider());
    }

    @Test
    void rejectsUnsupportedProvider() {
        // 非法值必须尽早抛异常，不能带病启动
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> ModelSettings.fromProviderName("some-random-model"));
        assertTrue(e.getMessage().contains("some-random-model"));
    }

    @Test
    void safeSummaryNeverLeaksApiKey() {
        // 构造一个带假密钥的配置：safeSummary 里绝不能出现密钥内容
        ModelSettings settings = new ModelSettings(
            ModelProvider.DEEPSEEK, "https://api.deepseek.com", "sk-test-secret-12345", "deepseek-v4-flash", 0.2);
        String summary = settings.safeSummary();
        assertTrue(summary.contains("DEEPSEEK"));
        assertTrue(summary.contains("deepseek-v4-flash"));
        assertFalse(summary.contains("sk-test-secret-12345"));
    }

    @Test
    void hasCredentialDependsOnProviderAndKey() {
        // Ollama 是本机服务，不需要密钥也视为「可调用」
        assertTrue(ModelSettings.ollamaLocal().hasCredential());
        // DeepSeek/OpenAI 没配 key 时视为「不可调用」，装配层会落到离线规则模型
        ModelSettings noKey = new ModelSettings(
            ModelProvider.DEEPSEEK, "https://api.deepseek.com", null, "deepseek-v4-flash", 0.2);
        assertFalse(noKey.hasCredential());
        ModelSettings withKey = new ModelSettings(
            ModelProvider.OPENAI, "https://api.openai.com/v1", "sk-x", "gpt-4.1-mini", 0.2);
        assertTrue(withKey.hasCredential());
    }
}
