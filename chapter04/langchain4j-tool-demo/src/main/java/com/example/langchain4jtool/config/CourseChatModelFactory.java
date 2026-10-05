package com.example.langchain4jtool.config;

import java.util.Map;
import java.time.Duration;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

/** 只负责创建真实客户端，构建本身不发送请求；必须由显式真实入口使用。 */
public final class CourseChatModelFactory {
    private CourseChatModelFactory() { }
    public static ChatModel create(Map<String,String> env) {
        CourseChatSettings settings=CourseChatSettings.from(env);
        return OpenAiChatModel.builder().baseUrl(settings.baseUrl()).apiKey(settings.keyForClient())
            .modelName(settings.model()).temperature(0.0).timeout(Duration.ofSeconds(30)).maxRetries(0).build();
    }
}
