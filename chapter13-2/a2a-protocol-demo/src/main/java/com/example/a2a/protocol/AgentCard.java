package com.example.a2a.protocol;

import java.util.List;
import java.util.Map;

/**
 * A2A Agent 的自描述文件。
 *
 * <p>客户端先拿 Agent Card，再决定这个远程 Agent 是否适合处理当前任务、该走哪个 URL、
 * 支持哪些输入输出模式，以及需要什么认证方式。
 */
public record AgentCard(
        String name,
        String description,
        AgentProvider provider,
        String version,
        String protocolVersion,
        String url,
        AgentCapabilities capabilities,
        List<String> defaultInputModes,
        List<String> defaultOutputModes,
        List<AgentSkill> skills,
        List<AgentInterface> supportedInterfaces,
        Map<String, Object> securitySchemes,
        List<Map<String, List<String>>> security) {}
