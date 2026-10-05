package com.example.agentscope2.tool;

import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import java.util.Map;

/**
 * 给 Agent 调用的业务工具示例。
 *
 * <p>真实项目里这些方法通常会访问工单系统、CRM、知识库或风控系统。示例里用内存数据模拟，便于直接运行测试。
 */
public class CustomerTicketTools {

    private final Map<String, String> ticketSummaries = Map.of(
            "T-1001", "企业版客户反馈 DeepSeek 网关偶发 504，影响在线客服摘要生成。",
            "T-1002", "普通客户咨询发票抬头修改，未影响核心业务。");

    @Tool(
            name = "query_ticket_summary",
            description = "根据工单号查询工单摘要，适合在判断优先级前调用。",
            readOnly = true)
    public String queryTicketSummary(
            @ToolParam(name = "ticketId", description = "工单编号，例如 T-1001") String ticketId) {
        return ticketSummaries.getOrDefault(ticketId, "没有找到工单：" + ticketId);
    }

    @Tool(
            name = "suggest_ticket_owner",
            description = "根据工单摘要推荐处理团队，返回团队名。",
            readOnly = true)
    public String suggestTicketOwner(
            @ToolParam(name = "summary", description = "工单摘要或客户问题描述") String summary) {
        String normalized = summary == null ? "" : summary.toLowerCase();
        if (normalized.contains("网关") || normalized.contains("504") || normalized.contains("api")) {
            return "平台稳定性小组";
        }
        if (normalized.contains("发票") || normalized.contains("合同")) {
            return "商务支持小组";
        }
        return "一线客服小组";
    }
}
