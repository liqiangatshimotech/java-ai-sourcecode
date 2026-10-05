package com.example.agentscope2.bailian.demo;

import com.example.agentscope2.bailian.config.BailianModelFactory;
import com.example.agentscope2.bailian.config.BailianModelSettings;
import com.example.agentscope2.bailian.project.BailianRoutingAdvice;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.UserMessage;
import io.agentscope.core.model.Model;
import java.util.List;

/**
 * AgentScope Java 2.0 接入阿里云百炼模型的最小入口。
 *
 * <p>默认只构建模型和 Agent，不请求真实模型。设置 `RUN_MODEL_CALL=true` 后才会调用模型，
 * 这样没有 API Key 的环境也能运行配置解析和单元测试。
 */
public final class BailianDashScopeModelDemo {
    private BailianDashScopeModelDemo() {}

    public static void main(String[] args) {
        BailianModelSettings settings = BailianModelSettings.fromEnvironment();
        System.out.println("当前模型配置：" + settings.summary());
        System.out.println("AgentScope 模型标识：" + settings.modelId());

        if (!settings.hasUsableCredential()) {
            System.out.println("没有检测到模型凭证，只展示配置解析结果。需要真实调用时请配置 "
                    + settings.requiredCredentialName());
            return;
        }

        Model model = BailianModelFactory.create(settings);
        ReActAgent agent = ReActAgent.builder()
                .name("bailian-course-agent")
                .sysPrompt("你是一个 Java AI 应用架构助手，回答要具体、克制，并优先给出可执行建议。")
                .model(model)
                .maxIters(3)
                .maxRetries(2)
                .build();

        System.out.println("已创建 ReActAgent：" + agent.getName());
        if (Boolean.parseBoolean(System.getenv().getOrDefault("RUN_MODEL_CALL", "false"))) {
            callModel(agent, settings);
        } else {
            System.out.println("RUN_MODEL_CALL 未开启，本次不请求大模型。");
        }
    }

    private static void callModel(ReActAgent agent, BailianModelSettings settings) {
        String prompt = """
                请判断这个模型接入适合放在哪类 Java AI 应用里：
                provider=%s
                model=%s
                请输出 provider、model、recommendedScenario、risk、nextAction 五个字段。
                """.formatted(settings.provider(), settings.modelName());

        RuntimeContext context = RuntimeContext.builder()
                .userId("course-demo")
                .sessionId("bailian-model-demo")
                .put("provider", settings.provider().name())
                .build();

        BailianRoutingAdvice advice = agent.call(List.of(new UserMessage(prompt)), BailianRoutingAdvice.class, context)
                .block()
                .getStructuredData(BailianRoutingAdvice.class);
        System.out.println("结构化建议：" + advice);
    }
}
