package com.example.agentscopeframework.demo;

import com.example.agentscopeframework.project.ServiceTicket;
import com.example.agentscopeframework.project.ServiceTicketProjectAgent;
import java.util.List;

/**
 * 知识点 13.6：多智能体协作。
 *
 * <p>AgentScope 提供 Pipeline、MsgHub、Agent as Tool 和 A2A。这个例子用普通 Java 模拟 Supervisor 先分类，再分派专家，再汇总结果的结构。
 */
public class MultiAgentKnowledgeDemo {

    public static void main(String[] args) {
        ServiceTicket ticket = ServiceTicketProjectAgent.sampleTicket();
        var classifier = new ClassifierAgent();
        var policy = new PolicyAgent();
        var reply = new ReplyAgent();

        String category = classifier.classify(ticket);
        String policyAdvice = policy.check(category);
        String replyDraft = reply.compose(ticket, policyAdvice);

        System.out.println(new MultiAgentResult(category, List.of(policyAdvice), replyDraft));
    }

    static class ClassifierAgent {
        String classify(ServiceTicket ticket) {
            return ticket.description().contains("退款") ? "退款/履约" : "普通咨询";
        }
    }

    static class PolicyAgent {
        String check(String category) {
            return "退款/履约".equals(category) ? "需要同时核对订单状态和退款政策" : "按普通 FAQ 回复";
        }
    }

    static class ReplyAgent {
        String compose(ServiceTicket ticket, String advice) {
            return "工单 " + ticket.ticketId() + " 的处理建议：" + advice;
        }
    }

    public record MultiAgentResult(String category, List<String> expertAdvice, String replyDraft) {}
}
