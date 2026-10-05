package com.example.springaitool.config;

import java.util.Map;
import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Spring 容器与命令行使用同一开关；缺少真实模型配置时不偷偷回退成成功回答。 */
@Configuration
public class CourseChatModelConfiguration {
    @Bean public ChatClient.Builder chatClientBuilder() { return createBuilder(System.getenv()); }

    public static ChatClient.Builder createBuilder(Map<String,String> env) {
        if (Boolean.parseBoolean(env.getOrDefault("RUN_MODEL_CALL","false")))
            return ChatClient.builder(CourseChatModelFactory.create(env));
        // 离线模型清楚说明没有推理、也没有自动调用工具，防止被误认为真实Tool Calling。
        ChatModel offline = prompt -> new ChatResponse(List.of(new Generation(new AssistantMessage(
            "OFFLINE：未调用模型；请运行工具单元测试，或显式设置 RUN_MODEL_CALL=true 验证真实 Tool Calling。"))));
        return ChatClient.builder(offline);
    }
}
