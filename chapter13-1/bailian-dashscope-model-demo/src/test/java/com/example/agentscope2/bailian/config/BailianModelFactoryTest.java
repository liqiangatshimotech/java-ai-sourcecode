package com.example.agentscope2.bailian.config;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import io.agentscope.extensions.model.dashscope.DashScopeChatModel;
import io.agentscope.extensions.model.ollama.OllamaChatModel;
import io.agentscope.extensions.model.openai.OpenAIChatModel;
import java.util.Map;
import org.junit.jupiter.api.Test;

class BailianModelFactoryTest {

    @Test
    void createsBailianDashScopeModel() {
        BailianModelSettings settings = BailianModelSettings.from(Map.of(
                "AI_PROVIDER", "bailian",
                "DASHSCOPE_API_KEY", "sk-test"));

        assertInstanceOf(DashScopeChatModel.class, BailianModelFactory.create(settings));
    }

    @Test
    void createsDeepSeekWithOpenAiCompatibleModel() {
        BailianModelSettings settings = BailianModelSettings.from(Map.of(
                "AI_PROVIDER", "deepseek",
                "DEEPSEEK_API_KEY", "sk-test"));

        assertInstanceOf(OpenAIChatModel.class, BailianModelFactory.create(settings));
    }

    @Test
    void createsOllamaModel() {
        BailianModelSettings settings = BailianModelSettings.from(Map.of("AI_PROVIDER", "ollama"));

        assertInstanceOf(OllamaChatModel.class, BailianModelFactory.create(settings));
    }
}
