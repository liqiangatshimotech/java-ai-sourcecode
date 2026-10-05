package com.example.a2a.server;

import com.example.a2a.protocol.AgentCapabilities;
import com.example.a2a.protocol.AgentCard;
import com.example.a2a.protocol.AgentInterface;
import com.example.a2a.protocol.AgentProvider;
import com.example.a2a.protocol.AgentSkill;
import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * 构造客服 Agent 的 Agent Card。
 */
public final class CustomerSupportAgentCards {

    private CustomerSupportAgentCards() {}

    public static AgentCard create(URI baseUri) {
        String endpoint = baseUri.resolve("/").toString();
        return new AgentCard(
                "customer-support-a2a-agent",
                "处理客服工单分诊、处理团队推荐和下一步动作建议的 A2A Agent。",
                new AgentProvider("Java AI Course", "https://e1cq4reqnuu.feishu.cn"),
                "2026.07.08",
                "1.0",
                endpoint,
                new AgentCapabilities(
                        true,
                        false,
                        true,
                        false),
                List.of("text/plain", "application/json"),
                List.of("text/plain", "application/json"),
                List.of(new AgentSkill(
                        "customer-ticket-triage",
                        "客服工单分诊",
                        "根据工单内容判断优先级、推荐处理团队，并返回下一步动作。",
                        List.of("text/plain"),
                        List.of("application/json", "text/plain"),
                        List.of(
                                "请处理工单 T-1001",
                                "客户反馈 DeepSeek 网关 504，帮我判断处理团队"))),
                List.of(new AgentInterface(
                        endpoint,
                        "https://a2a-protocol.org/bindings/http+json/1.0",
                        "1.0",
                        null)),
                Map.of(
                        "bearerAuth",
                        Map.of(
                                "type", "http",
                                "scheme", "bearer",
                                "description", "示例服务接受任意非空 Bearer Token，生产环境必须接入真实鉴权。")),
                List.of(Map.of("bearerAuth", List.of())));
    }
}
