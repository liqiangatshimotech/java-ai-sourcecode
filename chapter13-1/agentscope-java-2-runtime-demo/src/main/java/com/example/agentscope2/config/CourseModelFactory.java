package com.example.agentscope2.config;

import io.agentscope.core.model.Model;
import io.agentscope.extensions.model.ollama.OllamaChatModel;
import io.agentscope.extensions.model.ollama.formatter.OllamaChatFormatter;
import io.agentscope.extensions.model.openai.OpenAIChatModel;
import io.agentscope.extensions.model.openai.formatter.DeepSeekFormatter;
import io.agentscope.extensions.model.openai.formatter.OpenAIChatFormatter;

/**
 * 把课程自己的配置对象转换成 AgentScope Java 2.0 的 Model。
 *
 * <p>2.0 RC5 起，OpenAI/Ollama 模型 provider 从 core 拆到扩展模块，所以这里的 import 路径是
 * `io.agentscope.extensions.model.*`，不是 1.x 常见的 core model 路径。
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
                .formatter(new OllamaChatFormatter())
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
