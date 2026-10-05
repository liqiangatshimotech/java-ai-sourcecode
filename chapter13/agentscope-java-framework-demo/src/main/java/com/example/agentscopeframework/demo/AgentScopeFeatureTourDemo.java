package com.example.agentscopeframework.demo;

import com.example.agentscopeframework.config.CourseModelSettings;
import com.example.agentscopeframework.project.ServiceTicket;
import com.example.agentscopeframework.project.ServiceTicketProjectAgent;
import com.example.agentscopeframework.project.TicketDecision;

/**
 * 框架功能导览 Demo。
 *
 * <p>这个 main 方法会先展示配置和项目工单，再根据环境变量决定是否真实调用大模型。没有配置 API Key 时，程序会走本地 fallback，
 * 这样第一次拉代码的人也能看懂业务流程。
 */
public class AgentScopeFeatureTourDemo {

    public static void main(String[] args) {
        CourseModelSettings settings = CourseModelSettings.fromEnvironment();
        ServiceTicket ticket = ServiceTicketProjectAgent.sampleTicket();

        System.out.println("AgentScope Java feature tour");
        System.out.println("Model settings: " + settings.summary());
        System.out.println("Sample ticket: " + ticket);

        TicketDecision decision;
        if (settings.hasUsableCredential()) {
            decision = new ServiceTicketProjectAgent(settings).handle(ticket);
        } else {
            System.out.println("No model credential found. Using local fallback decision.");
            decision = ServiceTicketProjectAgent.fallbackDecision(ticket);
        }

        printDecision(decision);
    }

    static void printDecision(TicketDecision decision) {
        System.out.println("\n=== Ticket Decision ===");
        System.out.println("category: " + decision.category);
        System.out.println("priority: " + decision.priority);
        System.out.println("summary: " + decision.summary);
        System.out.println("replyDraft: " + decision.replyDraft);
        System.out.println("needHumanReview: " + decision.needHumanReview);
        System.out.println("toolEvidence: " + decision.toolEvidence);
        System.out.println("nextActions: " + decision.nextActions);
    }
}
