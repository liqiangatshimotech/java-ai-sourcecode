package com.example.agentscope2.sdk.anthropic;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;

/**
 * Anthropic 官方 Java SDK adapter。
 *
 * <p>Anthropic Messages API 使用顶层 system 参数、user/assistant 消息和 `maxTokens`。这里先展示最小调用，
 * 后续可以在这个类里补 streaming、tool use、cache control 和 request id 记录。
 */
public final class AnthropicChatClient implements ChatClient {
    private final AnthropicSdkSettings settings;
    private final AnthropicClient client;

    public AnthropicChatClient(AnthropicSdkSettings settings) {
        if (!settings.hasUsableCredential()) {
            throw new IllegalStateException("Missing " + settings.requiredCredentialName());
        }
        this.settings = settings;
        this.client = AnthropicOkHttpClient.builder()
                .apiKey(settings.apiKey())
                .baseUrl(settings.baseUrl())
                .build();
    }

    @Override
    public String complete(String userText) {
        MessageCreateParams params = MessageCreateParams.builder()
                .model(Model.of(settings.modelName()))
                .maxTokens(512L)
                .addUserMessage(userText)
                .build();

        Message message = client.messages().create(params);
        return message.content().stream()
                .flatMap(contentBlock -> contentBlock.text().stream())
                .map(textBlock -> textBlock.text())
                .findFirst()
                .orElse("");
    }
}
