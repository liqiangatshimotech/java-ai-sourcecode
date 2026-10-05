package com.example.agentscope2.sdk.openai;

/**
 * OpenAI Java SDK 接入入口。
 *
 * <p>运行时可以通过 `AI_PROMPT` 改问题；如果没有设置，就用一个和 AgentScope 生产化接入相关的默认问题。
 */
public final class OpenAiJavaSdkDemo {
    private OpenAiJavaSdkDemo() {}

    public static void main(String[] args) {
        OpenAiSdkSettings settings = OpenAiSdkSettings.fromEnvironment();
        String prompt = System.getenv().getOrDefault(
                "AI_PROMPT",
                "在 Java 项目里接入 OpenAI-compatible 模型时，配置层最容易踩的坑是什么？");

        System.out.println("OpenAI SDK settings: " + settings.summary());
        OpenAiChatClient chatClient = new OpenAiChatClient(settings);
        System.out.println(chatClient.complete(prompt));
    }
}
