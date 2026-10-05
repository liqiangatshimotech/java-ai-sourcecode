package com.example.agentscope2.sdk.anthropic;

/**
 * Anthropic Java SDK 接入入口。
 */
public final class AnthropicJavaSdkDemo {
    private AnthropicJavaSdkDemo() {}

    public static void main(String[] args) throws Exception {
        AnthropicSdkSettings settings = AnthropicSdkSettings.fromEnvironment();
        String prompt = System.getenv().getOrDefault(
                "AI_PROMPT",
                "在 Java 项目里接入 Anthropic SDK 时，为什么不要把 SDK 类型传到业务层？");

        System.out.println("Anthropic SDK demo settings: " + settings.summary());
        ChatClient chatClient = ChatClientFactory.create(settings);
        System.out.println(chatClient.complete(prompt));
    }
}
