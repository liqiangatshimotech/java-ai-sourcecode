package com.example.agentscope2.demo;

import com.example.agentscope2.agent.AgentScope2AgentFactory;
import com.example.agentscope2.config.CourseModelFactory;
import com.example.agentscope2.config.CourseModelSettings;
import com.example.agentscope2.project.TicketDecision;
import com.example.agentscope2.tool.CustomerTicketTools;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.UserMessage;
import io.agentscope.core.model.Model;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.harness.agent.HarnessAgent;
import java.nio.file.Path;
import java.util.List;

/**
 * AgentScope Java 2.0 运行入口。
 *
 * <p>默认只构建模型和 Agent，避免没有 API Key 时直接失败。设置 `RUN_MODEL_CALL=true` 后才会真实调用模型。
 */
public class AgentScope2RuntimeDemo {

    public static void main(String[] args) {
        CourseModelSettings settings = CourseModelSettings.fromEnvironment();
        System.out.println("当前模型配置：" + settings.summary());
        System.out.println("模型标识：" + settings.modelId());

        if (!settings.hasUsableCredential()) {
            System.out.println("没有检测到模型凭证，只展示 2.0 配置解析结果。");
            return;
        }

        Model model = CourseModelFactory.create(settings);
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(new CustomerTicketTools());

        ReActAgent reactAgent = AgentScope2AgentFactory.createReActAgent(model, toolkit);
        HarnessAgent harnessAgent = AgentScope2AgentFactory.createHarnessAgent(
                model,
                Path.of(".agentscope", "workspace"),
                Path.of(".").toAbsolutePath().normalize());

        RuntimeContext context = RuntimeContext.builder()
                .userId("alice")
                .sessionId("ticket-demo")
                .put("businessLine", "customer-service")
                .build();

        System.out.println("已创建 ReActAgent：" + reactAgent.getName());
        System.out.println("已创建 HarnessAgent：" + harnessAgent.getName());
        System.out.println("RuntimeContext：userId=%s, sessionId=%s"
                .formatted(context.getUserId(), context.getSessionId()));

        if (Boolean.parseBoolean(System.getenv().getOrDefault("RUN_MODEL_CALL", "false"))) {
            callModel(reactAgent, context);
        } else {
            System.out.println("RUN_MODEL_CALL 未开启，本次不请求大模型。");
        }
    }

    private static void callModel(ReActAgent agent, RuntimeContext context) {
        String prompt = """
                请处理工单 T-1001。
                先查询工单摘要，再推荐处理团队，最后输出结构化处理建议。
                """;

        TicketDecision decision = agent.call(List.of(new UserMessage(prompt)), TicketDecision.class, context)
                .block()
                .getStructuredData(TicketDecision.class);

        System.out.println("结构化处理建议：" + decision);
    }
}
