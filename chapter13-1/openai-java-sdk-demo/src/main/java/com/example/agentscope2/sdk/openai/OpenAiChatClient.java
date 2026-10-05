package com.example.agentscope2.sdk.openai;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;

/**
 * 用 OpenAI Java SDK 封装一次最小的聊天请求。
 *
 * <p>这里故意使用 Chat Completions，而不是只写 OpenAI Responses API。原因是 DeepSeek 和 Ollama 的兼容接口
 * 通常先兼容 `/chat/completions`，这样同一个示例可以覆盖课程默认的 DeepSeek、本地 Ollama 和 OpenAI 官方模型。
 */
public final class OpenAiChatClient {
    private final OpenAiSdkSettings settings;
    private final OpenAIClient client;

    public OpenAiChatClient(OpenAiSdkSettings settings) {
        if (!settings.hasUsableCredential()) {
            throw new IllegalStateException("Missing " + settings.requiredCredentialName());
        }
        this.settings = settings;
        this.client = OpenAIOkHttpClient.builder()
                .apiKey(settings.apiKey())
                .baseUrl(settings.baseUrl())
                .build();
    }

    public String complete(String userText) {
        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .model(settings.modelName())
                .addSystemMessage("你是一个严谨的 Java AI 应用工程助手，回答要短，先给结论。")
                .addUserMessage(userText)
                .build();

        ChatCompletion completion = client.chat().completions().create(params);
        return completion.choices().stream()
                .flatMap(choice -> choice.message().content().stream())
                .findFirst()
                .orElse("");
    }
}
