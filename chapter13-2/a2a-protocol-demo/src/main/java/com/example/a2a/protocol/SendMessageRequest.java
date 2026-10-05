package com.example.a2a.protocol;

import java.util.Map;

/**
 * message:send 请求体。
 */
public record SendMessageRequest(
        String tenant,
        Message message,
        SendMessageConfiguration configuration,
        Map<String, Object> metadata) {}
