package com.example.agentscope2.sdk.google;

/**
 * 业务层只关心“给一段输入，拿一段输出”。
 *
 * <p>实际项目可以继续扩展成支持流式响应、结构化输出、token 统计和 trace id 的接口；课程示例先把 SDK 接入边界讲清楚。
 */
public interface ChatClient {
    String complete(String userText) throws Exception;
}
