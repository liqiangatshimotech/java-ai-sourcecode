package com.example.agentscope2.sdk.anthropic;

/**
 * 根据配置选择真实模型 adapter。
 */
public final class ChatClientFactory {
    private ChatClientFactory() {}

    public static ChatClient create(AnthropicSdkSettings settings) {
        return switch (settings.provider()) {
            case ANTHROPIC -> new AnthropicChatClient(settings);
            case DEEPSEEK, OLLAMA, CHATGPT -> new OpenAiCompatibleHttpChatClient(settings);
        };
    }
}
