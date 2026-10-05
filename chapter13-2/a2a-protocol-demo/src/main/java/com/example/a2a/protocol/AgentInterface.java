package com.example.a2a.protocol;

/**
 * Agent Card 中声明的协议入口。
 */
public record AgentInterface(
        String url,
        String protocolBinding,
        String protocolVersion,
        String tenant) {}
