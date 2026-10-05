package com.example.agentscopeframework.demo;

import java.util.List;
import java.util.Map;

/**
 * 知识点 13.5：RAG 和 MCP。
 *
 * <p>RAG 解决“查知识”，MCP 解决“接外部工具”。这个例子用接口模拟两种能力，方便后续替换成 AgentScope RAG 扩展或真实 MCP Server。
 */
public class RagMcpKnowledgeDemo {

    public static void main(String[] args) {
        PolicyKnowledgeBase rag = new InMemoryPolicyKnowledgeBase();
        McpToolCatalog mcp = new McpToolCatalog(List.of("order.query", "refund.create", "crm.note"));

        System.out.println("RAG 命中：" + rag.retrieve("refund"));
        System.out.println("MCP 可用工具：" + mcp.enabledTools());
    }

    public interface PolicyKnowledgeBase {
        String retrieve(String topic);
    }

    public static class InMemoryPolicyKnowledgeBase implements PolicyKnowledgeBase {
        private final Map<String, String> docs =
                Map.of("refund", "未发货订单可以自动退款；已发货订单需要退货入库后退款。");

        @Override
        public String retrieve(String topic) {
            return docs.getOrDefault(topic, "没有检索到相关政策");
        }
    }

    public record McpToolCatalog(List<String> enabledTools) {}
}
