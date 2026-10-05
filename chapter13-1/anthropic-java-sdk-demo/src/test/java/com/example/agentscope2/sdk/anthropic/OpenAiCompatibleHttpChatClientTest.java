package com.example.agentscope2.sdk.anthropic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class OpenAiCompatibleHttpChatClientTest {

    @Test
    void buildsChatCompletionsUrl() {
        assertEquals(
                "https://api.deepseek.com/v1/chat/completions",
                OpenAiCompatibleHttpChatClient.chatCompletionsUrl("https://api.deepseek.com/v1/"));
    }

    @Test
    void buildsRequestJsonWithModelAndPrompt() throws Exception {
        String json = OpenAiCompatibleHttpChatClient.buildRequestJson("deepseek-v4-flash", "说一下 SDK adapter");

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
