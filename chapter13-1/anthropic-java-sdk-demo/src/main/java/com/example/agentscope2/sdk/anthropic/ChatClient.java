package com.example.agentscope2.sdk.anthropic;

/**
 * 业务侧的最小聊天接口。
 *
 * <p>这里先保持一个方法，便于看清 Anthropic SDK 与 OpenAI-compatible 调用的边界；真实服务可以继续扩展流式输出、
 * 结构化输出、审计字段和限流重试。
 */
public interface ChatClient {
    String complete(String userText) throws Exception;
}
