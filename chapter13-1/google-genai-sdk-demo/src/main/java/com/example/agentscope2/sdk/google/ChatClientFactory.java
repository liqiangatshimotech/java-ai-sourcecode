package com.example.agentscope2.sdk.google;

/**
 * 根据配置选择真实 adapter。
 *
 * <p>后续接入 AgentScope 2.0 时，可以把这里返回的 `ChatClient` 包成 Model 或 Tool，而不是在业务代码里写 switch。
 */
public final class ChatClientFactory {
    private ChatClientFactory() {}

    public static ChatClient create(GoogleGenAiSettings settings) {
        return switch (settings.provider()) {
            case GOOGLE -> new GoogleGenAiChatClient(settings);
            case DEEPSEEK, OLLAMA, CHATGPT -> new OpenAiCompatibleHttpChatClient(settings);
        };
    }
}
