package com.example.agentscopeframework.demo;

import com.example.agentscopeframework.project.ServiceTicketProjectAgent;
import com.example.agentscopeframework.project.TicketDecision;

/**
 * 知识点 13.3：结构化输出。
 *
 * <p>AgentScope 支持把模型输出映射成 Java 类型。这个例子用本地 fallback 数据展示 TicketDecision 的业务结构，真实模型调用在
 * ServiceTicketProjectAgent.handle 中完成。
 */
public class StructuredOutputKnowledgeDemo {

    public static void main(String[] args) {
        TicketDecision decision =
                ServiceTicketProjectAgent.fallbackDecision(
                        ServiceTicketProjectAgent.sampleTicket());

        AgentScopeFeatureTourDemo.printDecision(decision);
    }
}
