package com.example.agentscope2.bailian.config;

import io.agentscope.core.model.Model;
import io.agentscope.extensions.model.dashscope.DashScopeChatModel;
import io.agentscope.extensions.model.dashscope.formatter.DashScopeChatFormatter;
import io.agentscope.extensions.model.ollama.OllamaChatModel;
import io.agentscope.extensions.model.ollama.formatter.OllamaChatFormatter;
import io.agentscope.extensions.model.openai.OpenAIChatModel;
import io.agentscope.extensions.model.openai.formatter.DeepSeekFormatter;
import io.agentscope.extensions.model.openai.formatter.OpenAIChatFormatter;

/**
 * 把课程自己的配置对象转换成 AgentScope Java 2.0 的 Model。
 *
 * <p>百炼模型使用 AgentScope 的 DashScope 扩展模块；DeepSeek 和 ChatGPT 继续走 OpenAI-compatible 模块；
 * Ollama 走本地模型模块。业务代码只拿到统一的 `Model` 接口。
 */
public final class BailianModelFactory {

    private BailianModelFactory() {}

    public static Model create(BailianModelSettings settings) {
        return switch (settings.provider()) {
            case DEEPSEEK -> createDeepSeek(settings);
            case BAILIAN -> createBailian(settings);
            case CHATGPT -> createChatGpt(settings);
            case OLLAMA -> createOllama(settings);
        };
    }

    private static Model createDeepSeek(BailianModelSettings settings) {
        requireApiKey(settings);
        return OpenAIChatModel.builder()
                .apiKey(settings.apiKey())
                .modelName(settings.modelName())
                .baseUrl(settings.baseUrl())
                .stream(settings.stream())
                .formatter(new DeepSeekFormatter())
                .build();
    }

    private static Model createBailian(BailianModelSettings settings) {
        requireApiKey(settings);
        var builder = DashScopeChatModel.builder()
                .apiKey(settings.apiKey())
                .modelName(settings.modelName())
                .stream(settings.stream())
                .enableThinking(settings.enableThinking())
                .formatter(new DashScopeChatFormatter());
        if (settings.baseUrl() != null && !settings.baseUrl().isBlank()) {
            builder.baseUrl(settings.baseUrl());
        }
        return builder.build();
    }

    private static Model createChatGpt(BailianModelSettings settings) {
        requireApiKey(settings);
        return OpenAIChatModel.builder()
                .apiKey(settings.apiKey())
                .modelName(settings.modelName())
                .baseUrl(settings.baseUrl())
                .stream(settings.stream())
                .formatter(new OpenAIChatFormatter())
                .build();
    }

    private static Model createOllama(BailianModelSettings settings) {
        return OllamaChatModel.builder()
                .modelName(settings.modelName())
                .baseUrl(settings.baseUrl())
                .formatter(new OllamaChatFormatter())
                .build();
    }

    private static void requireApiKey(BailianModelSettings settings) {
        if (!settings.hasUsableCredential()) {
            throw new IllegalStateException(
                    "Missing API key for %s. Please configure %s."
                            .formatted(settings.provider(), settings.requiredCredentialName()));
        }
    }
}
