package com.example.agenticworkflow;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.UntypedAgent;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/** 官方 langchain4j-agentic：并行查事实，顺序交接，循环生成与检查。 */
public final class OfficialAgenticWorkflowDemo {
    public interface ReplyAgent {
        @UserMessage("""
                你是订单售后助手。订单={{order}}；物流={{logistics}}。
                用户问题={{message}}；本轮改进意见={{feedback}}。
                用中文给出回复，明确写出订单编号和“人工确认”。只提出补偿建议，不宣称已发放。
                """)
        @Agent(outputKey = "reply", description = "依据订单与物流事实生成售后答复")
        String reply(@V("order") String order, @V("logistics") String logistics,
                     @V("message") String message, @V("feedback") String feedback);
    }
    /** 演示数据仓库；生产实现必须先校验身份与订单归属，再访问业务数据库。 */
    public static final class OrderLookup {
        @Agent(outputKey = "order")
        public String lookup(@V("orderId") String orderId) {
            if (!"A100".equals(orderId)) throw new IllegalArgumentException("演示仓库无此订单");
            return "A100，已支付，金额 199 元";
        }
    }
    public static final class LogisticsLookup {
        @Agent(outputKey = "logistics")
        public String lookup(@V("orderId") String orderId) {
            if (!"A100".equals(orderId)) throw new IllegalArgumentException("演示仓库无此物流");
            return "A100，配送延误 2 天";
        }
    }
    /** 用确定性业务规则做最后检查，避免由模型自己批准付款或补偿。 */
    public static final class Review {
        @Agent(outputKey = "feedback")
        public String review(@V("reply") String reply) {
            return reply.contains("A100") && reply.contains("人工确认")
                    && !reply.contains("已发放") && !reply.contains("已到账")
                    ? "PASS" : "请补全订单 A100 与人工确认条件，不要承诺已发放或已到账。";
        }
    }
    /** Scope 是官方对象。Map 只作为初始输入，不再用自定义 Map 冒充 AgenticScope。 */
    public record Outcome(String reply, AgenticScope scope) {}
    public static Outcome run(ChatModel model, String orderId, String message, int maxIterations) {
        if (maxIterations < 1 || maxIterations > 5) throw new IllegalArgumentException("迭代上限必须为 1 到 5");
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            UntypedAgent facts = AgenticServices.parallelBuilder()
                    .subAgents(new OrderLookup(), new LogisticsLookup()).executor(executor)
                    .outputKey("factsReady").output(scope -> "READY").build();
            ReplyAgent writer = AgenticServices.agentBuilder(ReplyAgent.class).chatModel(model).build();
            UntypedAgent revision = AgenticServices.loopBuilder().subAgents(writer, new Review())
                    .maxIterations(maxIterations).testExitAtLoopEnd(true)
                    .exitCondition(scope -> "PASS".equals(scope.readState("feedback")))
                    .outputKey("reply").build();
            UntypedAgent workflow = AgenticServices.sequenceBuilder().subAgents(facts, revision)
                    .outputKey("reply").build();
            var result = workflow.invokeWithAgenticScope(Map.of("orderId", orderId,
                    "message", message, "feedback", "首次生成，请核对人工确认条件"));
            AgenticScope scope = result.agenticScope();
            // 达到上限只代表停止，绝不代表通过；未通过时失败关闭，不对外返回不合格回复。
            if (!"PASS".equals(scope.readState("feedback"))) {
                throw new IllegalStateException("质量检查未通过，已到迭代上限，转人工处理");
            }
            return new Outcome((String) scope.readState("reply"), scope);
        }
    }
    /** 第一次有意漏写条件，第二次修正，验证官方 Loop 确实执行了反馈闭环。 */
    public static final class OfflineModel implements ChatModel {
        public final AtomicInteger calls = new AtomicInteger();
        public ChatResponse doChat(ChatRequest request) {
            int turn = calls.incrementAndGet();
            return ChatResponse.builder().aiMessage(AiMessage.from(turn == 1
                    ? "A100 配送延误，建议评估补偿。"
                    : "A100 配送延误 2 天，建议评估补偿，发放前必须人工确认。")).build();
        }
    }
    public static void main(String[] args) {
        boolean live = "live".equalsIgnoreCase(System.getenv("APP_AI_MODE"));
        ChatModel model = live
                ? AgenticWorkflowDemo.LangChain4jModelFactory.create(AgenticWorkflowDemo.ModelSettings.fromEnvironment(System.getenv()))
                : new OfflineModel();
        System.out.println("mode=" + (live ? "live" : "offline（模拟模型，官方 Agentic 编排）"));
        Outcome outcome = run(model, "A100", "订单迟迟没有到，请帮我判断如何处理", 2);
        System.out.println(outcome.reply());
        System.out.println("官方 Scope 状态键：" + outcome.scope().state().keySet());
        System.out.println("官方调用记录数：" + outcome.scope().agentInvocations().size());
    }
}
