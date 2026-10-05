package com.example.agentscope2.sdk.google;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class OpenAiCompatibleHttpChatClientTest {

    @Test
    void buildsChatCompletionsUrl() {
        assertEquals(
                "http://localhost:11434/v1/chat/completions",
                OpenAiCompatibleHttpChatClient.chatCompletionsUrl("http://localhost:11434/v1/"));
    }

    @Test
    void buildsRequestJsonWithModelAndPrompt() throws Exception {
        String json = OpenAiCompatibleHttpChatClient.buildRequestJson("deepseek-v4-flash", "说一下 adapter 的价值");

        assertTrue(json.contains("\"model\":\"deepseek-v4-flash\""));
        assertTrue(json.contains("adapter"));
    }

    @Test
    void extractsAssistantText() throws Exception {
        String text = OpenAiCompatibleHttpChatClient.extractAssistantText(
                "{\"choices\":[{\"message\":{\"content\":\"ok\"}}]}");

        assertEquals("ok", text);
    }
}
