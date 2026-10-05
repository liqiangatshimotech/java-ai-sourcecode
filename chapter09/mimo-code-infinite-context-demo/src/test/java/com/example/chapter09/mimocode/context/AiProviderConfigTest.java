package com.example.chapter09.mimocode.context;

import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 下载包演示允许读取模型配置，但任何输出都不应携带密钥片段。 */
class AiProviderConfigTest {
    @Test void displayDoesNotExposeCredentialFragments() {
        var config = AiProviderConfig.fromEnvironment(Map.of("DEEPSEEK_API_KEY", "prefix-test-secret-suffix"));
        assertEquals("deepseek", config.provider());
        assertEquals("(configured)", config.maskedApiKey());
        assertFalse(config.toString().contains("prefix"));
        assertFalse(config.toString().contains("suffix"));
    }
    @Test void threeProviderProfilesDoNotRequireExternalFiles() {
        assertEquals("deepseek", AiProviderConfig.fromEnvironment(Map.of()).provider());
        assertEquals("ollama", AiProviderConfig.fromEnvironment(Map.of("APP_AI_DEFAULT_PROVIDER", "ollama")).provider());
        assertEquals("chatgpt", AiProviderConfig.fromEnvironment(Map.of("APP_AI_DEFAULT_PROVIDER", "openai")).provider());
    }
}
