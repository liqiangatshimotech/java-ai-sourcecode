package com.example.agentscopeframework.config;

import io.agentscope.core.formatter.openai.DeepSeekFormatter;
import io.agentscope.core.formatter.openai.OpenAIChatFormatter;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.OllamaChatModel;
import io.agentscope.core.model.OpenAIChatModel;

/**
 * 把课程统一模型配置转换成 AgentScope Java 的 Model。
 *
 * <p>AgentScope Java 的 `OpenAIChatModel` 支持 OpenAI 兼容 API，所以 DeepSeek 也走这一类模型；区别在于 DeepSeek
 * 需要自己的 Formatter 来处理消息格式差异。Ollama 则使用独立的 `OllamaChatModel`。
 */
public final class CourseModelFactory {

    private CourseModelFactory() {}

    public static Model create(CourseModelSettings settings) {
        return switch (settings.provider()) {
            case DEEPSEEK -> createDeepSeek(settings);
            case CHATGPT -> createChatGpt(settings);
            case OLLAMA -> createOllama(settings);
        };
    }

    private static Model createDeepSeek(CourseModelSettings settings) {
        requireApiKey(settings);
        return OpenAIChatModel.builder()
                .apiKey(settings.apiKey())
                .modelName(settings.modelName())
                .baseUrl(settings.baseUrl())
                .stream(settings.stream())
                .formatter(new DeepSeekFormatter())
                .build();
    }

    private static Model createChatGpt(CourseModelSettings settings) {
        requireApiKey(settings);
        return OpenAIChatModel.builder()
                .apiKey(settings.apiKey())
                .modelName(settings.modelName())
                .baseUrl(settings.baseUrl())
                .stream(settings.stream())
                .formatter(new OpenAIChatFormatter())
                .build();
    }

    private static Model createOllama(CourseModelSettings settings) {
        return OllamaChatModel.builder()
                .modelName(settings.modelName())
                .baseUrl(settings.baseUrl())
                .build();
    }

    private static void requireApiKey(CourseModelSettings settings) {
        if (!settings.hasUsableCredential()) {
            throw new IllegalStateException(
                    "Missing API key for %s. Please configure the matching environment variable."
                            .formatted(settings.provider()));
        }
    }
}
