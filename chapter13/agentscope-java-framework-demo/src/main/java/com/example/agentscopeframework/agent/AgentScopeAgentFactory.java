package com.example.agentscopeframework.agent;

import com.example.agentscopeframework.config.CourseModelFactory;
import com.example.agentscopeframework.config.CourseModelSettings;
import com.example.agentscopeframework.tool.CustomerTicketTools;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.tool.Toolkit;

/**
 * 负责创建课程示例里的 AgentScope ReActAgent。
 *
 * <p>把 Agent 构造集中放在这里，是为了避免业务代码里到处散落模型、记忆、工具和 Prompt 配置。真实项目里可以继续拆成 Spring
 * Bean 或配置工厂。
 */
public final class AgentScopeAgentFactory {

    private AgentScopeAgentFactory() {}

    public static ReActAgent createCustomerServiceAgent(CourseModelSettings settings) {
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(new CustomerTicketTools());

        return ReActAgent.builder()
                .name("customer-service-agent")
                .sysPrompt(
                        """
                        你是一个电商客服工单处理 Agent。
                        你要先理解工单，再按需调用订单查询、政策检索和人工升级工具。
                        如果涉及退款、库存不足、投诉升级、金额争议或用户情绪强烈，必须建议人工复核。
                        输出内容要清晰、克制，不要编造工具没有返回的信息。
                        """)
                .model(CourseModelFactory.create(settings))
                .toolkit(toolkit)
                .memory(new InMemoryMemory())
                .build();
    }
}
