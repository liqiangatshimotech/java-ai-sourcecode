package com.example.springaitool.config;

import java.util.Map;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;

/** 三家回答模型经兼容接口创建；业务检索或工具回调不随供应商变化。 */
public final class CourseChatModelFactory {
    private CourseChatModelFactory() { }
    public static ChatModel create(Map<String,String> env) {
        CourseChatSettings settings=CourseChatSettings.from(env);
        // Spring AI OpenAiApi 自己添加 /v1/chat/completions，base 去掉末尾 /v1 避免重复。
        String apiBase=settings.baseUrl().substring(0,settings.baseUrl().length()-3);
        return OpenAiChatModel.builder()
            .openAiApi(OpenAiApi.builder().baseUrl(apiBase).apiKey(settings.keyForClient()).build())
            .defaultOptions(OpenAiChatOptions.builder().model(settings.model()).temperature(0.0).maxTokens(2048).build())
            .build();
    }
}
