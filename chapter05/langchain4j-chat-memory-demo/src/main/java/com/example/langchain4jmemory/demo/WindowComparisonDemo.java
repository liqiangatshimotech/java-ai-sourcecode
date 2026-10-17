package com.example.langchain4jmemory.demo;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.openai.OpenAiTokenCountEstimator;

/** 离线比较真实框架窗口；token 使用 GPT tokenizer 估算，不代表其他模型实际账单。 */
public final class WindowComparisonDemo {
    public static void main(String[] args) {
        var estimator = new OpenAiTokenCountEstimator("gpt-4o-mini");
        var messages = MessageWindowChatMemory.withMaxMessages(3);
        var tokens = TokenWindowChatMemory.builder().id("tokens").maxTokens(100,estimator).build();
        for (String text : java.util.List.of("短消息","很长的消息".repeat(80),"最新问题")) {
            messages.add(UserMessage.from(text));
            tokens.add(UserMessage.from(text));
        }
        System.out.println("按条数保留："+messages.messages().size());
        System.out.println("按 token 保留："+tokens.messages().size());
        System.out.println("估算 token："+estimator.estimateTokenCountInMessages(tokens.messages()));
    }
}
