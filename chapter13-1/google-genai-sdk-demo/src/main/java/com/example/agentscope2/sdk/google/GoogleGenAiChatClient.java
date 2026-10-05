package com.example.agentscope2.sdk.google;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;

/**
 * Google 官方 Gen AI Java SDK adapter。
 *
 * <p>SDK 的 `Client` 会处理 Gemini Developer API 的请求细节。这里不把 `Client` 暴露给业务层，是为了后续替换模型、
 * 接入 AgentScope Runtime 或加入限流重试时，只改 adapter。
 */
public final class GoogleGenAiChatClient implements ChatClient {
    private final GoogleGenAiSettings settings;
    private final Client client;

    public GoogleGenAiChatClient(GoogleGenAiSettings settings) {
        if (!settings.hasUsableCredential()) {
            throw new IllegalStateException("Missing " + settings.requiredCredentialName());
        }
        this.settings = settings;
        this.client = Client.builder().apiKey(settings.apiKey()).build();
    }

    @Override
    public String complete(String userText) {
        GenerateContentResponse response = client.models.generateContent(settings.modelName(), userText, null);
        return response.text();
    }
}
