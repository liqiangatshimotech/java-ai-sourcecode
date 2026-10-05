package com.example.agentscopeframework.project;

import com.example.agentscopeframework.agent.AgentScopeAgentFactory;
import com.example.agentscopeframework.config.CourseModelSettings;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import java.util.List;

/**
 * 客服工单项目实战的应用服务。
 *
 * <p>这个类演示后端项目里常见的分层方式：Controller 收到请求后，不直接操作 AgentScope 细节，而是调用应用服务。应用服务负责
 * 把业务对象转换成 Prompt、调用 Agent、解析结构化结果。
 */
public class ServiceTicketProjectAgent {

    private final ReActAgent agent;

    public ServiceTicketProjectAgent(CourseModelSettings settings) {
        this.agent = AgentScopeAgentFactory.createCustomerServiceAgent(settings);
    }

    public TicketDecision handle(ServiceTicket ticket) {
        Msg request = buildRequest(ticket);
        Msg response = agent.call(request, TicketDecision.class).block();
        if (response == null) {
            throw new IllegalStateException("Agent returned no response");
        }
        return response.getStructuredData(TicketDecision.class);
    }

    static Msg buildRequest(ServiceTicket ticket) {
        return Msg.builder()
                .role(MsgRole.USER)
                .content(TextBlock.builder().text(buildPrompt(ticket)).build())
                .build();
    }

    static String buildPrompt(ServiceTicket ticket) {
        return """
                请处理下面这张客服工单，并返回结构化结果。

                工单 ID：%s
                用户等级：%s
                来源渠道：%s
                当前优先级：%s
                标题：%s
                用户描述：%s

                处理要求：
                1. 如果描述里出现订单号，请优先调用 query_order_status。
                2. 如果涉及退款、物流、发票政策，请调用 search_policy。
                3. 如果需要人工介入，请调用 create_escalation。
                4. 最终结果必须能映射成 TicketDecision。
                """
                .formatted(
                        ticket.ticketId(),
                        ticket.userLevel(),
                        ticket.channel(),
                        ticket.priority(),
                        ticket.title(),
                        ticket.description());
    }

    public static ServiceTicket sampleTicket() {
        return new ServiceTicket(
                "TICKET-20260611-001",
                "VIP",
                "online-chat",
                "用户催促缺货订单并要求退款",
                "我买的 ORDER-2002 一直不发货。如果今天不能发，请帮我退款并给一个明确说明。",
                TicketPriority.HIGH);
    }

    public static TicketDecision fallbackDecision(ServiceTicket ticket) {
        return new TicketDecision(
                "订单履约/退款",
                ticket.priority(),
                "用户反馈订单可能因库存不足未发货，并提出退款诉求。",
                "我先帮你核实订单状态。如果订单仍未出库且符合退款规则，会协助提交退款或转人工处理。",
                true,
                List.of("需要查询订单状态", "需要核对退款政策", "涉及 VIP 用户体验"),
                List.of("查询订单 ORDER-2002", "检索退款政策", "创建人工复核任务"));
    }
}
