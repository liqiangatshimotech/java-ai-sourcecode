package com.example.agentscope2.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import io.agentscope.extensions.model.ollama.OllamaChatModel;
import io.agentscope.extensions.model.openai.OpenAIChatModel;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CourseModelFactoryTest {

    @Test
    void createsDeepSeekWithOpenAiCompatibleModel() {
        CourseModelSettings settings = CourseModelSettings.from(Map.of(
                "AI_PROVIDER", "deepseek",
                "DEEPSEEK_API_KEY", "sk-test"));

        assertInstanceOf(OpenAIChatModel.class, CourseModelFactory.create(settings));
    }

    @Test
    void createsOllamaModel() {
        CourseModelSettings settings = CourseModelSettings.from(Map.of("AI_PROVIDER", "ollama"));

        assertInstanceOf(OllamaChatModel.class, CourseModelFactory.create(settings));
    }

    @Test
    void keepsProviderSpecificDefaults() {
        CourseModelSettings settings = CourseModelSettings.from(Map.of(
                "AI_PROVIDER", "chatgpt",
                "OPENAI_API_KEY", "sk-test"));

        assertEquals("gpt-4o-mini", settings.modelName());
    }
}
