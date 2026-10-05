package com.example.agentscopeframework.demo;

import com.example.agentscopeframework.config.CourseModelSettings;
import com.example.agentscopeframework.project.ServiceTicketProjectAgent;

/**
 * 项目实战入口。
 *
 * <p>保留独立 main 方法，是为了在课程中可以单独演示“客服工单 Agent”这一段，不必每次从框架功能导览开始。
 */
public class ServiceTicketProjectDemo {

    public static void main(String[] args) {
        CourseModelSettings settings = CourseModelSettings.fromEnvironment();
        if (!settings.hasUsableCredential()) {
            System.out.println("Missing model credential. Please read README.md to configure provider.");
            return;
        }
        var decision =
                new ServiceTicketProjectAgent(settings)
                        .handle(ServiceTicketProjectAgent.sampleTicket());
        AgentScopeFeatureTourDemo.printDecision(decision);
    }
}
