package com.example.agentscope2.sdk.google;

/**
 * Google Gen AI Java SDK 接入入口。
 */
public final class GoogleGenAiSdkDemo {
    private GoogleGenAiSdkDemo() {}

    public static void main(String[] args) throws Exception {
        GoogleGenAiSettings settings = GoogleGenAiSettings.fromEnvironment();
        String prompt = System.getenv().getOrDefault(
                "AI_PROMPT",
                "在 Java 项目里接入 Google Gen AI SDK 时，为什么要把 SDK Client 包在 adapter 后面？");

        System.out.println("Google GenAI demo settings: " + settings.summary());
        ChatClient chatClient = ChatClientFactory.create(settings);
        System.out.println(chatClient.complete(prompt));
    }
}
