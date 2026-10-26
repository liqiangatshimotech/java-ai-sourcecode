package com.example.agentruntime.planner;

import com.example.agentruntime.domain.AgentState;
import com.example.agentruntime.tool.ToolCall;
import java.util.Map;
/** 使用文章中的 facts 协议推进，离线也能观察完整链路。 */
public final class RulePlanner implements AgentPlanner {
    public AgentDecision next(AgentState state) {
        Map<String, String> args = Map.of("orderNo", state.orderNo());
        if (!state.hasFact("orderStatus")) return AgentDecision.callTool("先查询订单基础状态", new ToolCall("queryOrder", args));
        if (!state.hasFact("logisticsDelayHours")) return AgentDecision.callTool("继续查询物流轨迹", new ToolCall("queryLogistics", args));
        if (!state.hasFact("compensationDecision")) return AgentDecision.callTool("开始套补偿规则", new ToolCall("evaluateCompensation", args));
        if (!state.hasFact("customerReply")) return AgentDecision.callTool("生成用户可读的处理口径", new ToolCall("draftCustomerReply", args));
        if ("ELIGIBLE".equals(state.fact("compensationDecision")) && !state.hasFact("ticketNo") && state.pendingApproval().isEmpty()) {
            return AgentDecision.callTool("创建补偿工单需要权限守卫", new ToolCall("createCompensationTicket", args));
        }
        // 已确认后不再输出过期的“等待确认”，也不能把创建工单说成已发款。
        return AgentDecision.finish(state.hasFact("ticketNo")
                ? "已创建补偿工单 " + state.fact("ticketNo") + "，补偿金额候选为 " + state.fact("compensationAmount") + " 元；尚未发放补偿。"
                : state.fact("customerReply"));
    }
}
