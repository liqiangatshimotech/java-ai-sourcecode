package com.example.agentscopeframework.demo;

import com.example.agentscopeframework.project.ServiceTicket;
import com.example.agentscopeframework.project.ServiceTicketProjectAgent;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 知识点 13.4：记忆、会话和状态。
 *
 * <p>AgentScope 里 Memory 负责上下文，Session/State 负责保存和恢复运行状态。这个例子用一个可序列化快照模拟后端会怎么保存工单处理状态。
 */
public class MemorySessionStateKnowledgeDemo {

    public static void main(String[] args) {
        ServiceTicket ticket = ServiceTicketProjectAgent.sampleTicket();
        TicketSessionSnapshot snapshot =
                new TicketSessionSnapshot(
                        "session-" + ticket.ticketId(),
                        ticket.ticketId(),
                        new ArrayList<>(),
                        Instant.now());

        snapshot.timeline().add("收到用户工单：" + ticket.title());
        snapshot.timeline().add("准备查询订单状态和退款政策");
        snapshot.timeline().add("等待人工复核后恢复 Agent 状态");

        System.out.println(snapshot);
    }

    /**
     * 真实项目里可以把这个快照保存到 MySQL、Redis 或 AgentScope Session 后端。
     */
    public record TicketSessionSnapshot(
            String sessionId, String ticketId, List<String> timeline, Instant updatedAt) {}
}
