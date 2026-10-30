package com.example.agenticworkflow;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * 纯 Java 对照入口：8.4 正文对应 {@link OfficialAgenticWorkflowDemo}。
 * 提示词在 {@link OfficialAgenticWorkflowDemo.ReplyAgent#reply(String, String, String, String)} 上；
 * 本文件使用模板拼接，不经过 AI Services 代理，因此给这里的接口加提示词注解不会生效。
 *
 * 用订单售后业务演示 Agentic Workflow 的三种基本编排：
 * 1. 顺序：意图识别完成后，才进入事实查询和补偿判断；
 * 2. 并行：订单状态与物流状态只依赖 orderId，可以同时查询；
 * 3. 循环：回复生成后接受质量检查，未达标时带着修改意见再生成。
 *
 * <p>这个项目使用 LangChain4j 的 {@link Tool} 和模型接口，但工作流编排保持为纯 Java，
 * 这样不依赖实验性 Agentic API 也能离线运行。文档会明确标注这条边界。</p>
 */
public final class AgenticWorkflowDemo {
    private AgenticWorkflowDemo() {
    }

    public static void main(String[] args) {
        AfterSalesTools tools = new AfterSalesTools();
        try (var executor = Executors.newFixedThreadPool(2)) {
            Workflow workflow = new Workflow(
                    new RuleIntentAgent(),
                    new ToolOrderAgent(tools),
                    new ToolLogisticsAgent(tools),
                    new RuleCompensationAgent(),
                    new TemplateReplyAgent(),
                    new RuleReplyQualityAgent(),
                    executor,
                    2
            );

            WorkflowResult result = workflow.run(
                    new Request("A100", "订单一直没有送到，请帮我判断是否需要补偿")
            );
            result.invocations().forEach(System.out::println);
            System.out.println("Scope: " + result.scope());
            System.out.println("最终回复：" + result.reply());
        }
    }

    public record Request(String orderId, String message) {
    }

    public record Intent(String name, double confidence) {
    }

    public record CompensationDecision(boolean eligible, String reason) {
    }

    public record QualityResult(boolean passed, double score, String instruction) {
    }

    public record AgentInvocation(
            String agentName,
            List<String> inputKeys,
            String outputKey,
            int iteration,
            String status,
            long durationMillis
    ) {
    }

    public record WorkflowResult(
            String reply,
            Map<String, Object> scope,
            List<AgentInvocation> invocations
    ) {
    }

    /**
     * Scope 是 Agent 之间的数据协议，不是可以随意读写的全局变量。
     * putOnce 用于事实和决策，禁止两个并行分支覆盖同一个 key；
     * replace 用于循环里的当前回复和质量控制变量。
     */
    public static final class PlainJavaScope {
        private final Map<String, Object> values = new LinkedHashMap<>();
        private final List<AgentInvocation> invocations = new ArrayList<>();

        public synchronized void putOnce(String key, Object value) {
            if (values.containsKey(key)) {
                throw new IllegalStateException("Scope key 已经存在，禁止覆盖：" + key);
            }
            values.put(key, value);
        }

        public synchronized void replace(String key, Object value) {
            values.put(key, value);
        }

        public synchronized <T> T require(String key, Class<T> type) {
            Object value = values.get(key);
            if (!type.isInstance(value)) {
                throw new IllegalStateException("Scope 缺少变量或类型不匹配：" + key);
            }
            return type.cast(value);
        }

        public synchronized void record(AgentInvocation invocation) {
            invocations.add(invocation);
        }

        public synchronized Map<String, Object> snapshot() {
            return Map.copyOf(values);
        }

        public synchronized List<AgentInvocation> invocationSnapshot() {
            return List.copyOf(invocations);
        }
    }

    public interface IntentAgent {
        Intent classify(Request request);
    }

    public interface OrderAgent {
        String query(String orderId);
    }

    public interface LogisticsAgent {
        String query(String orderId);
    }

    public interface CompensationAgent {
        CompensationDecision decide(String orderStatus, String logisticsStatus);
    }

    /** 纯 Java 模板契约；实现见 TemplateReplyAgent。模型提示词见官方入口的 ReplyAgent。 */
    public interface PlainJavaReplyAgent {
        String compose(Request request, CompensationDecision decision, String instruction);
    }

    public interface PlainJavaReplyQualityAgent {
        QualityResult review(String reply);
    }

    /** 工作流只负责数据依赖与控制流，不在这里实现订单和物流业务规则。 */
    public static final class Workflow {
        private final IntentAgent intentAgent;
        private final OrderAgent orderAgent;
        private final LogisticsAgent logisticsAgent;
        private final CompensationAgent compensationAgent;
        private final PlainJavaReplyAgent replyAgent;
        private final PlainJavaReplyQualityAgent qualityAgent;
        private final Executor parallelExecutor;
        private final int maxReplyIterations;

        public Workflow(
                IntentAgent intentAgent,
                OrderAgent orderAgent,
                LogisticsAgent logisticsAgent,
                CompensationAgent compensationAgent,
                PlainJavaReplyAgent replyAgent,
                PlainJavaReplyQualityAgent qualityAgent,
                Executor parallelExecutor,
                int maxReplyIterations
        ) {
            if (maxReplyIterations < 1) {
                throw new IllegalArgumentException("maxReplyIterations 必须大于 0");
            }
            this.intentAgent = intentAgent;
            this.orderAgent = orderAgent;
            this.logisticsAgent = logisticsAgent;
            this.compensationAgent = compensationAgent;
            this.replyAgent = replyAgent;
            this.qualityAgent = qualityAgent;
            this.parallelExecutor = parallelExecutor;
            this.maxReplyIterations = maxReplyIterations;
        }

        public WorkflowResult run(Request request) {
            PlainJavaScope scope = new PlainJavaScope();
            scope.putOnce("input.request", request);

            Intent intent = invoke(
                    scope, "IntentAgent", List.of("input.request"), "decision.intent", 1,
                    () -> intentAgent.classify(request)
            );
            scope.putOnce("decision.intent", intent);
            if (intent.confidence() < 0.7) {
                String reply = "问题还需要进一步确认，已为你转接人工服务。";
                scope.putOnce("output.customerReply", reply);
                return result(scope, reply);
            }

            // 两个查询都只读取 orderId，没有互相依赖，因此使用有界线程池并行执行。
            CompletableFuture<String> orderFuture = CompletableFuture.supplyAsync(
                    () -> invoke(scope, "OrderAgent", List.of("input.request"), "fact.orderStatus", 1,
                            () -> orderAgent.query(request.orderId())),
                    parallelExecutor
            );
            CompletableFuture<String> logisticsFuture = CompletableFuture.supplyAsync(
                    () -> invoke(scope, "LogisticsAgent", List.of("input.request"), "fact.logisticsStatus", 1,
                            () -> logisticsAgent.query(request.orderId())),
                    parallelExecutor
            );
            String orderStatus = orderFuture.join();
            String logisticsStatus = logisticsFuture.join();
            scope.putOnce("fact.orderStatus", orderStatus);
            scope.putOnce("fact.logisticsStatus", logisticsStatus);

            CompensationDecision decision = invoke(
                    scope,
                    "CompensationAgent",
                    List.of("fact.orderStatus", "fact.logisticsStatus"),
                    "decision.compensation",
                    1,
                    () -> compensationAgent.decide(orderStatus, logisticsStatus)
            );
            scope.putOnce("decision.compensation", decision);

            String instruction = "";
            String reply = "";
            for (int iteration = 1; iteration <= maxReplyIterations; iteration++) {
                String currentInstruction = instruction;
                reply = invoke(
                        scope,
                        "PlainJavaReplyAgent",
                        List.of("input.request", "decision.compensation", "control.replyInstruction"),
                        "output.customerReply",
                        iteration,
                        () -> replyAgent.compose(request, decision, currentInstruction)
                );
                scope.replace("output.customerReply", reply);

                String reviewedReply = reply;
                QualityResult quality = invoke(
                        scope,
                        "PlainJavaReplyQualityAgent",
                        List.of("output.customerReply"),
                        "metric.replyQuality",
                        iteration,
                        () -> qualityAgent.review(reviewedReply)
                );
                scope.replace("metric.replyQuality", quality);
                if (quality.passed()) {
                    break;
                }
                instruction = quality.instruction();
                scope.replace("control.replyInstruction", instruction);
            }
            return result(scope, reply);
        }

        private WorkflowResult result(PlainJavaScope scope, String reply) {
            return new WorkflowResult(reply, scope.snapshot(), scope.invocationSnapshot());
        }

        private <T> T invoke(
                PlainJavaScope scope,
                String agentName,
                List<String> inputKeys,
                String outputKey,
                int iteration,
                Operation<T> operation
        ) {
            Instant startedAt = Instant.now();
            try {
                T result = operation.run();
                scope.record(new AgentInvocation(
                        agentName,
                        inputKeys,
                        outputKey,
                        iteration,
                        "SUCCEEDED",
                        Duration.between(startedAt, Instant.now()).toMillis()
                ));
                return result;
            } catch (RuntimeException exception) {
                scope.record(new AgentInvocation(
                        agentName,
                        inputKeys,
                        outputKey,
                        iteration,
                        "FAILED",
                        Duration.between(startedAt, Instant.now()).toMillis()
                ));
                throw exception;
            }
        }
    }

    @FunctionalInterface
    private interface Operation<T> {
        T run();
    }

    /** LangChain4j 的 @Tool 把普通 Java 方法声明为可被模型调用的业务能力。 */
    public static final class AfterSalesTools {
        @Tool("按订单号查询订单状态和承诺送达时间")
        public String queryOrder(String orderId) {
            return switch (orderId) {
                case "A100" -> "status=SHIPPED,promisedHours=24,elapsedHours=42";
                case "A200" -> "status=DELIVERED,promisedHours=24,elapsedHours=20";
                default -> "NOT_FOUND";
            };
        }

        @Tool("按订单号查询最新物流节点")
        public String queryLogistics(String orderId) {
            return switch (orderId) {
                case "A100" -> "node=杭州分拨中心,hoursWithoutUpdate=18";
                case "A200" -> "node=已签收,hoursWithoutUpdate=0";
                default -> "NOT_FOUND";
            };
        }
    }

    public static final class RuleIntentAgent implements IntentAgent {
        @Override
        public Intent classify(Request request) {
            boolean afterSales = request.message().contains("补偿") || request.message().contains("没送到");
            return afterSales ? new Intent("DELIVERY_DELAY", 0.98) : new Intent("UNKNOWN", 0.45);
        }
    }

    public record ToolOrderAgent(AfterSalesTools tools) implements OrderAgent {
        @Override
        public String query(String orderId) {
            return tools.queryOrder(orderId);
        }
    }

    public record ToolLogisticsAgent(AfterSalesTools tools) implements LogisticsAgent {
        @Override
        public String query(String orderId) {
            return tools.queryLogistics(orderId);
        }
    }

    /** 金额与补偿资格属于确定性业务规则，不交给概率模型自由判断。 */
    public static final class RuleCompensationAgent implements CompensationAgent {
        @Override
        public CompensationDecision decide(String orderStatus, String logisticsStatus) {
            boolean delayed = orderStatus.contains("elapsedHours=42")
                    && logisticsStatus.contains("hoursWithoutUpdate=18");
            return delayed
                    ? new CompensationDecision(true, "已超过承诺时间且物流 18 小时未更新")
                    : new CompensationDecision(false, "未满足物流延误补偿条件");
        }
    }

    public static final class TemplateReplyAgent implements PlainJavaReplyAgent {
        @Override
        public String compose(Request request, CompensationDecision decision, String instruction) {
            String base = decision.eligible()
                    ? "订单 " + request.orderId() + " 已满足延误补偿条件，原因：" + decision.reason() + "。"
                    : "订单 " + request.orderId() + " 暂不满足补偿条件，原因：" + decision.reason() + "。";
            // 第一轮故意缺少“人工确认”提示，让质量 Agent 触发第二轮修改。
            return instruction.isBlank() ? base : base + " " + instruction;
        }
    }

    public static final class RuleReplyQualityAgent implements PlainJavaReplyQualityAgent {
        @Override
        public QualityResult review(String reply) {
            boolean passed = reply.contains("人工确认");
            return passed
                    ? new QualityResult(true, 0.95, "")
                    : new QualityResult(false, 0.62, "补偿发放前仍需人工确认。");
        }
    }

    /** DeepSeek、Ollama、ChatGPT 都通过 OpenAI 兼容协议接入。 */
    public record ModelSettings(String provider, String baseUrl, String modelName, String apiKey) {
        public static ModelSettings fromEnvironment(Map<String, String> env) {
            String provider = env.getOrDefault("AI_PROVIDER", "deepseek").toLowerCase(Locale.ROOT);
            return switch (provider) {
                case "deepseek" -> new ModelSettings(
                        provider,
                        env.getOrDefault("AI_BASE_URL", "https://api.deepseek.com"),
                        env.getOrDefault("AI_MODEL", "deepseek-flash"),
                        env.getOrDefault("DEEPSEEK_API_KEY", "")
                );
                case "ollama" -> new ModelSettings(
                        provider,
                        env.getOrDefault("AI_BASE_URL", "http://localhost:11434/v1"),
                        env.getOrDefault("AI_MODEL", "deepseek-r1:7b"),
                        "not-required"
                );
                case "openai", "chatgpt" -> new ModelSettings(
                        "openai",
                        env.getOrDefault("AI_BASE_URL", "https://api.openai.com/v1"),
                        env.getOrDefault("AI_MODEL", "gpt-4.1-mini"),
                        env.getOrDefault("OPENAI_API_KEY", "")
                );
                default -> throw new IllegalArgumentException("不支持的 AI_PROVIDER：" + provider);
            };
        }
    }

    public static final class LangChain4jModelFactory {
        private LangChain4jModelFactory() {
        }

        public static ChatModel create(ModelSettings settings) {
            if (settings.apiKey().isBlank()) throw new IllegalStateException("live 模式需要模型 API Key");
            String key = settings.apiKey();
            return OpenAiChatModel.builder()
                    .baseUrl(settings.baseUrl())
                    .apiKey(key)
                    .modelName(settings.modelName())
                    .temperature(0.1)
                    .timeout(Duration.ofSeconds(45))
                    .maxRetries(0)
                    .build();
        }
    }
}
